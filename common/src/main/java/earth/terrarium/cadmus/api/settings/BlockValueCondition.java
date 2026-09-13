package earth.terrarium.cadmus.api.settings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record BlockValueCondition(ResourceLocation block) implements BlockCondition {

    @Override
    public boolean matches(Block value) {
        return value.builtInRegistryHolder().key().location().equals(block);
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String display() {
        return block.toString();
    }
}
