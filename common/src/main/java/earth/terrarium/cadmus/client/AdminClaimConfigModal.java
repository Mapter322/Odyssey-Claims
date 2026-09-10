package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.BulkClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.UpdateAdminClaimInfoPacket;
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
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;
import earth.terrarium.olympus.client.components.textbox.TextBox;

import java.util.HashMap;
import java.util.Map;

public class AdminClaimConfigModal extends BaseModal {
    private final OpenAdminClaimSettingsPacket packet;
    private final Map<String, RadioState<TriState>> settings = new HashMap<>();
    private final State<String> name;
    private final State<String> motd;
    private final State<Color> color;

    public AdminClaimConfigModal(ClaimMapScreen background, OpenAdminClaimSettingsPacket packet) {
        super(Component.translatable("gui.cadmus.admin_claim.settings"), background);
        this.packet = packet;
        this.name = State.of(packet.name());
        this.motd = State.of(packet.motd());
        this.color = State.of(packet.color());
        packet.settings().forEach((setting, value) -> settings.put(setting, RadioState.of(value, switch (value) {
            case TRUE -> 0;
            case UNDEFINED -> 1;
            case FALSE -> 2;
        })));
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
        settings.forEach((setting, state) -> content.add(Widgets.labelled(font, Component.literal(setting), Widgets.tristate(state))));

        FrameLayout footer = new FrameLayout(modalContentWidth, 20 + INNER_PADDING * 2);
        footer.setPosition(modalContentLeft, top + modalHeight - 21 - INNER_PADDING * 2);
        footer.addChild(Widgets.button(button -> {
            button.withSize(100, 20);
            button.withRenderer(WidgetRenderers.text(Component.translatable("gui.cadmus.claim_map.save")).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withCallback(() -> {
                Map<String, TriState> values = new HashMap<>();
                settings.forEach((key, value) -> values.put(key, value.get()));
                NetworkHandler.CHANNEL.sendToServer(new BulkClaimSettingsPacket(packet.id(), values));
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

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, left + 1, top + modalHeight - 21 - INNER_PADDING * 2, modalWidth - 2, 20 + INNER_PADDING * 2);
    }
}
