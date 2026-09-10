package earth.terrarium.cadmus.mixins.common.flags;

import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(
        method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void cadmus$spawnCategoryForPosition(MobCategory category, ServerLevel level, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback, CallbackInfo ci) {
        if (category == MobCategory.MONSTER && !Settings.isEnabledAt(level, chunk.getPos(), SettingDefinitions.MONSTER_SPAWNING)) {
            ci.cancel();
        } else if (!Settings.isEnabledAt(level, chunk.getPos(), SettingDefinitions.CREATURE_SPAWNING)) {
            ci.cancel();
        }
    }
}
