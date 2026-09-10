package earth.terrarium.cadmus.client;

import com.mojang.math.Axis;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.screens.BaseCursorScreen;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.client.events.CadmusClientEvents;
import earth.terrarium.cadmus.api.events.CadmusEvents;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommand;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.RequestClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.AdminClaimActionPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.RequestAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.protections.SettingsData;
import earth.terrarium.cadmus.common.teams.TeamInfo;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.common.towns.TownManager;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.dropdown.DropdownState;
import earth.terrarium.olympus.client.components.map.MapRenderer;
import earth.terrarium.olympus.client.components.map.MapWidget;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.OverlayAlignment;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.UIIcons;
import earth.terrarium.olympus.client.ui.context.ContextMenu;
import earth.terrarium.olympus.client.ui.modals.DeleteConfirmModal;
import earth.terrarium.olympus.client.utils.State;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClaimMapScreen extends BaseCursorScreen {
    public static final ResourceLocation MAP_ICONS = ResourceLocation.withDefaultNamespace("textures/map/decorations/player.png");
    public static final int MAP_SIZE = 192;
    public static final int BANNER_HEIGHT = 15;
    public static final int BUTTON_HEIGHT = 24;
    public static final int PADDING = 4;
    public static final int WIDTH = MAP_SIZE + PADDING * 2 + 2;
    public static final int HEIGHT = MAP_SIZE + PADDING * 4 + 2 + BANNER_HEIGHT + BUTTON_HEIGHT;

    private static final long NOTIFICATION_DURATION = 4000;
    private static final int MAX_NOTIFICATIONS = 4;

    private final Map<ChunkPos, ClaimTile> claims = new HashMap<>();
    private final List<Notification> notifications = new ArrayList<>();

    private final LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
    private final ClientLevel level = player.clientLevel;

    private final State<MapRenderer> mapState = State.empty();
    private final ClaimContextMenu contextMenu = new ClaimContextMenu();

    private MapWidget mapWidget;
    private Button settingsButton;
    private final Map<TeamId, TeamData> teams = new HashMap<>();
    private List<UUID> availableTowns = List.of();
    private TeamId fallbackTeam;
    public static final DropdownState<UUID> selected = DropdownState.of(null);
    private static final UUID ADMIN_SELECTION = new UUID(0L, 0L);

    private float chunkScale;
    private float pixelScale;
    private int playerChunkX;
    private int playerChunkZ;

    private int selectionStartX;
    private int selectionStartZ;
    private int selectionEndX;
    private int selectionEndZ;
    private ChunkPos lastPaintedChunk;

    public ClaimMapScreen() {
        super(CommonComponents.EMPTY);
    }

    public void refresh() {
        teams.clear();
        TeamApi.API.getTeamsList(this.player).forEach(teamId -> {
            TeamInfo info = CadmusClient.TEAM_INFO.get(teamId);
            if (info == null) return;
            teams.put(teamId, new TeamData(
                info.name(),
                ClaimCommand.getClaimsCount(level, teamId, false),
                ClaimLimitApi.API.getMaxClaims(teamId),
                ClaimCommand.getClaimsCount(level, teamId, true),
                ClaimLimitApi.API.getMaxChunkLoadedClaims(teamId),
                new HashMap<>(),
                State.of(info.color()),
                State.empty()
            ));
        });
        this.fallbackTeam = this.teams.keySet().stream().findFirst().orElse(null);
        this.availableTowns = CadmusClient.TOWNS.values().stream()
            .filter(town -> teams.containsKey(town.team()))
            .map(CadmusClient.ClientTown::id)
            .toList();
        if (selected.get() == null || (!isAdminSelected() && !this.availableTowns.contains(selected.get()))) {
            selected.set(this.availableTowns.isEmpty() ? null : this.availableTowns.get(0));
        }

        int renderDistanceScale = this.getScaledRenderDistance();
        this.chunkScale = renderDistanceScale / 16f;
        this.pixelScale = (float) MAP_SIZE / renderDistanceScale * 16;
        this.playerChunkX = Math.round(player.chunkPosition().x - chunkScale / 2);
        this.playerChunkZ = Math.round(player.chunkPosition().z - chunkScale / 2);

        this.calculateClaims();
        NetworkHandler.CHANNEL.sendToServer(new RequestClaimSettingsPacket());
    }

    public void refreshMap() {
        mapWidget.refreshMap();
    }

    @Override
    protected void init() {
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;

        var frame = new FrameLayout(x, y, WIDTH, HEIGHT);
        frame.setMinDimensions(WIDTH, HEIGHT);

        frame.addChild(new ImageButton(0, 0, 11, 11, UIConstants.MODAL_CLOSE, button -> onClose()), (settings) -> {
                settings.padding(2);
                settings.alignHorizontallyRight();
                settings.alignVerticallyTop();
            })
            .setTooltip(Tooltip.create(ConstantComponents.CLOSE));

        frame.addChild(new ImageButton(0, 0, 11, 11, UIConstants.MODAL_REFRESH, button -> {
                refresh();
                refreshMap();
            }), (settings) -> {
                settings.padding(15, 2);
                settings.alignHorizontallyRight();
                settings.alignVerticallyTop();
            })
            .setTooltip(Tooltip.create(UIConstants.REFRESH));

        frame.addChild(Widgets.button()
            .withRenderer(WidgetRenderers.icon(UIIcons.TRASH).withColor(MinecraftColors.RED))
            .withTexture(null)
            .withCallback(this::unclaimAll)
            .withTooltip(ConstantComponents.UNCLAIM_ALL)
            .withSize(11),
            (settings) -> {
                settings.padding(28, 2);
                settings.alignHorizontallyRight();
                settings.alignVerticallyTop();
            }
        );

        frame.addChild(new StringWidget(ConstantComponents.MAP_TITLE, font), (settings) -> {
            settings.padding(4);
            settings.alignHorizontallyLeft();
            settings.alignVerticallyTop();
        }).setColor(0xFFFFFF);

        this.refresh();
        this.mapWidget = frame.addChild(Widgets.map(mapState), (settings) -> {
            settings.padding(0, BANNER_HEIGHT + PADDING + 1);
            settings.alignHorizontallyCenter();
            settings.alignVerticallyTop();
        });

        mapWidget.withSize(MAP_SIZE);

        settingsButton = frame.addChild(
            Widgets.button()
                .withCallback(() -> {
                    if (isAdminSelected()) {
                        NetworkHandler.CHANNEL.sendToServer(new RequestAdminClaimSettingsPacket());
                    } else {
                        minecraft.setScreen(new ClaimConfigModal(this));
                    }
                })
                .withSize(MAP_SIZE / 2, BUTTON_HEIGHT)
                .withRenderer(WidgetRenderers.text(ConstantComponents.SETTINGS)),
            (settings) -> {
                settings.padding(PADDING);
                settings.alignHorizontallyRight();
                settings.alignVerticallyBottom();
            }
        );

        Button townButton = Widgets.button()
            .withRenderer(selected.withRenderer((value, open) -> value == null
                ? WidgetRenderers.ellpsisWithChevron(open)
                : WidgetRenderers.textWithChevron(townName(value), open)).withPadding(4, 6))
            .withSize(MAP_SIZE / 2, BUTTON_HEIGHT)
            .withCallback(this::openTownMenu);
        selected.setButton(townButton);
        frame.addChild(townButton, (settings) -> {
            settings.padding(PADDING);
            settings.alignHorizontallyLeft();
            settings.alignVerticallyBottom();
        });

        settingsButton.active = selectedTeam() != null && (isAdminSelected() || getData().settings().isEmpty());

        frame.arrangeElements();
        frame.visitWidgets(this::addRenderableWidget);
    }

    private void openTownMenu() {
        List<UUID> towns = List.copyOf(this.availableTowns);
        selected.setOpened(true);
        ContextMenu.open(ctx -> {
            ctx.withBounds(MAP_SIZE / 2, 150)
                .withAlignment(OverlayAlignment.TOP_RIGHT, selected)
                .withTexture(UIConstants.LIST_BG)
                .withCloseCallback(() -> selected.setOpened(false));
            if (player.hasPermissions(2)) {
                ctx.add(() -> Widgets.button()
                    .withTexture(UIConstants.LIST_ENTRY)
                    .withRenderer(WidgetRenderers.text(Component.translatable("gui.cadmus.claim_map.admin_claim")).withColor(MinecraftColors.WHITE).withAlignment(0).withPadding(0, 4))
                    .withSize(MAP_SIZE / 2, 20)
                    .withCallback(() -> select(ADMIN_SELECTION)));
            }
            for (UUID town : towns) {
                ctx.add(() -> Widgets.button()
                    .withTexture(UIConstants.LIST_ENTRY)
                    .withRenderer(WidgetRenderers.text(townName(town)).withColor(MinecraftColors.WHITE).withAlignment(0).withPadding(0, 4))
                    .withSize(MAP_SIZE / 2, 20)
                    .withCallback(() -> select(town)));
            }
        });
    }

    private Component townName(UUID townId) {
        if (ADMIN_SELECTION.equals(townId)) return Component.translatable("gui.cadmus.claim_map.admin_claim");
        return Optional.ofNullable(CadmusClient.TOWNS.get(townId))
            .map(CadmusClient.ClientTown::displayName)
            .orElse(ConstantComponents.NO_TOWNS.copy());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawClaimLabels(graphics);
        drawClaims(graphics, mouseX, mouseY);

        if (this.selectionStartX == 0 && this.selectionStartZ == 0) {
            drawHover(graphics, mouseX, mouseY);
        } else {
            drawSelection(graphics);
        }

        renderPlayerAvatar(graphics);
        contextMenu.render(graphics, mouseX, mouseY);
        renderNotifications(graphics);
    }

    public void showNotification(Component message) {
        notifications.removeIf(notification -> System.currentTimeMillis() > notification.expireAt());
        notifications.removeIf(notification -> notification.message().getString().equals(message.getString()));
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.removeFirst();
        }
        notifications.add(new Notification(message, System.currentTimeMillis() + NOTIFICATION_DURATION));
    }

    private void renderNotifications(GuiGraphics graphics) {
        notifications.removeIf(notification -> System.currentTimeMillis() > notification.expireAt());
        if (notifications.isEmpty()) return;

        int width = notifications.stream()
            .mapToInt(notification -> font.width(notification.message()))
            .max().orElse(0) + ClaimContextMenu.PADDING * 2;
        int itemHeight = font.lineHeight + 4;
        int x = mapWidget.getX() + (mapWidget.getWidth() - width) / 2;
        int y = mapWidget.getY() + PADDING;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        graphics.fill(x - 1, y - 1, x + width + 1, y + notifications.size() * itemHeight + 1, ClaimContextMenu.BORDER);
        graphics.fill(x, y, x + width, y + notifications.size() * itemHeight, ClaimContextMenu.BACKGROUND);
        for (int i = 0; i < notifications.size(); i++) {
            Component message = notifications.get(i).message();
            graphics.drawString(font, message, x + ClaimContextMenu.PADDING, y + i * itemHeight + 2, 0xFFFF5555, false);
        }
        graphics.pose().popPose();
    }

    private void drawClaimLabels(GuiGraphics graphics) {
        int left = mapWidget.getX() + PADDING;
        int right = mapWidget.getX() + mapWidget.getWidth() - PADDING;
        int top = mapWidget.getY() + mapWidget.getHeight() - PADDING - font.lineHeight;

        TeamData data = getData();
        String claimedCount = String.format("%d/%d", data.claimed, data.maxClaims);
        String chunkLoadedCount = String.format("%d/%d", data.loaded, data.maxLoaded);

        graphics.drawString(font, claimedCount, left, top, 0xFFFFFF, true);
        graphics.drawString(font, chunkLoadedCount, right - font.width(chunkLoadedCount), top, 0xFFFFFF, true);

        graphics.drawString(font, ConstantComponents.MAX_CLAIMS, left, top - 10, 0xFFFFFF, true);
        graphics.drawString(font, ConstantComponents.MAX_CHUNK_LOADED_CLAIMS, right - font.width(ConstantComponents.MAX_CHUNK_LOADED_CLAIMS), top - 10, 0xFFFFFF, true);
    }

    private TeamData getData() {
        if (isAdminSelected()) {
            TeamId admin = TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
            TeamInfo info = CadmusClient.TEAM_INFO.getOrDefault(admin, new TeamInfo("Admin Claim", Color.DEFAULT));
            return new TeamData(info.name(), ClaimCommand.getClaimsCount(level, admin, false), 0,
                ClaimCommand.getClaimsCount(level, admin, true), 0, new HashMap<>(), State.of(info.color()), State.of(false));
        }
        TeamData teamData = teams.get(selectedTeam());
        return teamData == null ? TeamData.EMPTY : teamData;
    }

    TeamId selectedTeam() {
        if (isAdminSelected()) return TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
        return Optional.ofNullable(CadmusClient.TOWNS.get(selected.get()))
            .map(CadmusClient.ClientTown::team)
            .orElse(this.fallbackTeam);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        graphics.blitSprite(UIConstants.MODAL, left, top, WIDTH, HEIGHT);
        graphics.blitSprite(UIConstants.MODAL_HEADER, left, top, WIDTH, BANNER_HEIGHT);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, left, top + HEIGHT - BUTTON_HEIGHT - PADDING * 2, WIDTH, BUTTON_HEIGHT + PADDING * 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu.isVisible()) {
            boolean handled = contextMenu.mouseClicked(mouseX, mouseY, button);
            if (!contextMenu.isVisible()) clearSelection();
            return handled;
        }

        ChunkPos hoveredChunk = getChunkAt(mouseX, mouseY);
        if (hoveredChunk == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 || button == 2) {
            paintChunk(hoveredChunk, button);
            this.lastPaintedChunk = hoveredChunk;
            return true;
        }

        this.selectionStartX = hoveredChunk.x;
        this.selectionStartZ = hoveredChunk.z;
        this.selectionEndX = hoveredChunk.x;
        this.selectionEndZ = hoveredChunk.z;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 || button == 2) {
            ChunkPos hoveredChunk = getChunkAt(mouseX, mouseY);
            if (hoveredChunk != null && !hoveredChunk.equals(this.lastPaintedChunk)) {
                paintChunk(hoveredChunk, button);
                this.lastPaintedChunk = hoveredChunk;
            }
            return true;
        }

        for (int i = 0; i < chunkScale; i++) {
            for (int j = 0; j < chunkScale; j++) {
                float x = mapWidget.getX() + (i * pixelScale);
                float y = mapWidget.getY() + (j * pixelScale);

                if (isHovering(mouseX, mouseY, x, y)) {
                    this.selectionEndX = playerChunkX + i;
                    this.selectionEndZ = playerChunkZ + j;
                }
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        setFocused(null);
        if (button == 0 || button == 2) {
            this.lastPaintedChunk = null;
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (button == 1) {
            if (this.selectionStartX != 0 || this.selectionStartZ != 0) {
                openContextMenu(
                    new ChunkPos(this.selectionStartX, this.selectionStartZ),
                    new ChunkPos(this.selectionEndX, this.selectionEndZ),
                    (int) mouseX,
                    (int) mouseY
                );
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
        doAction(this.selectionStartX, this.selectionEndX, this.selectionStartZ, this.selectionEndZ, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private ChunkPos getChunkAt(double mouseX, double mouseY) {
        for (int i = 0; i < chunkScale; i++) {
            for (int j = 0; j < chunkScale; j++) {
                float x = mapWidget.getX() + (i * pixelScale);
                float y = mapWidget.getY() + (j * pixelScale);
                if (isHovering(mouseX, mouseY, x, y)) {
                    return new ChunkPos(playerChunkX + i, playerChunkZ + j);
                }
            }
        }
        return null;
    }

    private void openContextMenu(ChunkPos startPos, ChunkPos endPos, int mouseX, int mouseY) {
        contextMenu.clearItems();
        boolean canClaim = hasUnclaimedChunk(startPos, endPos);
        boolean canUnclaim = hasOwnedClaim(startPos, endPos);
        boolean allFree = allUnclaimed(startPos, endPos);
        boolean hasTeam = selected.get() != null || isAdminSelected();
        contextMenu.addItem(ConstantComponents.CLAIM, () -> {
            if (isAdminSelected()) {
                sendAdminAction(startPos, endPos, true);
            } else {
                CadmusClient.sendTownAdd(selected.get(), startPos, endPos);
            }
            clearSelection();
        }, canClaim && hasTeam);
        contextMenu.addItem(ConstantComponents.UNCLAIM, () -> {
            unclaimWithConfirmation(startPos, endPos);
            clearSelection();
        }, canUnclaim);
        contextMenu.addItem(ConstantComponents.CREATE_TOWN, () -> {
            openCreateTownModal(startPos, endPos);
            clearSelection();
        }, allFree && !isAdminSelected());
        contextMenu.open(mouseX, mouseY);
    }

    private void openCreateTownModal(ChunkPos startPos, ChunkPos endPos) {
        CadmusModals.input(
            ConstantComponents.CREATE_TOWN_MODAL_TITLE,
            ConstantComponents.CREATE_TOWN_MODAL_DESCRIPTION,
            ConstantComponents.CREATE_TOWN_MODAL_PLACEHOLDER,
            TownManager.MAX_TOWN_NAME_LENGTH,
            ConstantComponents.CREATE_TOWN_MODAL_CONFIRM,
            TownManager::isValidTownName,
            name -> CadmusClient.sendTownCreate(name, startPos, endPos)
        );
    }

    private void unclaimWithConfirmation(ChunkPos startPos, ChunkPos endPos) {
        CadmusClient.ClientTown removed = getTownDeletedByUnclaim(startPos, endPos);
        if (removed == null) {
            doUnclaim(startPos, endPos);
            return;
        }
        this.lastPaintedChunk = null;
        CadmusModals.confirm(
            ConstantComponents.DELETE_TOWN_MODAL_TITLE,
            ConstantComponents.deleteTownModalDescription(removed.displayName()),
            ConstantComponents.DELETE_TOWN_MODAL_CONFIRM,
            () -> doUnclaim(startPos, endPos)
        );
    }

    private void doUnclaim(ChunkPos startPos, ChunkPos endPos) {
        if (isAdminSelected()) {
            sendAdminAction(startPos, endPos, false);
            return;
        }
        if (startPos.equals(endPos)) {
            unclaim(startPos);
        } else {
            unclaimArea(startPos, endPos);
        }
    }

    private CadmusClient.ClientTown getTownDeletedByUnclaim(ChunkPos startPos, ChunkPos endPos) {
        TeamId team = selectedTeam();
        if (team == null) return null;
        int minX = Math.min(startPos.x, endPos.x);
        int maxX = Math.max(startPos.x, endPos.x);
        int minZ = Math.min(startPos.z, endPos.z);
        int maxZ = Math.max(startPos.z, endPos.z);
        for (CadmusClient.ClientTown town : CadmusClient.TOWNS.values()) {
            if (!town.team().equals(team) || town.chunks().isEmpty()) continue;
            boolean contained = true;
            for (ChunkPos pos : town.chunks()) {
                if (pos.x < minX || pos.x > maxX || pos.z < minZ || pos.z > maxZ) {
                    contained = false;
                    break;
                }
            }
            if (contained) return town;
        }
        return null;
    }

    private boolean hasUnclaimedChunk(ChunkPos startPos, ChunkPos endPos) {
        for (int x = Math.min(startPos.x, endPos.x); x <= Math.max(startPos.x, endPos.x); x++) {
            for (int z = Math.min(startPos.z, endPos.z); z <= Math.max(startPos.z, endPos.z); z++) {
                if (!claims.containsKey(new ChunkPos(x, z))) return true;
            }
        }
        return false;
    }

    private boolean hasOwnedClaim(ChunkPos startPos, ChunkPos endPos) {
        TeamId team = selectedTeam();
        if (team == null) return false;
        UUID teamId = team.id();
        for (int x = Math.min(startPos.x, endPos.x); x <= Math.max(startPos.x, endPos.x); x++) {
            for (int z = Math.min(startPos.z, endPos.z); z <= Math.max(startPos.z, endPos.z); z++) {
                ClaimTile claim = claims.get(new ChunkPos(x, z));
                if (claim != null && claim.id().equals(teamId)) return true;
            }
        }
        return false;
    }

    private boolean allUnclaimed(ChunkPos startPos, ChunkPos endPos) {
        for (int x = Math.min(startPos.x, endPos.x); x <= Math.max(startPos.x, endPos.x); x++) {
            for (int z = Math.min(startPos.z, endPos.z); z <= Math.max(startPos.z, endPos.z); z++) {
                if (claims.containsKey(new ChunkPos(x, z))) return false;
            }
        }
        return true;
    }

    private void clearSelection() {
        this.selectionStartX = 0;
        this.selectionStartZ = 0;
        this.selectionEndX = 0;
        this.selectionEndZ = 0;
    }

    private void paintChunk(ChunkPos pos, int button) {
        if (button == 0) {
            if (isAdminSelected()) {
                if (this.claims.containsKey(pos)) showNotification(Component.translatable(TownManager.ERR_CHUNK_CLAIMED));
                else sendAdminAction(pos, pos, true);
            } else if (selected.get() == null) {
                showNotification(Component.translatable("gui.cadmus.claim_map.no_town_selected"));
            } else if (this.claims.containsKey(pos)) {
                showNotification(Component.translatable(TownManager.ERR_CHUNK_CLAIMED));
            } else {
                CadmusClient.sendTownAdd(selected.get(), pos, pos);
            }
        } else if (button == 2) {
            if (this.claims.get(pos) != null && selectedTeam() != null && this.claims.get(pos).id().equals(selectedTeam().id())) {
                unclaimWithConfirmation(pos, pos);
            }
        }
    }

    private void drawClaims(GuiGraphics graphics, int mouseX, int mouseY) {
        this.claims.forEach((pos, claim) -> {
            float x = mapWidget.getX() + (claim.x * pixelScale);
            float y = mapWidget.getY() + (claim.y * pixelScale);

            drawClaimSquare(graphics, x, y, pixelScale, pixelScale, modifyAlpha(claim.color, 127),
                claim.north, claim.east,
                claim.south, claim.west,
                claim.northEast, claim.southEast,
                claim.southWest, claim.northWest);

            if (isHovering(mouseX, mouseY, x, y)) {
                ScreenUtils.setTooltip(claim.name);
            }
        });
    }

    private void drawHover(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < chunkScale; i++) {
            for (int j = 0; j < chunkScale; j++) {
                float x = mapWidget.getX() + (i * pixelScale);
                float y = mapWidget.getY() + (j * pixelScale);

                if (isHovering(mouseX, mouseY, x, y)) {
                    drawClaimSquare(graphics, x, y, pixelScale, pixelScale, color(getColor(), 127),
                        true, true, true, true, true, true, true, true);
                    return;
                }
            }
        }
    }

    private void drawSelection(GuiGraphics graphics) {
        int selectionStartX = Math.min(this.selectionStartX, this.selectionEndX);
        int selectionStartZ = Math.min(this.selectionStartZ, this.selectionEndZ);
        int selectionEndX = Math.max(this.selectionStartX, this.selectionEndX);
        int selectionEndZ = Math.max(this.selectionStartZ, this.selectionEndZ);

        float x = mapWidget.getX() + ((selectionStartX - playerChunkX) * pixelScale);
        float y = mapWidget.getY() + ((selectionStartZ - playerChunkZ) * pixelScale);
        float width = Math.max(pixelScale, ((selectionEndX + 1 - selectionStartX)) * pixelScale);
        float height = Math.max(pixelScale, ((selectionEndZ + 1 - selectionStartZ)) * pixelScale);

        drawClaimSquare(graphics, x, y, width, height, color(getColor(), 127),
            true, true, true, true, true, true, true, true);
    }

    private void calculateClaims() {
        this.claims.clear();
        for (int i = 0; i < chunkScale; i++) {
            for (int j = 0; j < chunkScale; j++) {
                ChunkPos pos = new ChunkPos(playerChunkX + i, playerChunkZ + j);

                var claim = ClaimApi.API.getClaim(level, pos);
                if (claim.isEmpty()) continue;
                TeamId id = claim.get().team();

                Component name = getName(id, pos, claim.get().isChunkLoaded());
                int color = color(CadmusClient.TEAM_INFO.getOrDefault(id, new TeamInfo("", Color.DEFAULT)).color(), 127);

                boolean north = checkSide(i, j, 0, -1);
                boolean east = checkSide(i, j, 1, 0);
                boolean south = checkSide(i, j, 0, 1);
                boolean west = checkSide(i, j, -1, 0);

                boolean northEast = checkSide(i, j, 1, -1);
                boolean southEast = checkSide(i, j, 1, 1);
                boolean southWest = checkSide(i, j, -1, 1);
                boolean northWest = checkSide(i, j, -1, -1);

                this.claims.put(pos, new ClaimTile(id.id(), name, color, pos, i, j, north, east, south, west, northEast, southEast, southWest, northWest));
            }
        }
    }

    private boolean checkSide(int x, int z, int offsetX, int offsetZ) {
        ChunkPos pos = new ChunkPos(playerChunkX + x + offsetX, playerChunkZ + z + offsetZ);

        return ClaimApi.API.getClaim(level, pos).map(claim -> {
            ChunkPos currentPos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
            return !claim.team().equals(ClaimApi.API.getClaim(level, currentPos).map(ClaimData::team).orElse(null));
        }).orElse(true);
    }

    private void drawClaimSquare(
        GuiGraphics graphics,
        float x, float y,
        float width, float height,
        int color,
        boolean north, boolean east,
        boolean south, boolean west,
        boolean northEast, boolean southEast,
        boolean southWest, boolean northWest
    ) {
        int roundedX = Math.round(x);
        int roundedY = Math.round(y);
        int roundedWidth = Math.round(x + width);
        int roundedHeight = Math.round(y + height);

        int borderColor = (color & 0x00FFFFFF) | 0xFF000000;

        if (north) graphics.fill(roundedX, roundedY, roundedWidth, roundedY + 1, 2, borderColor);
        else if (northEast) graphics.fill(roundedWidth - 1, roundedY, roundedWidth, roundedY + 1, 2, borderColor);

        if (east) graphics.fill(roundedWidth - 1, roundedY, roundedWidth, roundedHeight, 2, borderColor);
        else if (southEast) graphics.fill(roundedWidth - 1, roundedHeight - 1, roundedWidth, roundedHeight, 2, borderColor);

        if (south) graphics.fill(roundedX, roundedHeight - 1, roundedWidth, roundedHeight, 2, borderColor);
        else if (southWest) graphics.fill(roundedX, roundedHeight - 1, roundedX + 1, roundedHeight, 2, borderColor);

        if (west) graphics.fill(roundedX, roundedY, roundedX + 1, roundedHeight, 2, borderColor);
        else if (northWest) graphics.fill(roundedX, roundedY, roundedX + 1, roundedY + 1, 2, borderColor);

        graphics.fill(roundedX, roundedY, roundedWidth, roundedHeight, 2, color & 0x33ffffff);
    }

    private void renderPlayerAvatar(GuiGraphics graphics) {
        float left = (this.width) / 2f;
        float top = (this.height) / 2f;

        double playerX = player.getX();
        double playerZ = player.getZ();
        double x = (playerX % 16) + (playerX >= 0 ? -8 : 8);
        double y = (playerZ % 16) + (playerZ >= 0 ? -8 : 8);

        float scale = MAP_SIZE / (getMapScale() * 2f + 16);

        x *= scale;
        y *= scale;
        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(left + x, top + y, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot() + 180));
            pose.translate(-4, -4, 2);
            graphics.blit(MAP_ICONS, 0, 0, 40, 0, 8, 8, 128, 128);
        }
    }

    private void doAction(int startX, int endX, int startZ, int endZ, int button) {
        if (startX == 0 && startZ == 0) return;
        ChunkPos startPos = new ChunkPos(startX, startZ);
        ChunkPos endPos = new ChunkPos(endX, endZ);
        if (button == 0) {
            if (isAdminSelected()) {
                if (startPos.equals(endPos) && this.claims.containsKey(startPos)) {
                    showNotification(Component.translatable(TownManager.ERR_CHUNK_CLAIMED));
                } else {
                    sendAdminAction(startPos, endPos, true);
                }
            } else if (selected.get() == null) {
                showNotification(Component.translatable("gui.cadmus.claim_map.no_town_selected"));
            } else if (startPos.equals(endPos) && this.claims.containsKey(startPos)) {
                showNotification(Component.translatable(TownManager.ERR_CHUNK_CLAIMED));
            } else {
                CadmusClient.sendTownAdd(selected.get(), startPos, endPos);
            }
        } else if (button == 2) {
            if (startPos.equals(endPos)) {
                if (this.claims.containsKey(startPos) && selectedTeam() != null && this.claims.get(startPos).id().equals(selectedTeam().id())) {
                    unclaimWithConfirmation(startPos, startPos);
                }
            } else {
                unclaimWithConfirmation(startPos, endPos);
            }
        }
        this.selectionStartX = 0;
        this.selectionStartZ = 0;
        this.selectionEndX = 0;
        this.selectionEndZ = 0;
    }

    private boolean isHovering(double mouseX, double mouseY, float x, float y) {
        return mouseX >= x && mouseX < x + pixelScale && mouseY >= y && mouseY < y + pixelScale;
    }

    private int color(Color color, int alpha) {
        return modifyAlpha(color.getValue(), alpha);
    }

    private int modifyAlpha(int color, int alpha) {
        alpha = Math.min(255, Math.max(0, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private Component getName(TeamId id, ChunkPos pos, boolean chunkLoad) {
        String townName = CadmusClient.TOWNS.values().stream()
            .filter(town -> town.chunks().contains(pos))
            .map(town -> town.displayName().getString())
            .findFirst().orElse(TeamApi.API.getName(level, id).getString());
        return Component.literal(townName).withStyle(ChatFormatting.GRAY)
            .append(CommonComponents.SPACE)
            .append(chunkLoad ?
                Component.translatable("text.cadmus.chunk_loaded").withStyle(ChatFormatting.GOLD) :
                CommonComponents.EMPTY
            );
    }

    private int getScaledRenderDistance() {
        int scale = Minecraft.getInstance().options.renderDistance().get() * 16;
        return scale - (scale % 16) + 16;
    }

    public int getMapScale() {
        int scale = Minecraft.getInstance().options.renderDistance().get() * 8;
        return scale - scale % 16 + 16;
    }


    private void claim(ChunkPos pos, boolean chunkLoad) {
        if (selectedTeam() == null) return;
        CadmusClient.sendClaimCommand(ClaimCommandType.CLAIM, selectedTeam(), "%s %s %s".formatted(pos.getMaxBlockX(), pos.getMaxBlockZ(), chunkLoad));
    }

    private void unclaim(ChunkPos pos) {
        CadmusClient.sendTeamlessClaimCommand(ClaimCommandType.UNCLAIM, "%s %s".formatted(pos.getMaxBlockX(), pos.getMaxBlockZ()));
    }

    private void claimArea(ChunkPos startPos, ChunkPos endPos, boolean chunkLoad) {
        if (selectedTeam() == null) return;
        CadmusClient.sendClaimCommand(ClaimCommandType.CLAIM_AREA, selectedTeam(), "%s %s %s %s %s".formatted(startPos.getMaxBlockX(), startPos.getMaxBlockZ(), endPos.getMaxBlockX(), endPos.getMaxBlockZ(), chunkLoad));
    }

    private void unclaimArea(ChunkPos startPos, ChunkPos endPos) {
        CadmusClient.sendTeamlessClaimCommand(ClaimCommandType.UNCLAIM_AREA, "%s %s %s %s".formatted(startPos.getMaxBlockX(), startPos.getMaxBlockZ(), endPos.getMaxBlockX(), endPos.getMaxBlockZ()));
    }

    private void unclaimAll() {
        if (selectedTeam() == null) return;
        DeleteConfirmModal.open(ConstantComponents.UNCLAIM_MODAL_TITLE, ConstantComponents.UNCLAIM_MODAL_DESCRIPTION, ConstantComponents.UNCLAIM_MODAL_CONFIRM, () -> CadmusClient.sendClaimCommand(ClaimCommandType.UNCLAIM, selectedTeam(), selectedTeam().asArg()));
    }

    private static void update() {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (Minecraft.getInstance().screen instanceof ClaimMapScreen screen) {
            screen.refresh();
        }
    }

    public void updateSettings(Map<TeamId, SettingsData> settings) {
        settings.forEach((teamId, settingsData) -> {
            var team = teams.get(teamId);
            if (team != null) {
                team.settings.putAll(settingsData.settings());
                team.modifyColor.set(settingsData.canModifyColor());
            }
        });
    }

    public void updateColor(Color color) {
        if (selectedTeam() == null) return;
        CadmusClient.TEAM_INFO.put(selectedTeam(), new TeamInfo(CadmusClient.TEAM_INFO.get(selectedTeam()).name(), color));
    }

    public Map<String, TriState> getSettings() {
        return getData().settings;
    }

    public Color getColor() {
        if (selectedTeam() == null) return Color.DEFAULT;
        TeamInfo info = CadmusClient.TEAM_INFO.get(selectedTeam());
        return info == null ? Color.DEFAULT : info.color();
    }

    public boolean canModifyColor() {
        return getData().modifyColor.get();
    }

    private boolean isAdminSelected() {
        return ADMIN_SELECTION.equals(selected.get()) && player.hasPermissions(2);
    }

    private void sendAdminAction(ChunkPos start, ChunkPos end, boolean claim) {
        NetworkHandler.CHANNEL.sendToServer(new AdminClaimActionPacket(start, end, claim));
    }

    private void select(UUID value) {
        selected.set(value);
        if (settingsButton != null) {
            settingsButton.active = selectedTeam() != null && (isAdminSelected() || getData().settings().isEmpty());
        }
    }

    private record ClaimTile(
        UUID id,
        Component name,
        int color,
        ChunkPos pos,
        int x, int y,
        boolean north, boolean east,
        boolean south, boolean west,
        boolean northEast, boolean southEast,
        boolean southWest, boolean northWest
    ) {}

    private record Notification(Component message, long expireAt) {}

    private record TeamData(String name, int claimed, int maxClaims, int loaded, int maxLoaded, Map<String, TriState> settings, State<Color> color, State<Boolean> modifyColor) {
        public static final TeamData EMPTY = new TeamData("empty", 0, 0,0, 0, new HashMap<>(), State.of(Color.DEFAULT), State.of(false));
    }

    private static final class ClaimContextMenu {
        private static final int BACKGROUND = 0xF0111111;
        private static final int BORDER = 0xFF555555;
        private static final int HOVER = 0x55FFFFFF;
        private static final int TEXT = 0xFFDDDDDD;
        private static final int DISABLED = 0xFF777777;
        private static final int PADDING = 5;
        private static final int ITEM_HEIGHT = 16;

        private final List<MenuItem> items = new ArrayList<>();
        private boolean visible;
        private int x;
        private int y;
        private int width;
        private int height;

        private void addItem(Component label, Runnable action, boolean enabled) {
            items.add(new MenuItem(label, action, enabled));
        }

        private void clearItems() {
            items.clear();
        }

        private void open(int x, int y) {
            this.width = items.stream().mapToInt(item -> Minecraft.getInstance().font.width(item.label)).max().orElse(0) + PADDING * 2;
            this.height = PADDING * 2 + items.size() * ITEM_HEIGHT;
            this.x = Math.min(x, Minecraft.getInstance().screen.width - width - 2);
            this.y = Math.min(y, Minecraft.getInstance().screen.height - height - 2);
            this.x = Math.max(2, this.x);
            this.y = Math.max(2, this.y);
            this.visible = true;
        }

        private boolean isVisible() {
            return visible;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!visible) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
            graphics.fill(x, y, x + width, y + height, BACKGROUND);
            for (int i = 0; i < items.size(); i++) {
                MenuItem item = items.get(i);
                int itemY = y + PADDING + i * ITEM_HEIGHT;
                boolean hovered = item.enabled && mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                if (hovered) graphics.fill(x, itemY, x + width, itemY + ITEM_HEIGHT, HOVER);
                graphics.drawString(Minecraft.getInstance().font, item.label, x + PADDING, itemY + 4, item.enabled ? TEXT : DISABLED, false);
            }
            graphics.pose().popPose();
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) return false;
            if (button != 0 || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
                visible = false;
                return true;
            }
            int index = ((int) mouseY - y - PADDING) / ITEM_HEIGHT;
            if (index >= 0 && index < items.size() && items.get(index).enabled) {
                items.get(index).action.run();
            }
            visible = false;
            return true;
        }

        private record MenuItem(Component label, Runnable action, boolean enabled) {}
    }

    static {
        CadmusEvents.AddClaimsEvent.register((level, id, positions) -> update());
        CadmusEvents.RemoveClaimsEvent.register((level, id, positions) -> update());
        CadmusEvents.ClearClaimsEvent.register((level, id) -> update());
        CadmusClientEvents.UpdateTeamInfo.register((id, name, color, updateMaps) -> {
            if (updateMaps) update();
        });
    }
}
