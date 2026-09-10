package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.settings.SettingCategory;
import earth.terrarium.cadmus.api.settings.SettingScope;
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
import earth.terrarium.olympus.client.components.textbox.TextBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminClaimConfigModal extends BaseModal {
    private static final SettingCategory IDENTITY_CATEGORY = SettingCategory.IDENTITY;

    private final OpenAdminClaimSettingsPacket packet;
    private final Map<String, RadioState<TriState>> booleanStates = new HashMap<>();
    private final Map<String, State<String>> textStates = new HashMap<>();
    private final State<String> name;
    private final State<String> motd;
    private final State<Color> color;

    public AdminClaimConfigModal(ClaimMapScreen background, OpenAdminClaimSettingsPacket packet) {
        super(Component.translatable("gui.cadmus.admin_claim.settings"), background);
        this.packet = packet;
        this.name = State.of(packet.name());
        this.motd = State.of(packet.motd());
        this.color = State.of(packet.color());

        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
            if (definition.category() == IDENTITY_CATEGORY) return;
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
        ListWidget content = new ListWidget(modalContentWidth, modalContentHeight - 22) {{ this.gap = 4; }};
        content.setPosition(modalContentLeft, modalContentTop - 4);
        content.add(new BaseParentWidget(0, 0) {});
        TextBox nameBox = Widgets.textInput(name).withMaxLength(32).withPlaceholder("Admin Claim");
        TextBox motdBox = Widgets.textInput(motd).withMaxLength(64).withPlaceholder("MOTD");
        content.add(Widgets.labelled(font, Component.translatable("gui.cadmus.admin_claim.name"), nameBox));
        content.add(Widgets.labelled(font, Component.translatable("gui.cadmus.admin_claim.color"), Widgets.carousel(widget -> {
            widget.withSize(100, 20);
            widget.withContents(layout -> {
                layout.withChild(Widgets.colorInput(color, textBox -> textBox.withSize(80, 20)));
                layout.withChild(Widgets.colorPicker(color, false, button -> button.withSize(20), overlay -> overlay.withAlignment(OverlayAlignment.BOTTOM_RIGHT)));
            });
        })));
        content.add(Widgets.labelled(font, Component.translatable("gui.cadmus.admin_claim.motd"), motdBox));

        SettingCategory lastCategory = null;
        for (var entry : SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).entrySet()) {
            String id = entry.getKey();
            var definition = entry.getValue();
            if (definition.category() == IDENTITY_CATEGORY) continue;
            if (definition.category() != lastCategory) {
                lastCategory = definition.category();
                content.add(Widgets.labelled(font, categoryLabel(lastCategory), new BaseParentWidget(0, 0) {}));
            }
            RadioState<TriState> state = booleanStates.get(id);
            if (state != null) {
                content.add(Widgets.labelled(font, Component.translatable("cadmus.setting." + id), Widgets.tristate(state)));
            } else {
                State<String> textState = textStates.get(id);
                if (textState != null) {
                    content.add(Widgets.labelled(font, Component.translatable("cadmus.setting." + id), Widgets.textInput(textState).withMaxLength(64)));
                }
            }
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

        addRenderableWidget(content);
        content.visitWidgets(this::addWidget);
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);
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