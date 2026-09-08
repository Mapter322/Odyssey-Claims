package earth.terrarium.cadmus.common.towns;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncTownsPacket;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public final class TownManager {
    public static final int MAX_TOWNS_PER_TEAM = 3;

    private TownManager() {}

    public static Collection<Town> getTowns(MinecraftServer server, TeamId team) {
        return CadmusSaveData.read(server).towns().values().stream()
            .filter(town -> town.team().equals(team))
            .toList();
    }

    public static Optional<Town> getTown(MinecraftServer server, UUID id) {
        return Optional.ofNullable(CadmusSaveData.read(server).towns().get(id));
    }

    public static boolean create(ServerPlayer player, ChunkPos start, ChunkPos end) {
        TeamId team = TeamApi.API.getTeamsList(player).stream().findFirst().orElse(null);
        if (team == null || !TeamApi.API.canModifySettings(player, team)) return false;
        if (getTowns(player.server, team).size() >= MAX_TOWNS_PER_TEAM) return false;

        Set<ChunkPos> positions = positions(start, end);
        if (positions.isEmpty() || positions.stream().anyMatch(pos -> ClaimApi.API.isClaimed(player.level(), pos))) return false;
        int current = ClaimApi.API.getOwnedClaims(player.level(), team).map(Map::size).orElse(0);
        if (current + positions.size() > ClaimLimitApi.API.getMaxClaims(team)) return false;

        Town town = new Town(UUID.randomUUID(), team);
        town.chunks().addAll(positions);
        CadmusSaveData data = CadmusSaveData.read(player.server);
        data.towns().put(town.id(), town);
        data.setDirty();

        Object2BooleanOpenHashMap<ChunkPos> claims = new Object2BooleanOpenHashMap<>();
        positions.forEach(pos -> claims.put(pos, false));
        ClaimApi.API.claim(player.level(), team, claims);
        sync(player.server);
        return true;
    }

    public static boolean add(ServerPlayer player, UUID townId, ChunkPos start, ChunkPos end) {
        Town town = getTown(player.server, townId).orElse(null);
        if (town == null || !TeamApi.API.canModifySettings(player, town.team())) return false;
        Set<ChunkPos> positions = positions(start, end);
        if (positions.isEmpty() || positions.stream().anyMatch(pos -> ClaimApi.API.isClaimed(player.level(), pos))) return false;
        Set<ChunkPos> reachable = new HashSet<>(town.chunks());
        boolean changed;
        do {
            changed = positions.stream().anyMatch(pos -> !reachable.contains(pos) && reachable.stream().anyMatch(existing ->
                Math.abs(existing.x - pos.x) + Math.abs(existing.z - pos.z) == 1) && reachable.add(pos));
        } while (changed);
        if (!reachable.containsAll(positions)) return false;
        int current = ClaimApi.API.getOwnedClaims(player.level(), town.team()).map(Map::size).orElse(0);
        if (current + positions.size() > ClaimLimitApi.API.getMaxClaims(town.team())) return false;

        town.chunks().addAll(positions);
        CadmusSaveData.read(player.server).setDirty();
        Object2BooleanOpenHashMap<ChunkPos> claims = new Object2BooleanOpenHashMap<>();
        positions.forEach(pos -> claims.put(pos, false));
        ClaimApi.API.claim(player.level(), town.team(), claims);
        sync(player.server);
        return true;
    }

    private static Set<ChunkPos> positions(ChunkPos start, ChunkPos end) {
        return ChunkPos.rangeClosed(start, end).collect(Collectors.toSet());
    }

    public static void removeChunk(Level level, TeamId team, ChunkPos pos) {
        if (!(level.getServer() instanceof MinecraftServer server)) return;
        CadmusSaveData data = CadmusSaveData.read(server);
        Town town = data.towns().values().stream()
            .filter(value -> value.team().equals(team) && value.chunks().contains(pos))
            .findFirst().orElse(null);
        if (town == null) return;
        town.chunks().remove(pos);
        if (town.chunks().isEmpty()) data.towns().remove(town.id());
        data.setDirty();
        sync(server);
    }

    public static void removeTeam(MinecraftServer server, TeamId team) {
        CadmusSaveData data = CadmusSaveData.read(server);
        data.towns().values().removeIf(town -> town.team().equals(team));
        data.setDirty();
        sync(server);
    }

    public static void sync(MinecraftServer server) {
        NetworkHandler.sendToAllClientPlayers(new SyncTownsPacket(encode(server)), server);
    }

    public static String encode(MinecraftServer server) {
        return CadmusSaveData.read(server).towns().values().stream()
            .map(town -> town.id() + "|" + town.team().provider() + "|" + town.team().id() + "|" +
                town.chunks().stream().map(pos -> pos.x + "," + pos.z).collect(Collectors.joining(";")))
            .collect(Collectors.joining("/"));
    }
}
