package earth.terrarium.cadmus.common.protections.types;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class BlockExplosionProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.BLOCK_EXPLOSIONS;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_BLOCK_EXPLOSIONS;
    }

    public boolean canExplodeBlock(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide()) return true;
        TeamId id = getId(level, pos).orElse(null);
        if (id == null) return true;
        LivingEntity entity = explosion.getIndirectSourceEntity();

        if (entity instanceof Player player) {
            return isPlayerAllowed(player, id, new net.minecraft.world.level.ChunkPos(pos));
        } else if (entity != null) {
            return isEntityAllowed(entity, id);
        }
        return !ClaimApi.API.isClaimed(level, pos) && Settings.getAt(level, new net.minecraft.world.level.ChunkPos(pos), SettingDefinitions.BLOCK_EXPLOSIONS);
    }
}
