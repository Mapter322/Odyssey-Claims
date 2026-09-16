package earth.terrarium.cadmus.common.settings;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.config.AdminClaimDefaultsConfig;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.towns.Town;
import earth.terrarium.cadmus.common.towns.TownManager;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class Settings {

    private Settings() {
    }

    public static <T> T getAt(Level level, ChunkPos pos, SettingDefinition<T> definition) {
        if (level.isClientSide() || level.getServer() == null) return definition.defaultValue().value();

        MinecraftServer server = level.getServer();
        return ClaimApi.API.getClaim(level, pos)
            .map(ClaimData::team)
            .map(team -> {
                Town town = TownManager.getTownAt(server, team, pos);
                UUID townId = town == null ? null : town.id();
                return (T) resolve(server, team, townId, definition).value();
            })
            .orElse(definition.defaultValue().value());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getForTeam(MinecraftServer server, TeamId team, SettingDefinition<T> definition) {
        return (T) resolve(server, team, null, definition).value();
    }

    /**
     * Resolves the effective value for the given owner (guild when {@code townId} is null, otherwise town).
     */
    public static SettingValue<?> resolve(MinecraftServer server, TeamId team, @Nullable UUID townId, SettingDefinition<?> definition) {
        SettingScope scope = scopeOf(team);
        SettingDefinition<?> scoped = scoped(scope, definition);
        if (townId != null) {
            SettingValue<?> value = findValue(scoped, scope, d ->
                CadmusSaveData.hasTownSettingValue(server, townId, d) ? CadmusSaveData.getTownSettingValue(server, townId, d) : null);
            if (value != null) return value;
        }
        return resolveTeam(server, team, scoped, scope);
    }

    /**
     * Resolves the value inherited by the given owner from the levels below it.
     */
    public static SettingValue<?> resolveInherited(MinecraftServer server, TeamId team, @Nullable UUID townId, SettingDefinition<?> definition) {
        SettingScope scope = scopeOf(team);
        SettingDefinition<?> scoped = scoped(scope, definition);
        if (townId == null) return resolveDefaults(scoped, scope);
        return resolveTeam(server, team, scoped, scope);
    }

    /**
     * The configured or code default value, treated as an assigned value on the top level.
     */
    public static SettingValue<?> defaults(TeamId team, SettingDefinition<?> definition) {
        SettingScope scope = scopeOf(team);
        return resolveDefaults(scoped(scope, definition), scope);
    }

    private static SettingValue<?> resolveTeam(MinecraftServer server, TeamId team, SettingDefinition<?> scoped, SettingScope scope) {
        SettingValue<?> value = findValue(scoped, scope, d ->
            CadmusSaveData.hasSettingValue(server, team, d) ? CadmusSaveData.getSettingValue(server, team, d) : null);
        return value != null ? value : resolveDefaults(scoped, scope);
    }

    private static SettingValue<?> resolveDefaults(SettingDefinition<?> scoped, SettingScope scope) {
        SettingValue<?> value = findValue(scoped, scope, Settings::configValue);
        return value != null ? value : scoped.defaultValue();
    }

    @Nullable
    private static SettingValue<?> findValue(SettingDefinition<?> definition, SettingScope scope, Function<SettingDefinition<?>, SettingValue<?>> lookup) {
        SettingValue<?> value = lookup.apply(definition);
        if (value != null) return value;
        if (!definition.hasParent()) return null;
        SettingDefinition<?> parent = SettingDefinitions.forScope(scope).get(definition.parent());
        return parent == null ? null : findValue(parent, scope, lookup);
    }

    @Nullable
    private static SettingValue<?> configValue(SettingDefinition<?> definition) {
        if (definition.scope() == SettingScope.ADMIN_CLAIM) {
            Boolean value = AdminClaimDefaultsConfig.get(definition.id());
            return value == null ? null : new BooleanSetting(value);
        }
        Map<String, Boolean> defaults = CadmusConfig.get().defaultClaimSettings;
        if (defaults == null || !(definition.defaultValue() instanceof BooleanSetting)) return null;
        Boolean value = defaults.get(definition.id());
        return value == null ? null : new BooleanSetting(value);
    }

    private static SettingScope scopeOf(TeamId team) {
        return team.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN;
    }

    private static SettingDefinition<?> scoped(SettingScope scope, SettingDefinition<?> definition) {
        return SettingDefinitions.forScope(scope).getOrDefault(definition.id(), definition);
    }

    public static boolean isEnabledAt(Level level, ChunkPos pos, SettingDefinition<Boolean> definition) {
        return getAt(level, pos, definition);
    }

    public static boolean isPlayerAllowed(Level level, UUID player, TeamId team, SettingDefinition<Boolean> definition) {
        return isPlayerAllowed(level, player, team, null, definition);
    }

    public static boolean isPlayerAllowed(Level level, UUID player, TeamId team, @Nullable ChunkPos pos, SettingDefinition<Boolean> definition) {
        if (definition.target() != SettingTarget.PLAYER || (!level.isClientSide() && team.isAdmin())) {
            return pos == null ? getForTeam(level.getServer(), team, definition) : getAt(level, pos, definition);
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
