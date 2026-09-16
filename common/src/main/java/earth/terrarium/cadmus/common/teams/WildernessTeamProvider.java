package earth.terrarium.cadmus.common.teams;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.api.teams.TeamProvider;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WildernessTeamProvider implements TeamProvider {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Cadmus.MOD_ID, "wilderness");
    public static final UUID WILDERNESS_ID = new UUID(0L, 0L);
    private static final Color COLOR = new Color(0xFFFFFFFF);

    public static TeamId team() {
        return new TeamId(ID, WILDERNESS_ID);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<Component> getName(Level level, UUID id) {
        return Optional.of(ConstantComponents.WILDERNESS);
    }

    @Override
    public Optional<Color> getColor(Level level, UUID id) {
        return Optional.of(COLOR);
    }

    @Override
    public Set<UUID> getMembers(Level level, UUID id) {
        return Set.of();
    }

    @Override
    public boolean isMember(Level level, UUID id, GameProfile player) {
        return false;
    }

    @Override
    public Set<UUID> getTeams(Level level, GameProfile player) {
        return Set.of();
    }

    @Override
    public boolean canModifySettings(Level level, UUID teamId, GameProfile player) {
        return level.getServer() != null && level.getServer().getProfilePermissions(player) >= 2;
    }

    @Override
    public Set<UUID> getAllTeams(MinecraftServer server) {
        return Set.of(WILDERNESS_ID);
    }
}
