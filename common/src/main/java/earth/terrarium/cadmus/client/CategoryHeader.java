package earth.terrarium.cadmus.client;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRenderer;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRendererContext;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.TristateRenderers;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.UIIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

class CategoryHeader extends Button {
    private static final int OPTION_WIDTH = 12;
    private static final int OPTION_HEIGHT = 14;
    private static final int OPTIONS_WIDTH = OPTION_WIDTH * 3;
    private static final int PAD_Y = 2;
    private static final TriState[] OPTIONS = {TriState.TRUE, TriState.UNDEFINED, TriState.FALSE};

    private final Font font;
    private final Component label;
    private final Supplier<Boolean> expanded;
    private final Supplier<TriState> aggregate;
    private final Runnable onToggle;
    private final Consumer<TriState> onApply;

    CategoryHeader(Font font, Component label, Supplier<Boolean> expanded, Supplier<TriState> aggregate, Runnable onToggle, Consumer<TriState> onApply) {
        this.font = font;
        this.label = label;
        this.expanded = expanded;
        this.aggregate = aggregate;
        this.onToggle = onToggle;
        this.onApply = onApply;
        this.withSize(1, 20);
        this.withTexture(UIConstants.DARK_BUTTON);
        this.withRenderer(this::renderContent);
    }

    private void renderContent(GuiGraphics graphics, WidgetRendererContext<Button> context, float partialTick) {
        int iconY = context.getY() + (context.getHeight() - 12) / 2;
        graphics.blitSprite(this.expanded.get() ? UIIcons.CHEVRON_DOWN : UIIcons.CHEVRON_UP, context.getX() + 6, iconY, 12, 12);
        graphics.drawString(this.font, this.label, context.getX() + 24, context.getY() + (context.getHeight() - 8) / 2, 0xFFFFFFFF, false);
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
        if (this.onApply != null && mouseX >= this.getX() + this.getWidth() - OPTIONS_WIDTH - 3) {
            int index = (int) ((mouseX - (this.getX() + this.getWidth() - OPTIONS_WIDTH - 3)) / OPTION_WIDTH);
            this.onApply.accept(OPTIONS[Math.max(0, Math.min(index, OPTIONS.length - 1))]);
        } else {
            this.onToggle.run();
        }
    }
}
