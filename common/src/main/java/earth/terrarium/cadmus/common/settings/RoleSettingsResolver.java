package earth.terrarium.cadmus.common.settings;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.teams.TeamId;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

///Bridge that lets the Argonauts compatibility layer resolve player settings through guild roles.

public final class RoleSettingsResolver {

    @FunctionalInterface
    public interface Resolver {
        Optional<TriState> resolve(Level level, TeamId team, UUID player, String settingId);
    }

    private static Resolver resolver;

    private RoleSettingsResolver() {
    }

    public static void set(Resolver resolver) {
        RoleSettingsResolver.resolver = resolver;
    }

    public static Optional<TriState> resolve(Level level, TeamId team, UUID player, String settingId) {
        Resolver resolver = RoleSettingsResolver.resolver;
        return resolver == null ? Optional.empty() : resolver.resolve(level, team, player, settingId);
    }
}
