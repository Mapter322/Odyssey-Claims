package earth.terrarium.cadmus.api.settings;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record ItemTagCondition(TagKey<Item> tag) implements ItemCondition {

    @Override
    public boolean matches(Item value) {
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
