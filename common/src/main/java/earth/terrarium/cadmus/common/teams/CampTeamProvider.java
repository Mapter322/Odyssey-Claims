package earth.terrarium.cadmus.common.teams;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.api.teams.TeamProvider;
import earth.terrarium.cadmus.common.camps.CampSaveData;
import earth.terrarium.cadmus.common.utils.ModUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CampTeamProvider implements TeamProvider {
    public static final ResourceLocation ID = Cadmus.id("camp");
    private static final Color CAMP_COLOR = new Color(0xFFFFFFFF);

    private static PartyResolver partyResolver = new PartyResolver() {
        @Override
        public boolean isInSameParty(Level level, UUID owner, UUID player) {
            return false;
        }
    };

    public static void setPartyResolver(PartyResolver resolver) {
        partyResolver = resolver;
    }

    public static TeamId teamId(UUID owner) {
        return new TeamId(ID, owner);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<Component> getName(Level level, UUID id) {
        var server = level.getServer();
        if (server == null) return Optional.empty();
        CampSaveData.CampEntry camp = CampSaveData.read(server).camps().get(id);
        if (camp == null) return Optional.empty();
        var cache = server.getProfileCache();
        String name = cache == null ? "Unknown" : cache.get(id).map(profile -> profile.getName()).orElse("Unknown");
        return Optional.of(Component.translatable("text.cadmus.camp.name", name, ModUtils.formatTime(camp.expiresAt() - level.getGameTime())));
    }

    @Override
    public Optional<Color> getColor(Level level, UUID id) {
        return Optional.of(CAMP_COLOR);
    }

    @Override
    public Set<UUID> getMembers(Level level, UUID id) {
        Set<UUID> members = new HashSet<>();
        members.add(id);
        members.addAll(partyResolver.getPartyMembers(level, id));
        return members;
    }

    @Override
    public boolean isMember(Level level, UUID id, GameProfile player) {
        return id.equals(player.getId()) || partyResolver.isInSameParty(level, id, player.getId());
    }

    @Override
    public Set<UUID> getTeams(Level level, GameProfile player) {
        return Set.of();
    }

    @Override
    public boolean canModifySettings(Level level, UUID teamId, GameProfile player) {
        return teamId.equals(player.getId());
    }

    @Override
    public Set<UUID> getAllTeams(MinecraftServer server) {
        return Set.copyOf(CampSaveData.read(server).camps().keySet());
    }

    public interface PartyResolver {

        boolean isInSameParty(Level level, UUID owner, UUID player);

        default Set<UUID> getPartyMembers(Level level, UUID owner) {
            return Set.of();
        }
    }
}
