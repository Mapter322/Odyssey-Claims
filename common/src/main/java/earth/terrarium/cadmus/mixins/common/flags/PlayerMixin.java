package earth.terrarium.cadmus.mixins.common.flags;

import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    protected void cadmus$dropEquipment(CallbackInfo ci) {
        if (Settings.isEnabledAt(this.level(), this.chunkPosition(), SettingDefinitions.KEEP_INVENTORY)) {
            ci.cancel();
        }
    }
}
