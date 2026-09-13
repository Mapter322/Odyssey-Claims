package earth.terrarium.cadmus.common.settings;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

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

    @SuppressWarnings("unchecked")
    public static <T> T getForTeam(MinecraftServer server, TeamId team, SettingDefinition<T> definition) {
        SettingScope scope = team.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN;
        return (T) getValue(server, team, definition, scope).value();
    }

    private static SettingValue<?> getValue(MinecraftServer server, TeamId team, SettingDefinition<?> definition, SettingScope scope) {
        SettingDefinition<?> scoped = SettingDefinitions.forScope(scope).getOrDefault(definition.id(), definition);
        if (CadmusSaveData.hasSettingValue(server, team, scoped)) {
            return CadmusSaveData.getSettingValue(server, team, scoped);
        }
        if (scoped.hasParent()) {
            SettingDefinition<?> parent = SettingDefinitions.forScope(scope).get(scoped.parent());
            if (parent != null) return getValue(server, team, parent, scope);
        }
        return scoped.defaultValue();
    }

    public static boolean isEnabledAt(Level level, ChunkPos pos, SettingDefinition<Boolean> definition) {
        return getAt(level, pos, definition);
    }

    public static boolean isPlayerAllowed(Level level, UUID player, TeamId team, SettingDefinition<Boolean> definition) {
        if (definition.target() != SettingTarget.PLAYER) {
            return getForTeam(level.getServer(), team, definition);
        }
        if (!level.isClientSide()) {
            Optional<TriState> value = resolvePlayerSetting(level, team, player, definition);
            if (value.isPresent()) return value.get() == TriState.TRUE;
            SettingOverride override = CadmusSaveData.getPlayerSettingOverride(level.getServer(), team, player, definition);
            if (override == SettingOverride.ALLOW) return true;
            if (override == SettingOverride.DENY) return false;
        }
        if (!TeamApi.API.isMember(level, new com.mojang.authlib.GameProfile(player, ""), team)) return false;
        return true;
    }

    private static Optional<TriState> resolvePlayerSetting(Level level, TeamId team, UUID player, SettingDefinition<?> definition) {
        Optional<TriState> specific = RoleSettingsResolver.resolve(level, team, player, definition.id());
        if (specific.isEmpty()) return Optional.empty();
        TriState value = specific.get();
        if (value == TriState.UNDEFINED && definition.hasParent()) {
            value = RoleSettingsResolver.resolve(level, team, player, definition.parent()).orElse(TriState.UNDEFINED);
        }
        return Optional.of(value);
    }
}
