package earth.terrarium.cadmus.api.settings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public record EntityValueCondition(ResourceLocation entity) implements EntityCondition {

    @Override
    public boolean matches(EntityType<?> value) {
        return value.builtInRegistryHolder().key().location().equals(entity);
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String display() {
        return entity.toString();
    }
}
