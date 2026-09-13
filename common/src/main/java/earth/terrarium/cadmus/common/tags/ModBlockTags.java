package earth.terrarium.cadmus.common.tags;

import earth.terrarium.cadmus.Cadmus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {

    public static final TagKey<Block> ALLOWS_CLAIM_INTERACTIONS = tag("allows_claim_interactions");

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, Cadmus.id(name));
    }
}
