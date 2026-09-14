package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 16;

    private final ClaimSettingsTarget target;
    private final boolean canEdit;
    private final Map<String, String> inherited;
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
                TriState tri = value == null ? TriState.UNDEFINED : Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
                this.booleanStates.put(id, RadioState.of(tri, switch (tri) {
                    case TRUE -> 0;
                    case UNDEFINED -> 1;
                    case FALSE -> 2;
                }));
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

        int listWidth = this.imageWidth - SIDE_PADDING * 2;
        int listY = this.topPos + BANNER_HEIGHT + 4;
        int footerTop = this.topPos + this.imageHeight - FOOTER_TOTAL;
        int listHeight = footerTop - listY - 4;

        ListWidget list = new ListWidget(listWidth, listHeight);
        list.withGap(3);
        list.setPosition(this.leftPos + SIDE_PADDING, listY);

        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            RadioState<TriState> state = this.booleanStates.get(id);
            if (state != null) {
                var toggle = Widgets.tristate(state).withTooltip(inheritedTooltip(id));
                if (!this.canEdit) toggle.asDisabled();
                list.add(new LabelledEntry(this.font, settingLabel(id), toggle)
                    .setLockedWidth()
                    .setColor(MinecraftColors.GRAY.getValue())
                    .setDrawDivider(true));
            } else {
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

    private Component inheritedTooltip(String id) {
        Component value = Component.translatable(Boolean.parseBoolean(this.inherited.getOrDefault(id, "true")) ? "options.on" : "options.off");
        return Component.translatable("gui.cadmus.claim_settings.inherited", value);
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
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
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
