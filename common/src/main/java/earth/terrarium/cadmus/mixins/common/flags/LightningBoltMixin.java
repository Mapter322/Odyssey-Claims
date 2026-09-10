package earth.terrarium.cadmus.mixins.common.flags;

import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin extends Entity {

    public LightningBoltMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
        method = "spawnFire",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cadmus$spawnFire(int extraIgnitions, CallbackInfo ci) {
        if (this.level() instanceof ServerLevel level &&
            !Settings.isEnabledAt(level, chunkPosition(), SettingDefinitions.FIRE_SPREAD)) {
            ci.cancel();
        }
    }
}
