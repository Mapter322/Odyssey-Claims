package earth.terrarium.cadmus.common.outposts;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.events.CadmusEvents;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncOutpostsPacket;
import earth.terrarium.cadmus.common.towns.TownManager;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class OutpostManager {

    public static final String ERR_TOO_CLOSE = "command.cadmus.exception.outpost.too_close";
    public static final String ERR_TOO_LARGE = "command.cadmus.exception.outpost.too_large";
    public static final String ERR_OUTPOST_LIMIT = "command.cadmus.exception.outpost.limit";

    private OutpostManager() {}

    public static void init() {
        CadmusEvents.RemoveClaimsEvent.register((level, id, positions) -> {
            MinecraftServer server = level.getServer();
            if (server == null) return;
            boolean changed = false;
            for (ChunkPos pos : positions) {
                changed |= removeChunk(server, level.dimension().location(), id, pos);
            }
            if (changed) sync(server);
        });
        CadmusEvents.ClearClaimsEvent.register((level, id) -> {
            MinecraftServer server = level.getServer();
            if (server == null) return;
            if (removeTeam(server, level.dimension().location(), id)) sync(server);
        });
    }

    public static Collection<Outpost> getOutposts(MinecraftServer server, TeamId team) {
        return OutpostSaveData.read(server).outposts().values().stream()
            .filter(outpost -> outpost.team().equals(team))
            .toList();
    }

    @Nullable
    public static Outpost getOutpostAt(MinecraftServer server, TeamId team, ChunkPos pos) {
        for (Outpost outpost : OutpostSaveData.read(server).outposts().values()) {
            if (outpost.team().equals(team) && outpost.chunks().contains(pos)) return outpost;
        }
        return null;
    }

    public static int getChunkCount(MinecraftServer server, TeamId team) {
        return getOutposts(server, team).stream().mapToInt(outpost -> outpost.chunks().size()).sum();
    }

    public static Component claim(ServerPlayer player, ChunkPos start, ChunkPos end) {
        TeamId team = TeamApi.API.getTeamsList(player).stream().findFirst().orElse(null);
        if (team == null) return Component.translatable(TownManager.ERR_NO_GUILD);
        if (!TeamApi.API.canManageClaims(player, team)) return Component.translatable(TownManager.ERR_NO_PERMISSION);
        int maxOutposts = TeamApi.API.getMaxOutpostChunks(player.level(), team);
        if (maxOutposts <= 0) return Component.translatable(TownManager.ERR_NO_GUILD);
        if (!CadmusConfig.get().isClaimingAllowed(player.level())) return Component.translatable(TownManager.ERR_DIMENSION_BLOCKED);

        ServerLevel level = player.serverLevel();
        for (ChunkPos pos : ChunkPos.rangeClosed(start, end).toList()) {
            if (ClaimApi.API.isClaimed(level, pos)) continue;
            Component error = claimChunk(player, level, team, pos, maxOutposts);
            if (error != null) return error;
        }
        return null;
    }

    private static Component claimChunk(ServerPlayer player, ServerLevel level, TeamId team, ChunkPos pos, int maxOutposts) {
        Outpost outpost = findNearbyOutpost(player.server, level, team, pos);
        if (outpost != null) {
            if (outpost.chunks().size() + 1 > CadmusConfig.get().maxOutpostSize) return Component.translatable(ERR_TOO_LARGE);
            int current = ClaimApi.API.getOwnedClaims(level, team).map(Map::size).orElse(0);
            if (current + 1 > ClaimLimitApi.API.getMaxClaims(team)) return Component.translatable(TownManager.ERR_CLAIM_LIMIT);
            if (getChunkCount(player.server, team) + 1 > maxOutposts) return Component.translatable(ERR_OUTPOST_LIMIT);
            if (isTooCloseToOtherClaims(level, Set.of(pos), outpost.chunks())) return Component.translatable(ERR_TOO_CLOSE);

            outpost.chunks().add(pos);
            OutpostSaveData.read(player.server).setDirty();
        } else {
            if (CadmusConfig.get().maxOutpostSize < 1) return Component.translatable(ERR_TOO_LARGE);
            int current = ClaimApi.API.getOwnedClaims(level, team).map(Map::size).orElse(0);
            if (current + 1 > ClaimLimitApi.API.getMaxClaims(team)) return Component.translatable(TownManager.ERR_CLAIM_LIMIT);
            if (getChunkCount(player.server, team) + 1 > maxOutposts) return Component.translatable(ERR_OUTPOST_LIMIT);
            if (isTooCloseToOtherClaims(level, Set.of(pos), null)) return Component.translatable(ERR_TOO_CLOSE);

            outpost = new Outpost(UUID.randomUUID(), team, level.dimension().location(), new HashSet<>(Set.of(pos)));
            OutpostSaveData.read(player.server).outposts().put(outpost.id(), outpost);
            OutpostSaveData.read(player.server).setDirty();
        }

        Object2BooleanOpenHashMap<ChunkPos> claims = new Object2BooleanOpenHashMap<>();
        claims.put(pos, false);
        ClaimApi.API.claim(level, team, claims);
        sync(player.server);
        return null;
    }

    @Nullable
    private static Outpost findNearbyOutpost(MinecraftServer server, ServerLevel level, TeamId team, ChunkPos pos) {
        ResourceLocation dimension = level.dimension().location();
        for (Outpost outpost : OutpostSaveData.read(server).outposts().values()) {
            if (!outpost.team().equals(team) || !outpost.dimension().equals(dimension)) continue;
            for (ChunkPos other : outpost.chunks()) {
                if (Math.max(Math.abs(pos.x - other.x), Math.abs(pos.z - other.z)) <= 1) return outpost;
            }
        }
        return null;
    }

    public static void prune(MinecraftServer server) {
        OutpostSaveData data = OutpostSaveData.read(server);
        if (data.outposts().isEmpty()) return;
        boolean dirty = false;
        var iterator = data.outposts().values().iterator();
        while (iterator.hasNext()) {
            Outpost outpost = iterator.next();
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, outpost.dimension()));
            if (level == null) {
                iterator.remove();
                dirty = true;
                continue;
            }
            int size = outpost.chunks().size();
            outpost.chunks().removeIf(pos -> !isOwned(level, outpost.team(), pos));
            if (outpost.chunks().isEmpty()) {
                iterator.remove();
                dirty = true;
            } else if (outpost.chunks().size() != size) {
                dirty = true;
            }
        }
        if (dirty) data.setDirty();
    }

    private static boolean isOwned(ServerLevel level, TeamId team, ChunkPos pos) {
        return ClaimApi.API.getClaim(level, pos).map(claim -> claim.team().equals(team)).orElse(false);
    }

    private static boolean removeChunk(MinecraftServer server, ResourceLocation dimension, TeamId team, ChunkPos pos) {
        OutpostSaveData data = OutpostSaveData.read(server);
        boolean changed = false;
        var iterator = data.outposts().values().iterator();
        while (iterator.hasNext()) {
            Outpost outpost = iterator.next();
            if (!outpost.team().equals(team) || !outpost.dimension().equals(dimension)) continue;
            if (!outpost.chunks().remove(pos)) continue;
            changed = true;
            if (outpost.chunks().isEmpty()) iterator.remove();
        }
        if (changed) data.setDirty();
        return changed;
    }

    private static boolean removeTeam(MinecraftServer server, ResourceLocation dimension, TeamId team) {
        OutpostSaveData data = OutpostSaveData.read(server);
        boolean changed = data.outposts().values()
            .removeIf(outpost -> outpost.team().equals(team) && outpost.dimension().equals(dimension));
        if (changed) data.setDirty();
        return changed;
    }

    private static boolean isTooCloseToOtherClaims(ServerLevel level, Set<ChunkPos> positions, @Nullable Set<ChunkPos> excludeChunks) {
        int minDistance = CadmusConfig.get().minChunksBetweenTowns;
        if (minDistance <= 1) return false;
        for (ChunkPos pos : positions) {
            for (ChunkPos other : ClaimApi.API.getAllClaims(level).keySet()) {
                if (excludeChunks != null && excludeChunks.contains(other)) continue;
                if (Math.max(Math.abs(pos.x - other.x), Math.abs(pos.z - other.z)) < minDistance) return true;
            }
        }
        return false;
    }

    public static void sync(MinecraftServer server) {
        NetworkHandler.sendToAllClientPlayers(new SyncOutpostsPacket(encode(server)), server);
    }

    public static void sync(ServerPlayer player) {
        if (!NetworkHandler.CHANNEL.canSendToPlayer(player, SyncOutpostsPacket.TYPE)) return;
        NetworkHandler.CHANNEL.sendToPlayer(new SyncOutpostsPacket(encode(player.server)), player);
    }

    private static String encode(MinecraftServer server) {
        return OutpostSaveData.read(server).outposts().values().stream()
            .map(outpost -> outpost.team().provider() + "|" + outpost.team().id() + "|" +
                outpost.chunks().stream().map(pos -> pos.x + "," + pos.z).collect(Collectors.joining(";")))
            .collect(Collectors.joining("/"));
    }
}
