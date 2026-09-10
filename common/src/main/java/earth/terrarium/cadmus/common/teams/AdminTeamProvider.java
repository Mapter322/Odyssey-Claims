package earth.terrarium.cadmus.common.teams;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.api.settings.types.StringSetting;
import earth.terrarium.cadmus.api.teams.TeamProvider;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
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
        TeamId id = TeamId.ofAdmin(ADMIN_ID);
        if (CadmusSaveData.getSettingValue(server, id, SettingDefinitions.DISPLAY_NAME).value().isBlank()) {
            CadmusSaveData.setSettingValue(server, id, SettingDefinitions.DISPLAY_NAME, new StringSetting("Admin Claim"));
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<Component> getName(Level level, UUID id) {
        return Optional.ofNullable(level.getServer())
            .map(server -> Component.literal(CadmusSaveData.getSettingValue(server, TeamId.ofAdmin(id), SettingDefinitions.DISPLAY_NAME).value()));
    }

    @Override
    public Optional<Color> getColor(Level level, UUID id) {
        return Optional.ofNullable(level.getServer())
            .map(server -> CadmusSaveData.getSettingValue(server, TeamId.ofAdmin(id), SettingDefinitions.COLOR).value());
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