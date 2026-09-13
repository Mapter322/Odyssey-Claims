package earth.terrarium.cadmus.api.settings;

import net.minecraft.world.entity.EntityType;

public interface EntityCondition extends SettingCondition<EntityType<?>> {

    int priority();
}
