package earth.terrarium.cadmus.api.settings;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public record BlockTagCondition(TagKey<Block> tag) implements BlockCondition {

    @Override
    public boolean matches(Block value) {
        return value.builtInRegistryHolder().is(tag);
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public String display() {
        return "#" + tag.location();
    }
}
