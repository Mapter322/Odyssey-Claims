package earth.terrarium.cadmus.api.settings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public record ItemValueCondition(ResourceLocation item) implements ItemCondition {

    @Override
    public boolean matches(Item value) {
        return value.builtInRegistryHolder().key().location().equals(item);
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String display() {
        return item.toString();
    }
}
