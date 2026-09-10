package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.ClaimMapNotificationPacket;
import earth.terrarium.cadmus.common.towns.TownManager;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;

public record AdminClaimActionPacket(ChunkPos start, ChunkPos end, boolean claim) implements Packet<AdminClaimActionPacket> {
    public static final ServerboundPacketType<AdminClaimActionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("admin_claim_action"),
        ObjectByteCodec.create(
            ExtraByteCodecs.CHUNK_POS.fieldOf(AdminClaimActionPacket::start),
            ExtraByteCodecs.CHUNK_POS.fieldOf(AdminClaimActionPacket::end),
            ByteCodec.BOOLEAN.fieldOf(AdminClaimActionPacket::claim),
            AdminClaimActionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.hasPermissions(2)) return;

            TeamId admin = TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
            Set<ChunkPos> positions = ChunkPos.rangeClosed(packet.start(), packet.end()).collect(java.util.stream.Collectors.toSet());
            if (packet.claim()) {
                if (positions.stream().anyMatch(pos -> ClaimApi.API.getClaim(serverPlayer.level(), pos).isPresent())) {
                    notify(serverPlayer, TownManager.ERR_CHUNK_CLAIMED);
                    return;
                }
                var claims = new Object2BooleanArrayMap<ChunkPos>();
                positions.forEach(pos -> claims.put(pos, false));
                ClaimApi.API.claim(serverPlayer.level(), admin, claims);
            } else {
                Set<ChunkPos> owned = new HashSet<>();
                positions.forEach(pos -> ClaimApi.API.getClaim(serverPlayer.level(), pos)
                    .filter(claim -> claim.team().equals(admin))
                    .ifPresent(ignored -> owned.add(pos)));
                ClaimApi.API.unclaim(serverPlayer.level(), admin, owned);
            }
        })
    );

    private static void notify(ServerPlayer player, String key) {
        if (NetworkHandler.CHANNEL.canSendToPlayer(player, ClaimMapNotificationPacket.TYPE)) {
            NetworkHandler.CHANNEL.sendToPlayer(new ClaimMapNotificationPacket(key), player);
        }
    }

    @Override
    public PacketType<AdminClaimActionPacket> type() {
        return TYPE;
    }
}
