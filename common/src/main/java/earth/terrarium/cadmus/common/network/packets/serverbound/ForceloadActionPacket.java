package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.api.NotificationApi;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public record ForceloadActionPacket(ChunkPos pos, boolean state) implements Packet<ForceloadActionPacket> {
    public static final ServerboundPacketType<ForceloadActionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("forceload_action"),
        ObjectByteCodec.create(
            ExtraByteCodecs.CHUNK_POS.fieldOf(ForceloadActionPacket::pos),
            ByteCodec.BOOLEAN.fieldOf(ForceloadActionPacket::state),
            ForceloadActionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                forceload(serverPlayer, packet.pos(), packet.state());
            }
        })
    );

    private static void forceload(ServerPlayer player, ChunkPos pos, boolean state) {
        var claim = ClaimApi.API.getClaim(player.serverLevel(), pos);
        if (claim.isEmpty()) {
            NotificationApi.notify(player, "command.cadmus.exception.not_claimed");
            return;
        }

        TeamId team = claim.get().team();
        if (team.isAdmin()) {
            if (!player.hasPermissions(2)) return;
        } else {
            if (!ClaimApi.API.getOwnedClaims(player).map(claims -> claims.containsKey(pos)).orElse(false)) {
                NotificationApi.notify(player, "command.cadmus.exception.not_owner");
                return;
            }
            if (!TeamApi.API.canManageClaims(player, team)) {
                NotificationApi.notify(player, "command.cadmus.exception.no_claim_permission");
                return;
            }
        }

        if (state && !claim.get().isChunkLoaded()
            && ClaimCommand.getClaimsCount(player.serverLevel(), team, true) >= ClaimLimitApi.API.getMaxChunkLoadedClaims(team)) {
            NotificationApi.notify(player, "command.cadmus.exception.forceload_limit");
            return;
        }

        ClaimApi.API.setChunkLoaded(player.serverLevel(), team, pos, state);
    }

    @Override
    public PacketType<ForceloadActionPacket> type() {
        return TYPE;
    }
}
