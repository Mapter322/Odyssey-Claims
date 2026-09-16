package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.client.Modals;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.argonauts.client.widget.SettingCategoryEntry;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenWildernessSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.ModifyWildernessConditionPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.TargetConditions;
import earth.terrarium.cadmus.common.teams.WildernessTeamProvider;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.UIIcons;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WildernessSettingsScreen extends BaseScreen {

    private static final int BANNER_HEIGHT = 16;
    private static final int FOOTER_TOTAL = 28;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 16;

    private OpenWildernessSettingsPacket packet;
    private final Map<String, RadioState<TriState>> booleanStates = new LinkedHashMap<>();
    private final Map<String, State<String>> textStates = new LinkedHashMap<>();
    private final Set<String> expandedCategories = new HashSet<>();

    private ListWidget list;
    private Integer pendingScroll;

    public WildernessSettingsScreen(OpenWildernessSettingsPacket packet) {
        super(Component.translatable("gui.cadmus.wilderness.settings"), 280, 280);
        this.packet = packet;
        this.buildStates();
    }

    public void refresh(OpenWildernessSettingsPacket packet) {
        this.packet = packet;
        this.buildStates();
        if (this.list != null) this.pendingScroll = this.list.getScroll();
        this.rebuildWidgets();
    }

    private void buildStates() {
        this.booleanStates.clear();
        this.textStates.clear();
        SettingDefinitions.forScope(SettingScope.WILDERNESS).forEach((id, definition) -> {
            String value = this.valueFor(id, definition.hasParent() ? definition.parent() : null);
            if (definition.defaultValue() instanceof BooleanSetting) {
                TriState tri = Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
                this.booleanStates.put(id, RadioState.of(tri, tri == TriState.TRUE ? 0 : 2));
            } else {
                this.textStates.put(id, State.of(value));
            }
        });
        for (String condition : this.packet.conditions()) {
            if (this.booleanStates.containsKey(condition) || this.textStates.containsKey(condition)) continue;
            String value = this.valueFor(condition, TargetConditions.parentOf(condition));
            TriState tri = Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
            this.booleanStates.put(condition, RadioState.of(tri, tri == TriState.TRUE ? 0 : 2));
        }
    }

    private String valueFor(String id, @Nullable String parentId) {
        String value = this.packet.settings().get(id);
        if (value == null && parentId != null) {
            value = this.packet.settings().get(parentId);
            if (value == null) {
                SettingDefinition<?> parent = SettingDefinitions.forScope(SettingScope.WILDERNESS).get(parentId);
                if (parent != null) value = SettingCommandSupport.valueToString(parent.defaultValue());
            }
        }
        if (value != null) return value;
        SettingDefinition<?> definition = SettingDefinitions.forScope(SettingScope.WILDERNESS).get(id);
        return definition == null ? "true" : SettingCommandSupport.valueToString(definition.defaultValue());
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

        SettingsListWidget list = new SettingsListWidget(listWidth, listHeight);
        list.withGap(3);
        list.setPosition(this.leftPos + SIDE_PADDING, listY);
        this.list = list;

        for (SettingTarget target : SettingTarget.values()) {
            list.add(new SettingSectionHeader(this.font, targetLabel(target)));

            Map<String, List<ChildEntry>> children = new LinkedHashMap<>();
            List<SettingDefinition<?>> roots = new ArrayList<>();
            SettingDefinitions.forScope(SettingScope.WILDERNESS).forEach((id, definition) -> {
                if (definition.target() != target) return;
                if (definition.hasParent()) {
                    children.computeIfAbsent(definition.parent(), ignored -> new ArrayList<>())
                        .add(new ChildEntry(id, conditionLabel(definition), false));
                } else {
                    roots.add(definition);
                }
            });
            if (target == SettingTarget.PLAYER) {
                for (String condition : this.packet.conditions()) {
                    if (SettingDefinitions.forScope(SettingScope.WILDERNESS).containsKey(condition)) continue;
                    String parent = TargetConditions.parentOf(condition);
                    String key = TargetConditions.keyOf(condition);
                    if (parent == null || key == null) continue;
                    children.computeIfAbsent(parent, ignored -> new ArrayList<>())
                        .add(new ChildEntry(condition, Component.literal(key), true));
                }
            }

            for (SettingDefinition<?> root : roots) {
                List<ChildEntry> group = children.get(root.id());
                if (group == null || group.isEmpty()) {
                    addSettingRow(list, root.id(), Component.translatable("cadmus.setting." + root.id()), false, false);
                    continue;
                }
                list.add(categoryRow(root));
                if (!this.expandedCategories.contains(root.id())) continue;
                for (ChildEntry child : group) {
                    addSettingRow(list, child.id(), child.label(), true, child.dynamic());
                }
                if (TargetConditions.isValidParent(root.id())) {
                    list.add(addConditionButton(root.id()));
                }
            }
        }

        if (this.pendingScroll != null) {
            list.restoreScroll(this.pendingScroll);
            this.pendingScroll = null;
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
        NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(new ClaimSettingsTarget(WildernessTeamProvider.team()), values, resets));
        close();
    }

    private void close() {
        if (this.canGoBack()) {
            this.goBack();
        } else {
            this.onClose();
        }
    }

    private void addSettingRow(ListWidget list, String id, Component label, boolean child, boolean dynamic) {
        RadioState<TriState> state = this.booleanStates.get(id);
        if (state != null) {
            AbstractWidget value = dynamic ? dynamicToggle(state, id) : Widgets.tristate(state);
            LabelledEntry entry = new LabelledEntry(this.font, label, value)
                .setLockedWidth()
                .setColor(MinecraftColors.GRAY.getValue())
                .setDrawDivider(true);
            if (child) entry.setLeftPadding(14);
            list.add(entry);
            return;
        }
        State<String> textState = this.textStates.get(id);
        if (textState == null) return;
        AbstractWidget input = Widgets.textInput(textState).withMaxLength(64).withSize(100, 16);
        LabelledEntry entry = new LabelledEntry(this.font, label, input)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setColor(MinecraftColors.GRAY.getValue())
            .setDrawDivider(true);
        if (child) entry.setLeftPadding(14);
        list.add(entry);
    }

    private AbstractWidget dynamicToggle(RadioState<TriState> state, String id) {
        return Widgets.carousel(widget -> {
            widget.withSize(80, 20);
            widget.withContents(layout -> {
                layout.withChild(Widgets.tristate(state));
                layout.withChild(Widgets.button(button -> {
                    button.withSize(20);
                    button.withTexture(null);
                    button.withRenderer(WidgetRenderers.icon(UIIcons.TRASH).withColor(MinecraftColors.RED));
                    button.withTooltip(Component.translatable("gui.cadmus.claim_settings.remove_condition", id));
                    button.withCallback(() -> NetworkHandler.CHANNEL.sendToServer(new ModifyWildernessConditionPacket(id, false)));
                }));
            });
        });
    }

    private Button addConditionButton(String parent) {
        return Widgets.button(button -> {
            button.withSize(1, 16);
            button.withTexture(UIConstants.DARK_BUTTON);
            button.withRenderer(WidgetRenderers.text(Component.translatable("gui.cadmus.claim_settings.add_condition")).withColor(MinecraftColors.WHITE));
            button.withCallback(() -> this.openConditionModal(parent));
        });
    }

    private void openConditionModal(String parent) {
        Modals.input(
            Component.translatable("gui.cadmus.claim_settings.add_condition"),
            Component.translatable("gui.cadmus.claim_settings.add_condition.description"),
            Component.translatable("gui.cadmus.claim_settings.add_condition.placeholder"),
            64,
            Component.translatable("gui.cadmus.claim_settings.add_condition"),
            input -> this.normalizeCondition(parent, input) != null,
            input -> {
                String condition = this.normalizeCondition(parent, input);
                if (condition != null) {
                    NetworkHandler.CHANNEL.sendToServer(new ModifyWildernessConditionPacket(condition, true));
                }
            }
        );
    }

    @Nullable
    private String normalizeCondition(String parent, String input) {
        if (!TargetConditions.isValidParent(parent)) return null;
        String value = input.strip();
        if (value.isEmpty()) return null;
        int index = value.indexOf('/');
        if (index >= 0) {
            if (!value.substring(0, index).equals(parent)) return null;
            value = value.substring(index + 1);
            if (value.isEmpty()) return null;
        }
        if (TargetConditions.create(parent, value) == null) return null;
        String condition = parent + "/" + value;
        if (SettingDefinitions.forScope(SettingScope.WILDERNESS).containsKey(condition)) return null;
        if (this.packet.conditions().contains(condition)) return null;
        return condition;
    }

    private void toggleCategory(String key) {
        if (!this.expandedCategories.remove(key)) this.expandedCategories.add(key);
        if (this.list != null) this.pendingScroll = this.list.getScroll();
        this.rebuildWidgets();
    }

    private SettingCategoryEntry categoryRow(SettingDefinition<?> definition) {
        RadioState<TriState> state = this.booleanStates.get(definition.id());
        Supplier<TriState> value = state == null ? () -> TriState.UNDEFINED : state::get;
        Consumer<TriState> apply = state == null ? null : newValue -> {
            state.set(newValue);
            state.setIndex(switch (newValue) {
                case TRUE -> 0;
                case UNDEFINED -> 1;
                case FALSE -> 2;
            });
        };
        return new SettingCategoryEntry(
            this.font,
            Component.translatable("cadmus.setting." + definition.id()),
            () -> this.expandedCategories.contains(definition.id()),
            value,
            () -> this.toggleCategory(definition.id()),
            apply,
            true
        );
    }

    private static Component conditionLabel(SettingDefinition<?> definition) {
        if (definition.hasConditions()) {
            return Component.literal(definition.conditions().get(0).display());
        }
        return Component.literal(definition.id());
    }

    private static Component targetLabel(SettingTarget target) {
        return Component.translatable("cadmus.setting.target." + target.name().toLowerCase(Locale.ROOT));
    }

    private record ChildEntry(String id, Component label, boolean dynamic) {
    }

    private static class SettingsListWidget extends ListWidget {

        SettingsListWidget(int width, int height) {
            super(width, height);
        }

        void restoreScroll(int scroll) {
            this.scroll = Math.max(0, Math.min(scroll, Math.max(0, this.getContentHeight() - this.getHeight())));
        }
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
