package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.UpdateAdminClaimInfoPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.BaseParentWidget;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.OverlayAlignment;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.modals.BaseModal;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminClaimConfigModal extends BaseModal {
    private static final Set<String> IDENTITY_SETTINGS = Set.of("display-name", "motd", "color");

    private final OpenAdminClaimSettingsPacket packet;
    private final Map<String, RadioState<TriState>> booleanStates = new HashMap<>();
    private final Map<String, State<String>> textStates = new HashMap<>();
    private final Map<SettingTarget, Boolean> expandedTargets = new HashMap<>();
    private final State<String> name;
    private final State<String> motd;
    private final State<Color> color;

    private SettingsListWidget settingsList;
    private Integer pendingScroll;

    public AdminClaimConfigModal(ClaimMapScreen background, OpenAdminClaimSettingsPacket packet) {
        super(Component.translatable("gui.cadmus.admin_claim.settings"), background);
        this.packet = packet;
        this.name = State.of(packet.name());
        this.motd = State.of(packet.motd());
        this.color = State.of(packet.color());

        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
            if (IDENTITY_SETTINGS.contains(id)) return;
            String value = packet.settings().getOrDefault(id, SettingCommandSupport.valueToString(definition.defaultValue()));
            if (definition.defaultValue() instanceof BooleanSetting) {
                TriState tri = Boolean.parseBoolean(value) ? TriState.TRUE : TriState.FALSE;
                booleanStates.put(id, RadioState.of(tri, tri == TriState.TRUE ? 0 : 2));
            } else {
                textStates.put(id, State.of(value));
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        this.settingsList = new SettingsListWidget(modalContentWidth, modalContentHeight - 22);
        this.settingsList.setPosition(modalContentLeft, modalContentTop - 4);
        this.settingsList.add(new BaseParentWidget(0, 0) {});

        AbstractWidget nameBox = Widgets.textInput(name).withMaxLength(32).withPlaceholder("Admin Claim").withSize(116, 16);
        AbstractWidget motdBox = Widgets.textInput(motd).withMaxLength(64).withPlaceholder("MOTD").withSize(116, 16);
        this.settingsList.add(new LabelledEntry(font, Component.translatable("gui.cadmus.admin_claim.name"), nameBox)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true));
        this.settingsList.add(new LabelledEntry(font, Component.translatable("gui.cadmus.admin_claim.color"), Widgets.carousel(widget -> {
            widget.withSize(100, 20);
            widget.withContents(layout -> {
                layout.withChild(Widgets.colorInput(color, textBox -> textBox.withSize(80, 20)));
                layout.withChild(Widgets.colorPicker(color, false, button -> button.withSize(20), overlay -> overlay.withAlignment(OverlayAlignment.BOTTOM_RIGHT)));
            });
        })).setLockedWidth().setDrawDivider(true));
        this.settingsList.add(new LabelledEntry(font, Component.translatable("gui.cadmus.admin_claim.motd"), motdBox)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true));

        for (SettingTarget target : SettingTarget.values()) {
            this.settingsList.add(new CategoryHeader(font, targetLabel(target),
                () -> this.expandedTargets.getOrDefault(target, true),
                () -> this.aggregateState(target),
                () -> this.toggleTarget(target),
                hasBooleanSettings(target) ? value -> this.applyTarget(target, value) : null));
            if (!this.expandedTargets.getOrDefault(target, true)) continue;

            SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
                if (IDENTITY_SETTINGS.contains(id) || definition.target() != target) return;
                RadioState<TriState> state = this.booleanStates.get(id);
                if (state != null) {
                    this.settingsList.add(new LabelledEntry(font, settingLabel(id), Widgets.tristate(state))
                        .setLockedWidth()
                        .setColor(MinecraftColors.GRAY.getValue())
                        .setDrawDivider(true));
                } else {
                    State<String> textState = this.textStates.get(id);
                    if (textState != null) {
                        AbstractWidget input = Widgets.textInput(textState).withMaxLength(64).withSize(100, 16);
                        this.settingsList.add(new LabelledEntry(font, settingLabel(id), input)
                            .setLockedWidth()
                            .setEntryYOffset(-2)
                            .setColor(MinecraftColors.GRAY.getValue())
                            .setDrawDivider(true));
                    }
                }
            });
        }

        if (this.pendingScroll != null) {
            this.settingsList.restoreScroll(this.pendingScroll);
            this.pendingScroll = null;
        }

        FrameLayout footer = new FrameLayout(modalContentWidth, 20 + INNER_PADDING * 2);
        footer.setPosition(modalContentLeft, top + modalHeight - 21 - INNER_PADDING * 2);
        footer.addChild(Widgets.button(button -> {
            button.withSize(100, 20);
            button.withRenderer(WidgetRenderers.text(Component.translatable("gui.cadmus.claim_map.save")).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withCallback(() -> {
                Map<String, String> values = new HashMap<>();
                Set<String> resets = new HashSet<>();
                booleanStates.forEach((key, state) -> {
                    switch (state.get()) {
                        case TRUE -> values.put(key, "true");
                        case FALSE -> values.put(key, "false");
                        case UNDEFINED -> resets.add(key);
                    }
                });
                textStates.forEach((key, state) -> values.put(key, state.get()));
                NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(packet.id(), values, resets));
                NetworkHandler.CHANNEL.sendToServer(new UpdateAdminClaimInfoPacket(packet.id(), name.get(), color.get(), motd.get()));
                onClose();
            });
        }), layout -> {
            layout.alignHorizontallyRight();
            layout.alignVerticallyMiddle();
        });

        addRenderableWidget(this.settingsList);
        this.settingsList.visitWidgets(this::addWidget);
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);
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

    private void toggleTarget(SettingTarget target) {
        this.pendingScroll = this.settingsList == null ? 0 : this.settingsList.getScroll();
        this.expandedTargets.put(target, !this.expandedTargets.getOrDefault(target, true));
        this.clearWidgets();
        this.init();
    }

    private static Component settingLabel(String id) {
        return Component.literal("   ").append(Component.translatable("cadmus.setting." + id));
    }

    private static Component targetLabel(SettingTarget target) {
        return Component.translatable("cadmus.setting.target." + target.name().toLowerCase(Locale.ROOT));
    }

    private static class SettingsListWidget extends ListWidget {
        SettingsListWidget(int width, int height) {
            super(width, height);
            this.gap = 4;
        }

        void restoreScroll(int scroll) {
            this.scroll = Math.max(0, Math.min(scroll, Math.max(0, this.getContentHeight() - this.getHeight())));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, left + 1, top + modalHeight - 21 - INNER_PADDING * 2, modalWidth - 2, 20 + INNER_PADDING * 2);
    }
}
