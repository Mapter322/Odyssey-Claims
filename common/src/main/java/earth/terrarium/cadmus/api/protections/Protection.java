package earth.terrarium.cadmus.api.protections;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.flags.types.BooleanFlag;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public interface Protection {

    /**
     * The setting name. Used in the `/claim settings` command.
     *
     * @return the setting name
     */
    String setting();

    /**
     * The permission name, used to enable or disable the protection.
     *
     * @return the permission name
     */
    String permission();

    /**
     * The personal permission name, used to allow players to manage their personal claim settings.
     *
     * @return the personal permission name
     */
    String personalPermission();

    /**
     * The name of this protection's admin claim flag.
     *
     * @return the flag name
     */
    BooleanFlag flag();

    /**
     * The game rule key. Used to enable or disable the protection.
     *
     * @return the game rule key
     */
    GameRules.Key<GameRules.BooleanValue> gameRule();

    private boolean gameRuleEnabled(Level level) {
        return level.getGameRules().getBoolean(gameRule());
    }

    private boolean settingEnabled(MinecraftServer server, TeamId id) {
        return CadmusSaveData.getClaimSettingOrDefault(server, id, setting());
    }

    private boolean flagEnabled(MinecraftServer server, TeamId id) {
        return id.provider().equals(AdminTeamProvider.ID) &&
            flag().get(server, id.id());
    }

    default Optional<TeamId> getId(Level level, BlockPos pos) {
        return getId(level, new ChunkPos(pos));
    }

    default Optional<TeamId> getId(Level level, ChunkPos pos) {
        return ClaimApi.API.getClaim(level, pos).map(ClaimData::team);
    }

    default boolean isPlayerAllowed(Player player, TeamId id) {
        return isPlayerAllowed(player.level(), player.getGameProfile(), id);
    }

    default boolean isPlayerAllowed(Level level, GameProfile player, TeamId id) {
        if (CadmusSaveData.canBypass(level.getServer(), player.getId())) return true;

        if (id.provider().equals(AdminTeamProvider.ID)) {
            TriState setting = CadmusSaveData.getClaimSetting(level.getServer(), id, setting());
            if (setting == TriState.TRUE) return true;
            if (setting == TriState.FALSE) return false;
            return flagEnabled(level.getServer(), id);
        }

        if (gameRuleEnabled(level)) return true;

        if (settingEnabled(level.getServer(), id)) return true;
        return TeamApi.API.isMember(level, player, id);
    }

    default boolean isEntityAllowed(Entity entity, TeamId id) {
        if (flagEnabled(entity.getServer(), id)) return false;
        if (gameRuleEnabled(entity.level())) return true;
        return settingEnabled(entity.getServer(), id);
    }

    default boolean isBlockAllowed(Level level, TeamId id, BlockPos pos) {
        return isBlockAllowed(level, id, level.getBlockState(pos));
    }

    default boolean isBlockAllowed(Level level, TeamId id, BlockState state) {
        return CadmusSaveData.isBlockAllowed(level.getServer(), id, state.getBlock());
    }
}
