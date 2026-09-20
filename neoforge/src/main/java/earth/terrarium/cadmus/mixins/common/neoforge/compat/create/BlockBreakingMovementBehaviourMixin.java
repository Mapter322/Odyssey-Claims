package earth.terrarium.cadmus.mixins.common.neoforge.compat.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import earth.terrarium.cadmus.api.protections.PlacerTracked;
import earth.terrarium.cadmus.common.compat.create.CreateProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public class BlockBreakingMovementBehaviourMixin {

    @WrapOperation(
            method = "tickBreaker",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour;canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean cadmus$protectTickBreaker(BlockBreakingMovementBehaviour instance, Level level, BlockPos pos, BlockState state, Operation<Boolean> original, MovementContext context) {
        return original.call(instance, level, pos, state) && cadmus$isBreakingAllowed(level, pos, context);
    }

    @WrapOperation(
            method = "visitNewPosition",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour;canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean cadmus$protectVisitNewPosition(BlockBreakingMovementBehaviour instance, Level level, BlockPos pos, BlockState state, Operation<Boolean> original, MovementContext context) {
        return original.call(instance, level, pos, state) && cadmus$isBreakingAllowed(level, pos, context);
    }

    @Unique
    private static boolean cadmus$isBreakingAllowed(Level level, BlockPos pos, MovementContext context) {
        if (level.isClientSide()) return true;

        UUID placer = null;
        if (context.blockEntityData != null && context.blockEntityData.hasUUID(PlacerTracked.PLACER_KEY)) {
            placer = context.blockEntityData.getUUID(PlacerTracked.PLACER_KEY);
        }
        return CreateProtectionHelper.isBreakingAllowed(placer, level, pos);
    }
}
