package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.BaseParentWidget;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.modals.BaseModal;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClaimConfigModal extends BaseModal {
    private static final float WIDTH_RATIO = 0.5f;

    private final TeamId teamId;
    private final Map<String, RadioState<TriState>> booleanStates = new HashMap<>();
    private final Map<String, State<String>> textStates = new HashMap<>();
    private final Map<SettingTarget, Boolean> expandedTargets = new HashMap<>();

    private SettingsListWidget settingsList;
    private Integer pendingScroll;

    protected ClaimConfigModal(ClaimMapScreen background, TeamId teamId, Map<String, String> settings) {
        super(ConstantComponents.SETTINGS, background);
        this.teamId = teamId;

        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            String value = settings.getOrDefault(id, SettingCommandSupport.valueToString(definition.defaultValue()));
            if (definition.target() != SettingTarget.GLOBAL) return;
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
        this.modalWidth = Math.round(Math.max(this.minWidth, this.width * WIDTH_RATIO));
        this.modalHeight = Math.round(Math.max(this.minHeight, this.height * this.ratio));
        this.left = (this.width - this.modalWidth) / 2;
        this.top = (this.height - this.modalHeight) / 2;
        this.modalContentTop = this.top + TITLE_BAR_HEIGHT + INNER_PADDING;
        this.modalContentLeft = this.left + INNER_PADDING;
        this.modalContentWidth = this.modalWidth - INNER_PADDING * 2;
        this.modalContentHeight = this.modalHeight - TITLE_BAR_HEIGHT - INNER_PADDING * 2;

        GridLayout closeLayout = this.initButtons(0);
        closeLayout.arrangeElements();
        closeLayout.setPosition(this.left + this.modalWidth - closeLayout.getWidth() - INNER_PADDING, this.top + 3);
        closeLayout.visitWidgets(this::addRenderableWidget);

        this.settingsList = new SettingsListWidget(modalContentWidth, modalContentHeight - 22);
        this.settingsList.setPosition(modalContentLeft, modalContentTop - 4);
        this.settingsList.add(new BaseParentWidget(0, 0) {});

        for (SettingTarget target : new SettingTarget[]{SettingTarget.GLOBAL}) {
            this.settingsList.add(new CategoryHeader(font, targetLabel(target),
                () -> this.expandedTargets.getOrDefault(target, true),
                () -> this.aggregateState(target),
                () -> this.toggleTarget(target),
                hasBooleanSettings(target) ? value -> this.applyTarget(target, value) : null));
            if (!this.expandedTargets.getOrDefault(target, true)) continue;

            SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
                if (definition.target() != target) return;
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
            button.withRenderer(WidgetRenderers.text(ConstantComponents.SAVE).withColor(MinecraftColors.WHITE));
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
                NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(teamId, values, resets));
                onClose();
            });
        }), layoutSettings -> {
            layoutSettings.alignHorizontallyRight();
            layoutSettings.alignVerticallyMiddle();
        });

        this.addRenderableWidget(this.settingsList);
        this.settingsList.visitWidgets(this::addWidget);
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);
    }

    private boolean hasBooleanSettings(SettingTarget target) {
        for (var entry : SettingDefinitions.forScope(SettingScope.TOWN).entrySet()) {
            if (entry.getValue().target() == target && this.booleanStates.containsKey(entry.getKey())) return true;
        }
        return false;
    }

    private TriState aggregateState(SettingTarget target) {
        boolean anyTrue = false;
        boolean anyFalse = false;
        for (var entry : SettingDefinitions.forScope(SettingScope.TOWN).entrySet()) {
            if (entry.getValue().target() != target) continue;
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
        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            RadioState<TriState> state = this.booleanStates.get(id);
            if (definition.target() != target || state == null) return;
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
