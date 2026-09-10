package earth.terrarium.cadmus.mixins.common.protections;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Inject(
        method = "randomTick",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cadmus$randomTick(Level level, BlockPos pos, FluidState state, RandomSource random, CallbackInfo ci) {
        if (!Settings.isEnabledAt(level, new ChunkPos(pos), SettingDefinitions.FIRE_SPREAD)) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean cadmus$preventFire(Level level, BlockPos toPos, BlockState toState, Level ignored1, BlockPos fromPos, FluidState fromState, RandomSource ignored2) {
        return !Protections.BLOCK_PLACING.canPlaceBlock(level, fromPos, fromState.createLegacyBlock()) ||
            Protections.BLOCK_PLACING.canPlaceBlock(level, toPos, toState);
    }
}
