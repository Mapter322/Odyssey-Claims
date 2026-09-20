package earth.terrarium.cadmus.mixins.common.neoforge.compat.cbc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import earth.terrarium.cadmus.common.compat.cbc.CBCProtectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile")
public class AbstractBigCannonProjectileMixin {

    @WrapOperation(
            method = "calculateBlockPenetration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean cadmus$guardDirectBlockRemoval(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        if (level.isClientSide() || CBCProtectionEvents.isProtectedPosition(level, pos)) {
            return false;
        }
        return original.call(level, pos, state, flags);
    }
}
