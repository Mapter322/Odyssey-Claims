package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRenderer;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
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
import java.util.Map;
import java.util.Set;

public class ClaimSettingsScreen extends BaseScreen {

    private static final int BANNER_HEIGHT = 16;
    private static final int FOOTER_TOTAL = 28;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 16;
    private static final int WIDGET_H = 16;
    private static final int TOGGLE_W = 26;
    private static final int TOGGLE_H = 14;
    private static final int TOGGLE_HIT_W = 32;

    private final ClaimSettingsTarget target;
    private final boolean canEdit;
    private final Map<String, String> inherited;
    private final Map<String, State<Boolean>> toggleStates = new LinkedHashMap<>();
    private final Map<String, RadioState<TriState>> booleanStates = new LinkedHashMap<>();
    private final Map<String, State<String>> textStates = new LinkedHashMap<>();

    public ClaimSettingsScreen(SyncClaimSettingsPacket packet) {
        super(title(packet), 260, 280);
        this.target = packet.target();
        this.canEdit = packet.canEdit();
        this.inherited = packet.inherited();

        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            String value = packet.values().get(id);
            if (definition.defaultValue() instanceof BooleanSetting) {
                if (this.target.isGlobal()) {
                    this.toggleStates.put(id, State.of(value != null && Boolean.parseBoolean(value)));
                } else {
                    TriState tri = value == null ? TriState.UNDEFINED : Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
                    this.booleanStates.put(id, RadioState.of(tri, switch (tri) {
                        case TRUE -> 0;
                        case UNDEFINED -> 1;
                        case FALSE -> 2;
                    }));
                }
            } else if (value != null) {
                this.textStates.put(id, State.of(value));
            }
        });
    }

    private static Component title(SyncClaimSettingsPacket packet) {
        if (packet.target().isGlobal()) return Component.translatable("gui.cadmus.claim_settings.title");
        return Component.translatable("gui.cadmus.claim_settings.title.town", packet.name());
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(260, this.width - 12);
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

        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            if (this.target.isGlobal()) {
                State<Boolean> state = this.toggleStates.get(id);
                if (state != null) {
                    list.add(new LabelledEntry(this.font, settingLabel(id), toggle(state)).setLockedWidth().setEntryYOffset(-2).setColor(MinecraftColors.GRAY.getValue()).setDrawDivider(true));
                    return;
                }
            } else {
                RadioState<TriState> state = this.booleanStates.get(id);
                if (state != null) {
                    var tri = Widgets.tristate(state).withTooltip(inheritedTooltip(id));
                    if (!this.canEdit) tri.asDisabled();
                    list.add(new LabelledEntry(this.font, settingLabel(id), tri).setLockedWidth().setColor(MinecraftColors.GRAY.getValue()).setDrawDivider(true));
                    return;
                }
            }
            State<String> textState = this.textStates.get(id);
            if (textState != null) {
                AbstractWidget input = Widgets.textInput(textState).withMaxLength(64).withSize(100, 16);
                input.active = this.canEdit;
                list.add(new LabelledEntry(this.font, settingLabel(id), input)
                    .setLockedWidth()
                    .setEntryYOffset(-2)
                    .setColor(MinecraftColors.GRAY.getValue())
                    .setDrawDivider(true));
            }
        });

        FrameLayout footer = new FrameLayout(listWidth, FOOTER_TOTAL);
        footer.setPosition(this.leftPos + SIDE_PADDING, footerTop);
        footer.addChild(Widgets.button(button -> {
            button.withSize(SAVE_W, SAVE_H);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.SAVE).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            if (!this.canEdit) button.asDisabled();
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

    private Button toggle(State<Boolean> state) {
        WidgetRenderer<Button> onRenderer = WidgetRenderers.center(TOGGLE_W, TOGGLE_H, WidgetRenderers.sprite(UIConstants.SWITCH_ON));
        WidgetRenderer<Button> offRenderer = WidgetRenderers.center(TOGGLE_W, TOGGLE_H, WidgetRenderers.sprite(UIConstants.SWITCH));
        return Widgets.button(button -> {
            button.withSize(TOGGLE_HIT_W, WIDGET_H);
            button.withTexture(null);
            button.withRenderer((graphics, context, partialTick) ->
                (state.get() ? onRenderer : offRenderer).render(graphics, context, partialTick));
            button.withCallback(() -> state.set(!state.get()));
            if (!this.canEdit) button.asDisabled();
        });
    }

    private Component inheritedTooltip(String id) {
        Component value = Component.translatable(Boolean.parseBoolean(this.inherited.getOrDefault(id, "true")) ? "options.on" : "options.off");
        return Component.translatable("gui.cadmus.claim_settings.inherited", value);
    }

    private void save() {
        Map<String, String> values = new HashMap<>();
        Set<String> resets = new HashSet<>();
        if (this.target.isGlobal()) {
            this.toggleStates.forEach((key, state) -> values.put(key, state.get().toString()));
        } else {
            this.booleanStates.forEach((key, state) -> {
                switch (state.get()) {
                    case TRUE -> values.put(key, "true");
                    case FALSE -> values.put(key, "false");
                    case UNDEFINED -> resets.add(key);
                }
            });
        }
        this.textStates.forEach((key, state) -> values.put(key, state.get()));
        NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(this.target, values, resets));
        close();
    }

    private void close() {
        if (this.canGoBack()) {
            this.goBack();
        } else {
            this.onClose();
        }
    }

    private static Component settingLabel(String id) {
        return Component.literal("   ").append(Component.translatable("cadmus.setting." + id));
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
