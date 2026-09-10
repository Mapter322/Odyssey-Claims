package earth.terrarium.cadmus.common.teams;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.api.teams.TeamProvider;
import earth.terrarium.cadmus.common.flags.Flags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AdminTeamProvider implements TeamProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Cadmus.MOD_ID, "admin");
    public static final UUID ADMIN_ID = UUID.nameUUIDFromBytes("admin".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    public static void ensureAdminTeam(MinecraftServer server) {
        if (!FlagApi.API.isAdminTeam(server, ADMIN_ID)) {
            FlagApi.API.createAdminTeam(server, "admin");
        }
        if (FlagApi.API.<String>getFlag(server, ADMIN_ID, Flags.DISPLAY_NAME.id()).value().isBlank()) {
            FlagApi.API.setFlag(server, ADMIN_ID, Flags.DISPLAY_NAME.id(), new earth.terrarium.cadmus.api.flags.types.StringFlag(Flags.DISPLAY_NAME.id(), "Admin Claim"));
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<Component> getName(Level level, UUID id) {
        return Optional.ofNullable(level.getServer()).map(server -> Component.literal(FlagApi.API.<String>getFlag(server, id, Flags.DISPLAY_NAME.id()).value()));
    }

    @Override
    public Optional<Color> getColor(Level level, UUID id) {
        return Optional.ofNullable(level.getServer()).map(server -> FlagApi.API.<Color>getFlag(server, id, Flags.COLOR.id()).value());
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
        ensureAdminTeam(server);
        return Set.of(ADMIN_ID);
    }
}
