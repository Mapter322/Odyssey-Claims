package earth.terrarium.cadmus.common.towns;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncTownsPacket;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public final class TownManager {
    public static final int MAX_TOWNS_PER_TEAM = 3;
    public static final int MAX_TOWN_NAME_LENGTH = 32;

    public static final String ERR_NO_PERMISSION = "command.cadmus.exception.town.no_permission";
    public static final String ERR_MAX_TOWNS = "command.cadmus.exception.town.max_towns";
    public static final String ERR_CHUNK_CLAIMED = "command.cadmus.exception.town.chunk_claimed";
    public static final String ERR_CLAIM_LIMIT = "command.cadmus.exception.town.claim_limit";
    public static final String ERR_TOO_CLOSE = "command.cadmus.exception.town.too_close";
    public static final String ERR_NOT_ADJACENT = "command.cadmus.exception.town.not_adjacent";
    public static final String ERR_TOWN_NOT_FOUND = "command.cadmus.exception.town.not_found";
    public static final String ERR_TOWN_NAME = "command.cadmus.exception.town.invalid_name";

    private TownManager() {}

    public static Collection<Town> getTowns(MinecraftServer server, TeamId team) {
        return CadmusSaveData.read(server).towns().values().stream()
            .filter(town -> town.team().equals(team))
            .toList();
    }

    public static Optional<Town> getTown(MinecraftServer server, UUID id) {
        return Optional.ofNullable(CadmusSaveData.read(server).towns().get(id));
    }

    public static boolean isValidTownName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_TOWN_NAME_LENGTH) return false;
        return name.indexOf('|') < 0 && name.indexOf('/') < 0;
    }

    public static Component create(ServerPlayer player, ChunkPos start, ChunkPos end, String name) {
        TeamId team = TeamApi.API.getTeamsList(player).stream().findFirst().orElse(null);
        if (team == null || !TeamApi.API.canModifySettings(player, team)) return Component.translatable(ERR_NO_PERMISSION);
        name = name == null ? "" : name.strip();
        if (!isValidTownName(name)) return Component.translatable(ERR_TOWN_NAME);
        if (getTowns(player.server, team).size() >= MAX_TOWNS_PER_TEAM) return Component.translatable(ERR_MAX_TOWNS);

        Set<ChunkPos> positions = positions(start, end);
        if (positions.isEmpty() || positions.stream().anyMatch(pos -> ClaimApi.API.isClaimed(player.level(), pos))) return Component.translatable(ERR_CHUNK_CLAIMED);
        int current = ClaimApi.API.getOwnedClaims(player.level(), team).map(Map::size).orElse(0);
        if (current + positions.size() > ClaimLimitApi.API.getMaxClaims(team)) return Component.translatable(ERR_CLAIM_LIMIT);
        if (isTooCloseToOtherTowns(player.server, positions, null)) return Component.translatable(ERR_TOO_CLOSE);

        Town town = new Town(UUID.randomUUID(), team, name);
        town.chunks().addAll(positions);
        CadmusSaveData data = CadmusSaveData.read(player.server);
        data.towns().put(town.id(), town);
        data.setDirty();

        Object2BooleanOpenHashMap<ChunkPos> claims = new Object2BooleanOpenHashMap<>();
        positions.forEach(pos -> claims.put(pos, false));
        ClaimApi.API.claim(player.level(), team, claims);
        sync(player.server);
        return null;
    }

    public static Component add(ServerPlayer player, UUID townId, ChunkPos start, ChunkPos end) {
        Town town = getTown(player.server, townId).orElse(null);
        if (town == null || !TeamApi.API.canModifySettings(player, town.team())) return Component.translatable(town == null ? ERR_TOWN_NOT_FOUND : ERR_NO_PERMISSION);
        Set<ChunkPos> positions = positions(start, end);
        if (positions.isEmpty() || positions.stream().anyMatch(pos -> ClaimApi.API.isClaimed(player.level(), pos))) return Component.translatable(ERR_CHUNK_CLAIMED);
        Set<ChunkPos> reachable = new HashSet<>(town.chunks());
        boolean changed;
        do {
            changed = positions.stream().anyMatch(pos -> !reachable.contains(pos) && reachable.stream().anyMatch(existing ->
                Math.abs(existing.x - pos.x) + Math.abs(existing.z - pos.z) == 1) && reachable.add(pos));
        } while (changed);
        if (!reachable.containsAll(positions)) return Component.translatable(ERR_NOT_ADJACENT);
        int current = ClaimApi.API.getOwnedClaims(player.level(), town.team()).map(Map::size).orElse(0);
        if (current + positions.size() > ClaimLimitApi.API.getMaxClaims(town.team())) return Component.translatable(ERR_CLAIM_LIMIT);
        if (isTooCloseToOtherTowns(player.server, positions, townId)) return Component.translatable(ERR_TOO_CLOSE);

        town.chunks().addAll(positions);
        CadmusSaveData.read(player.server).setDirty();
        Object2BooleanOpenHashMap<ChunkPos> claims = new Object2BooleanOpenHashMap<>();
        positions.forEach(pos -> claims.put(pos, false));
        ClaimApi.API.claim(player.level(), town.team(), claims);
        sync(player.server);
        return null;
    }

    private static Set<ChunkPos> positions(ChunkPos start, ChunkPos end) {
        return ChunkPos.rangeClosed(start, end).collect(Collectors.toSet());
    }

    private static boolean isTooCloseToOtherTowns(MinecraftServer server, Set<ChunkPos> positions, UUID excludeTownId) {
        int minDistance = CadmusConfig.get().minChunksBetweenTowns;
        if (minDistance <= 1) return false;
        for (Town other : CadmusSaveData.read(server).towns().values()) {
            if (other.id().equals(excludeTownId)) continue;
            for (ChunkPos pos : positions) {
                for (ChunkPos otherPos : other.chunks()) {
                    if (Math.max(Math.abs(pos.x - otherPos.x), Math.abs(pos.z - otherPos.z)) < minDistance) return true;
                }
            }
        }
        return false;
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
            .map(town -> town.id() + "|" + town.team().provider() + "|" + town.team().id() + "|" + town.name() + "|" +
                town.chunks().stream().map(pos -> pos.x + "," + pos.z).collect(Collectors.joining(";")))
            .collect(Collectors.joining("/"));
    }
}
