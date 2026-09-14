package earth.terrarium.cadmus.common.camps;

import earth.terrarium.argonauts.api.NotificationApi;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.events.CadmusEvents;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncCampsPacket;
import earth.terrarium.cadmus.common.teams.CampTeamProvider;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.stream.Collectors;

public final class CampManager {

    public static final String ERR_TOO_CLOSE = "cadmus.camp.too_close";

    private CampManager() {
    }

    public static void init() {
        CadmusEvents.RemoveClaimsEvent.register((level, id, positions) -> onClaimRemoved(level, id));
        CadmusEvents.ClearClaimsEvent.register((level, id) -> onClaimRemoved(level, id));
    }

    @Nullable
    public static String create(ServerPlayer player, ChunkPos pos) {
        MinecraftServer server = player.server;
        CampSaveData data = CampSaveData.read(server);
        if (data.camps().containsKey(player.getUUID())) return "cadmus.camp.already_active";

        ServerLevel level = player.serverLevel();
        if (ClaimApi.API.getClaim(level, pos).isPresent()) return TownManager.ERR_CHUNK_CLAIMED;
        if (isTooCloseToOtherClaims(level, pos)) return ERR_TOO_CLOSE;

        TeamId camp = CampTeamProvider.teamId(player.getUUID());
        ClaimApi.API.claim(level, camp, pos, false);

        long expiresAt = level.getGameTime() + CadmusConfig.get().personalCampDurationSeconds * 20L;
        data.camps().put(player.getUUID(), new CampSaveData.CampEntry(level.dimension().location(), pos, expiresAt));
        data.setDirty();
        sync(server);
        return null;
    }

    public static void remove(ServerPlayer player) {
        MinecraftServer server = player.server;
        CampSaveData data = CampSaveData.read(server);
        CampSaveData.CampEntry camp = data.camps().remove(player.getUUID());
        if (camp == null) {
            NotificationApi.notify(player, "cadmus.camp.none");
            return;
        }

        data.setDirty();
        removeClaim(server, player.getUUID(), camp);
        sync(server);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        expire(server);
    }

    public static void prune(MinecraftServer server) {
        expire(server);
    }

    private static void expire(MinecraftServer server) {
        CampSaveData data = CampSaveData.read(server);
        if (data.camps().isEmpty()) return;

        long gameTime = server.overworld().getGameTime();
        boolean dirty = false;
        var iterator = data.camps().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().expiresAt() > gameTime) continue;

            iterator.remove();
            dirty = true;
            removeClaim(server, entry.getKey(), entry.getValue());

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) NotificationApi.notify(player, "cadmus.camp.expired");
        }
        if (dirty) {
            data.setDirty();
            sync(server);
        }
    }

    private static boolean isTooCloseToOtherClaims(ServerLevel level, ChunkPos pos) {
        int minDistance = CadmusConfig.get().minChunksBetweenTowns;
        if (minDistance <= 1) return false;
        for (ChunkPos other : ClaimApi.API.getAllClaims(level).keySet()) {
            if (Math.max(Math.abs(pos.x - other.x), Math.abs(pos.z - other.z)) < minDistance) return true;
        }
        return false;
    }

    public static void sync(MinecraftServer server) {
        NetworkHandler.sendToAllClientPlayers(new SyncCampsPacket(encode(server)), server);
    }

    public static void sync(ServerPlayer player) {
        if (!NetworkHandler.CHANNEL.canSendToPlayer(player, SyncCampsPacket.TYPE)) return;
        NetworkHandler.CHANNEL.sendToPlayer(new SyncCampsPacket(encode(player.server)), player);
    }

    private static String encode(MinecraftServer server) {
        var cache = server.getProfileCache();
        return CampSaveData.read(server).camps().entrySet().stream()
            .map(entry -> {
                String name = cache == null ? "Unknown" : cache.get(entry.getKey()).map(profile -> profile.getName()).orElse("Unknown");
                return entry.getKey() + "|" + name + "|" + entry.getValue().expiresAt();
            })
            .collect(Collectors.joining("/"));
    }

    private static void removeClaim(MinecraftServer server, UUID owner, CampSaveData.CampEntry camp) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, camp.dimension()));
        if (level == null) return;

        TeamId campId = CampTeamProvider.teamId(owner);
        ClaimApi.API.getClaim(level, camp.pos())
            .filter(claim -> claim.team().equals(campId))
            .ifPresent(claim -> ClaimApi.API.unclaim(level, campId, camp.pos()));
    }

    private static void onClaimRemoved(Level level, TeamId id) {
        if (!CampTeamProvider.ID.equals(id.provider())) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;

        CampSaveData data = CampSaveData.read(server);
        if (data.camps().remove(id.id()) != null) {
            data.setDirty();
            sync(server);
        }
    }
}
