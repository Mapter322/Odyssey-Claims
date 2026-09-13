package earth.terrarium.cadmus.api.settings;

import net.minecraft.world.item.Item;

public interface ItemCondition extends SettingCondition<Item> {

    int priority();
}
