package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.settings.SettingCategory;
import earth.terrarium.cadmus.api.settings.SettingScope;
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
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ClaimConfigModal extends BaseModal {
    private final TeamId teamId;
    private final Map<String, RadioState<TriState>> booleanStates = new HashMap<>();
    private final Map<String, State<String>> textStates = new HashMap<>();

    protected ClaimConfigModal(ClaimMapScreen background, TeamId teamId, Map<String, String> settings) {
        super(ConstantComponents.SETTINGS, background);
        this.teamId = teamId;

        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            String value = settings.getOrDefault(id, SettingCommandSupport.valueToString(definition.defaultValue()));
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
        ListWidget renderedSettings = new ListWidget(modalContentWidth, modalContentHeight - 22) {{ this.gap = 4; }};
        renderedSettings.setPosition(modalContentLeft, modalContentTop - 4);

        renderedSettings.add(new BaseParentWidget(0, 0) {});

        SettingCategory lastCategory = null;
        for (var entry : SettingDefinitions.forScope(SettingScope.TOWN).entrySet()) {
            String id = entry.getKey();
            var definition = entry.getValue();
            if (definition.category() != lastCategory) {
                lastCategory = definition.category();
                renderedSettings.add(Widgets.labelled(font, categoryLabel(lastCategory), new BaseParentWidget(0, 0) {}));
            }
            RadioState<TriState> state = booleanStates.get(id);
            if (state != null) {
                renderedSettings.add(Widgets.labelled(font, settingLabel(id), Widgets.tristate(state)));
            } else {
                State<String> textState = textStates.get(id);
                if (textState != null) {
                    renderedSettings.add(Widgets.labelled(font, settingLabel(id), Widgets.textInput(textState).withMaxLength(64)));
                }
            }
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

        this.addRenderableWidget(renderedSettings);
        renderedSettings.visitWidgets(this::addWidget);
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);
    }

    private static Component settingLabel(String id) {
        return Component.translatable("cadmus.setting." + id);
    }

    private static Component categoryLabel(SettingCategory category) {
        return Component.translatable("cadmus.setting.category." + category.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, left + 1, top + modalHeight - 21 - INNER_PADDING * 2, modalWidth - 2, 20 + INNER_PADDING * 2);
    }
}