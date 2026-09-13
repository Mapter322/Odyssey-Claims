package earth.terrarium.cadmus.api.settings;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record EntityTagCondition(TagKey<EntityType<?>> tag) implements EntityCondition {

    @Override
    public boolean matches(EntityType<?> value) {
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
