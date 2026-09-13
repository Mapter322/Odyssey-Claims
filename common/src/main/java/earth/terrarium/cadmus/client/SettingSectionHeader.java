package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRenderer;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRendererContext;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.TristateRenderers;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingSectionHeader extends Button {
    private static final int OPTION_WIDTH = 12;
    private static final int OPTION_HEIGHT = 14;
    private static final int OPTIONS_WIDTH = OPTION_WIDTH * 3;
    private static final int PAD_Y = 2;
    private static final TriState[] OPTIONS = {TriState.TRUE, TriState.UNDEFINED, TriState.FALSE};

    private final Font font;
    private final Component label;
    private final Supplier<TriState> aggregate;
    private final Consumer<TriState> onApply;

    public SettingSectionHeader(Font font, Component label, Supplier<TriState> aggregate, Consumer<TriState> onApply) {
        this.font = font;
        this.label = label;
        this.aggregate = aggregate;
        this.onApply = onApply;
        this.withSize(1, 20);
        this.withTexture(UIConstants.DARK_BUTTON);
        this.withRenderer(this::renderContent);
    }

    private void renderContent(GuiGraphics graphics, WidgetRendererContext<Button> context, float partialTick) {
        graphics.drawString(this.font, this.label, context.getX() + 6, context.getY() + (context.getHeight() - 8) / 2, 0xFFFFFFFF, false);
        if (this.onApply == null) return;

        TriState aggregate = this.aggregate.get();
        int stripX = context.getX() + context.getWidth() - OPTIONS_WIDTH - 3;
        int bottomPad = context.getHeight() - PAD_Y - OPTION_HEIGHT;
        for (int i = 0; i < OPTIONS.length; i++) {
            TriState option = OPTIONS[i];
            boolean active = aggregate == option;
            int left = stripX + i * OPTION_WIDTH - context.getX();
            int right = context.getWidth() - (left + OPTION_WIDTH);
            WidgetRenderer<Button> zoneRenderer = WidgetRenderers.layered(
                WidgetRenderers.<Button>sprite(active ? TristateRenderers.getButtonSprites(option) : UIConstants.BUTTON),
                WidgetRenderers.<Button>icon(TristateRenderers.getIcon(option))
                    .withColor(active ? MinecraftColors.WHITE : TristateRenderers.getColor(option))
                    .withPaddingBottom(1)
                    .withCentered(10, 10)
            );
            WidgetRenderers.padded(PAD_Y, right, bottomPad, left, zoneRenderer).render(graphics, context, partialTick);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.onApply == null) return;
        if (mouseX >= this.getX() + this.getWidth() - OPTIONS_WIDTH - 3) {
            int index = (int) ((mouseX - (this.getX() + this.getWidth() - OPTIONS_WIDTH - 3)) / OPTION_WIDTH);
            this.onApply.accept(OPTIONS[Math.max(0, Math.min(index, OPTIONS.length - 1))]);
        }
    }
}
