package earth.terrarium.cadmus.api.protections;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.teams.WildernessTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface Protection {

    /**
     * The setting name. Used in the `/claim settings` command.
     *
     * @return the setting name
     */
    SettingDefinition<Boolean> setting();

    /**
     * The game rule key. Used to enable or disable the protection.
     *
     * @return the game rule key
     */
    GameRules.Key<GameRules.BooleanValue> gameRule();

    private boolean gameRuleEnabled(Level level) {
        return level.getGameRules().getBoolean(gameRule());
    }

    default Optional<TeamId> getId(Level level, BlockPos pos) {
        return getId(level, new ChunkPos(pos));
    }

    default Optional<TeamId> getId(Level level, ChunkPos pos) {
        return ClaimApi.API.getClaim(level, pos).map(ClaimData::team)
            .or(() -> Optional.of(WildernessTeamProvider.team()));
    }

    default boolean isPlayerAllowed(Player player, TeamId id) {
        return isPlayerAllowed(player.level(), player.getGameProfile(), id, setting());
    }

    default boolean isPlayerAllowed(Level level, GameProfile player, TeamId id) {
        return isPlayerAllowed(level, player, id, setting());
    }

    default boolean isPlayerAllowed(Level level, GameProfile player, TeamId id, SettingDefinition<Boolean> definition) {
        if (CadmusSaveData.canBypass(level.getServer(), player.getId())) return true;

        if (id.isAdmin() || id.isWilderness()) return Settings.getForTeam(level.getServer(), id, definition);

        if (gameRuleEnabled(level)) return true;

        return Settings.isPlayerAllowed(level, player.getId(), id, definition);
    }

    default boolean isPlayerAllowed(Player player, TeamId id, ChunkPos pos) {
        return isPlayerAllowed(player.level(), player.getGameProfile(), id, pos, setting());
    }

    default boolean isPlayerAllowed(Level level, GameProfile player, TeamId id, ChunkPos pos, SettingDefinition<Boolean> definition) {
        if (CadmusSaveData.canBypass(level.getServer(), player.getId())) return true;

        if (id.isAdmin() || id.isWilderness()) return Settings.getAt(level, pos, definition);

        if (gameRuleEnabled(level)) return true;

        return Settings.isPlayerAllowed(level, player.getId(), id, pos, definition);
    }

    default boolean isEntityAllowed(Entity entity, TeamId id) {
        if (id.isAdmin() || id.isWilderness()) return Settings.getAt(entity.level(), entity.chunkPosition(), setting());
        if (gameRuleEnabled(entity.level())) return true;
        return Settings.getAt(entity.level(), entity.chunkPosition(), setting());
    }
}
