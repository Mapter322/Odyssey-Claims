package earth.terrarium.cadmus.client;

import earth.terrarium.olympus.client.components.base.BaseWidget;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class SettingSectionHeader extends BaseWidget {

    private static final int HEIGHT = 18;
    private static final int DIVIDER_COLOR = 0x40FFFFFF;

    private final Font font;
    private final Component label;

    public SettingSectionHeader(Font font, Component label) {
        super(1, HEIGHT);
        this.font = font;
        this.label = label;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(
            this.font,
            this.label,
            this.getX(),
            this.getY() + (this.height - this.font.lineHeight) / 2,
            MinecraftColors.GOLD.getValue(),
            false
        );
        int dividerY = this.getY() + this.height - 1;
        graphics.fill(this.getX(), dividerY, this.getX() + this.width, dividerY + 1, DIVIDER_COLOR);
    }
}
