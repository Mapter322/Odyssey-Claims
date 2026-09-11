package earth.terrarium.cadmus.common.settings;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

import java.util.Map;

public final class Settings {

    private Settings() {
    }

    public static <T> T getAt(Level level, ChunkPos pos, SettingDefinition<T> definition) {
        if (level.isClientSide() || level.getServer() == null) return definition.defaultValue().value();

        return ClaimApi.API.getClaim(level, pos)
            .map(ClaimData::team)
            .map(team -> getForTeam(level.getServer(), team, definition))
            .orElse(definition.defaultValue().value());
    }

    public static <T> T getForTeam(MinecraftServer server, TeamId team, SettingDefinition<T> definition) {
        SettingDefinition<T> scopedDefinition = findForScope(definition, team.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN);
        return CadmusSaveData.getSettingValue(server, team, scopedDefinition).value();
    }

    public static boolean isEnabledAt(Level level, ChunkPos pos, SettingDefinition<Boolean> definition) {
        return getAt(level, pos, definition);
    }

    public static boolean isPlayerAllowed(Level level, UUID player, TeamId team, SettingDefinition<Boolean> definition) {
        if (definition.target() != earth.terrarium.cadmus.api.settings.SettingTarget.PLAYER) {
            return getForTeam(level.getServer(), team, definition);
        }
        if (!TeamApi.API.isMember(level, new com.mojang.authlib.GameProfile(player, ""), team)) return false;
        if (!level.isClientSide()) {
            SettingOverride override = CadmusSaveData.getPlayerSettingOverride(level.getServer(), team, player, definition);
            if (override == SettingOverride.ALLOW) return true;
            if (override == SettingOverride.DENY) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> SettingDefinition<T> findForScope(SettingDefinition<T> definition, SettingScope scope) {
        Map<String, SettingDefinition<?>> definitions = SettingDefinitions.forScope(scope);
        return (SettingDefinition<T>) definitions.getOrDefault(definition.id(), definition);
    }
}
