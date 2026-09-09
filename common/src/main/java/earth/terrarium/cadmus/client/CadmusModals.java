package earth.terrarium.cadmus.client;

import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.components.textbox.TextBox;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.modals.Modals;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CadmusModals {

    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 24;
    private static final int TEXTBOX_HEIGHT = 20;

    private CadmusModals() {}

    public static void confirm(Component title, Component description, Component confirmLabel, Runnable onConfirm) {
        Modals.action()
            .withTitle(title)
            .withContent(description)
            .withAction(cancelButton())
            .withAction(Widgets.button()
                .withRenderer(WidgetRenderers.text(confirmLabel).withColor(MinecraftColors.WHITE))
                .withTexture(UIConstants.DANGER_BUTTON)
                .withCallback(() -> {
                    onConfirm.run();
                    close();
                })
                .withSize(BUTTON_WIDTH, BUTTON_HEIGHT))
            .open();
    }

    public static void input(Component title, Component description, Component placeholder, int maxLength,
                             Component confirmLabel, Predicate<String> validator, Consumer<String> onConfirm) {
        Button confirm = Widgets.button()
            .withRenderer(WidgetRenderers.text(confirmLabel).withColor(MinecraftColors.WHITE))
            .withTexture(UIConstants.PRIMARY_BUTTON)
            .withSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        State<String> state = new State<>() {
            private String value = "";

            @Override
            public String get() {
                return value;
            }

            @Override
            public void set(String value) {
                this.value = value;
                confirm.active = validator.test(value);
            }
        };

        confirm.active = false;
        confirm.withCallback(() -> {
            if (!validator.test(state.get())) return;
            onConfirm.accept(state.get().strip());
            close();
        });

        TextBox textBox = Widgets.textInput(state)
            .withPlaceholder(placeholder.getString())
            .withMaxLength(maxLength);

        Modals.action()
            .withTitle(title)
            .withContent(description)
            .withContent(width -> textBox.withSize(width, TEXTBOX_HEIGHT))
            .withAction(cancelButton())
            .withAction(confirm)
            .open();
    }

    private static Button cancelButton() {
        return Widgets.button()
            .withRenderer(WidgetRenderers.text(UIConstants.CANCEL))
            .withCallback(CadmusModals::close)
            .withSize(BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            minecraft.screen.onClose();
        }
    }
}
