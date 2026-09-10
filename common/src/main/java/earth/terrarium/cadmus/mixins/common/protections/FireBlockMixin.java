package earth.terrarium.cadmus.mixins.common.protections;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Inject(
        method = "tick",
        at = @At(
            value = "HEAD",
            target = "Lnet/minecraft/world/level/block/FireBlock;tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"),
        cancellable = true
    )
    private void cadmus$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!Settings.isEnabledAt(level, new ChunkPos(pos), SettingDefinitions.FIRE_SPREAD)) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            ordinal = 1
        )
    )
    private boolean cadmus$preventFire(ServerLevel level, BlockPos toPos, BlockState toState, int ignored1, BlockState fromState, ServerLevel ignored2, BlockPos fromPos, RandomSource ignored3) {
        return !Protections.BLOCK_PLACING.canPlaceBlock(level, fromPos, fromState) ||
            Protections.BLOCK_PLACING.canPlaceBlock(level, toPos, toState);
    }
}
