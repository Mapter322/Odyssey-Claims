package earth.terrarium.cadmus.mixins.common.flags;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @WrapWithCondition(
        method = "tickPrecipitation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 2)
    )
    private boolean cadmus$tickChunk(ServerLevel level, BlockPos pos, BlockState state) {
        return Settings.isEnabledAt(level, new ChunkPos(pos), SettingDefinitions.SNOW_FALL);
    }

    @WrapWithCondition(
        method = "tickPrecipitation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean cadmus$tickChunkIce(ServerLevel level, BlockPos pos, BlockState state) {
        return Settings.isEnabledAt(level, new ChunkPos(pos), SettingDefinitions.ICE_FORM);
    }
}
