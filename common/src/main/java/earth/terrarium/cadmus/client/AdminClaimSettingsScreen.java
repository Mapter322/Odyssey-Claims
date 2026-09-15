package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.UpdateAdminClaimInfoPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.OverlayAlignment;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class AdminClaimSettingsScreen extends BaseScreen {

    private static final Set<String> IDENTITY_SETTINGS = Set.of("display-name", "motd", "color");
    private static final int BANNER_HEIGHT = 16;
    private static final int FOOTER_TOTAL = 28;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 16;

    private final OpenAdminClaimSettingsPacket packet;
    private final Map<String, RadioState<TriState>> booleanStates = new LinkedHashMap<>();
    private final Map<String, State<String>> textStates = new LinkedHashMap<>();
    private final State<String> name;
    private final State<String> motd;
    private final State<Color> color;

    public AdminClaimSettingsScreen(OpenAdminClaimSettingsPacket packet) {
        super(Component.translatable("gui.cadmus.admin_claim.settings"), 280, 280);
        this.packet = packet;
        this.name = State.of(packet.name());
        this.motd = State.of(packet.motd());
        this.color = State.of(packet.color());

        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
            if (IDENTITY_SETTINGS.contains(id)) return;
            String value = packet.settings().get(id);
            if (value == null && definition.hasParent()) {
                SettingDefinition<?> parent = SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).get(definition.parent());
                if (parent != null) {
                    value = packet.settings().get(parent.id());
                    if (value == null) value = SettingCommandSupport.valueToString(parent.defaultValue());
                }
            }
            if (value == null) value = SettingCommandSupport.valueToString(definition.defaultValue());
            if (definition.defaultValue() instanceof BooleanSetting) {
                TriState tri = Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
                this.booleanStates.put(id, RadioState.of(tri, tri == TriState.TRUE ? 0 : 2));
            } else {
                this.textStates.put(id, State.of(value));
            }
        });
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(280, this.width - 12);
        this.imageHeight = Math.min(280, this.height - 12);
        super.init();

        ImageButton closeButton = new ImageButton(
            0, 0, 11, 11,
            UIConstants.MODAL_CLOSE,
            button -> close()
        );
        closeButton.setPosition(
            this.leftPos + this.imageWidth - 11 - HEADER_PAD,
            this.topPos + (BANNER_HEIGHT - 11) / 2
        );
        closeButton.setTooltip(Tooltip.create(ConstantComponents.CLOSE));
        this.addRenderableWidget(closeButton);

        int listWidth = this.imageWidth - SIDE_PADDING * 2;
        int listY = this.topPos + BANNER_HEIGHT + 4;
        int footerTop = this.topPos + this.imageHeight - FOOTER_TOTAL;
        int listHeight = footerTop - listY - 4;

        ListWidget list = new ListWidget(listWidth, listHeight);
        list.withGap(3);
        list.setPosition(this.leftPos + SIDE_PADDING, listY);

        AbstractWidget nameBox = Widgets.textInput(this.name).withMaxLength(32).withPlaceholder("Admin Claim").withSize(116, 16);
        AbstractWidget motdBox = Widgets.textInput(this.motd).withMaxLength(64).withPlaceholder("MOTD").withSize(116, 16);
        list.add(new LabelledEntry(this.font, Component.translatable("gui.cadmus.admin_claim.name"), nameBox)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true));
        list.add(new LabelledEntry(this.font, Component.translatable("gui.cadmus.admin_claim.color"), Widgets.carousel(widget -> {
            widget.withSize(100, 20);
            widget.withContents(layout -> {
                layout.withChild(Widgets.colorInput(this.color, textBox -> textBox.withSize(80, 20)));
                layout.withChild(Widgets.colorPicker(this.color, false, button -> button.withSize(20), overlay -> overlay.withAlignment(OverlayAlignment.BOTTOM_RIGHT)));
            });
        })).setLockedWidth().setDrawDivider(true));
        list.add(new LabelledEntry(this.font, Component.translatable("gui.cadmus.admin_claim.motd"), motdBox)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true));

        for (SettingTarget target : SettingTarget.values()) {
            Consumer<TriState> apply = hasBooleanSettings(target) ? value -> this.applyTarget(target, value) : null;
            list.add(new SettingSectionHeader(this.font, targetLabel(target), () -> this.aggregateState(target), apply));

            SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
                if (IDENTITY_SETTINGS.contains(id) || definition.target() != target) return;
                RadioState<TriState> state = this.booleanStates.get(id);
                if (state != null) {
                    list.add(new LabelledEntry(this.font, settingLabel(definition), Widgets.tristate(state))
                        .setLockedWidth()
                        .setColor(MinecraftColors.GRAY.getValue())
                        .setDrawDivider(true));
                } else {
                    State<String> textState = this.textStates.get(id);
                    if (textState != null) {
                        AbstractWidget input = Widgets.textInput(textState).withMaxLength(64).withSize(100, 16);
                        list.add(new LabelledEntry(this.font, settingLabel(definition), input)
                            .setLockedWidth()
                            .setEntryYOffset(-2)
                            .setColor(MinecraftColors.GRAY.getValue())
                            .setDrawDivider(true));
                    }
                }
            });
        }

        FrameLayout footer = new FrameLayout(listWidth, FOOTER_TOTAL);
        footer.setPosition(this.leftPos + SIDE_PADDING, footerTop);
        footer.addChild(Widgets.button(button -> {
            button.withSize(SAVE_W, SAVE_H);
            button.withRenderer(WidgetRenderers.text(Component.translatable("gui.cadmus.claim_map.save")).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withCallback(this::save);
        }), layout -> {
            layout.alignHorizontallyRight();
            layout.alignVerticallyMiddle();
        });
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);

        this.addRenderableWidget(list);
        list.visitWidgets(this::addWidget);
    }

    private void save() {
        Map<String, String> values = new HashMap<>();
        Set<String> resets = new HashSet<>();
        this.booleanStates.forEach((key, state) -> {
            switch (state.get()) {
                case TRUE -> values.put(key, "true");
                case FALSE -> values.put(key, "false");
                case UNDEFINED -> resets.add(key);
            }
        });
        this.textStates.forEach((key, state) -> values.put(key, state.get()));
        NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(new ClaimSettingsTarget(this.packet.id()), values, resets));
        NetworkHandler.CHANNEL.sendToServer(new UpdateAdminClaimInfoPacket(this.packet.id(), this.name.get(), this.color.get(), this.motd.get()));
        close();
    }

    private void close() {
        if (this.canGoBack()) {
            this.goBack();
        } else {
            this.onClose();
        }
    }

    private boolean hasBooleanSettings(SettingTarget target) {
        for (var entry : SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).entrySet()) {
            if (IDENTITY_SETTINGS.contains(entry.getKey())) continue;
            if (entry.getValue().target() == target && this.booleanStates.containsKey(entry.getKey())) return true;
        }
        return false;
    }

    private TriState aggregateState(SettingTarget target) {
        boolean anyTrue = false;
        boolean anyFalse = false;
        for (var entry : SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).entrySet()) {
            if (IDENTITY_SETTINGS.contains(entry.getKey()) || entry.getValue().target() != target) continue;
            RadioState<TriState> state = this.booleanStates.get(entry.getKey());
            if (state == null) continue;
            if (state.get() == TriState.TRUE) anyTrue = true;
            else if (state.get() == TriState.FALSE) anyFalse = true;
        }
        if (anyTrue && !anyFalse) return TriState.TRUE;
        if (anyFalse && !anyTrue) return TriState.FALSE;
        return TriState.UNDEFINED;
    }

    private void applyTarget(SettingTarget target, TriState value) {
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
            RadioState<TriState> state = this.booleanStates.get(id);
            if (IDENTITY_SETTINGS.contains(id) || definition.target() != target || state == null) return;
            state.set(value);
            state.setIndex(switch (value) {
                case TRUE -> 0;
                case UNDEFINED -> 1;
                case FALSE -> 2;
            });
        });
    }

    private static Component settingLabel(SettingDefinition<?> definition) {
        String label = definition.id().contains("/") ? definition.id().substring(definition.id().indexOf('/') + 1) : definition.id();
        return Component.literal("   ").append(Component.translatable("cadmus.setting." + label));
    }

    private static Component targetLabel(SettingTarget target) {
        return Component.translatable("cadmus.setting.target." + target.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, HEADER_PAD, (BANNER_HEIGHT - this.font.lineHeight) / 2, 0xFFFFFF, false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        this.renderBlurredBackground(partialTick);
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(this.leftPos, this.topPos, 0.0F);
            this.renderLabels(graphics, mouseX, mouseY);
        }
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (var listener : this.children()) {
            if (listener.mouseClicked(mx, my, button)) {
                this.setFocused(listener);
                if (button == 0) this.setDragging(true);
                return true;
            }
        }
        this.setFocused(null);
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.disableDepthTest();
        graphics.blitSprite(UIConstants.MODAL, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        graphics.blitSprite(UIConstants.MODAL_HEADER, this.leftPos, this.topPos, this.imageWidth, BANNER_HEIGHT);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, this.leftPos, this.topPos + this.imageHeight - FOOTER_TOTAL, this.imageWidth, FOOTER_TOTAL);
        RenderSystem.enableDepthTest();
    }
}
