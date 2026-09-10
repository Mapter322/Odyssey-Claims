package earth.terrarium.cadmus.mixins.common.flags;

import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {

    @Inject(
        method = "randomTick",
        at = @At(
            value = "HEAD",
            target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        ),
        cancellable = true)
    private void cadmus$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!Settings.isEnabledAt(level, new ChunkPos(pos), SettingDefinitions.SNOW_MELT)) {
            ci.cancel();
        }
    }
}
