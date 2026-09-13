package earth.terrarium.cadmus.api.settings;

import net.minecraft.world.level.block.Block;

public interface BlockCondition extends SettingCondition<Block> {

    int priority();
}
