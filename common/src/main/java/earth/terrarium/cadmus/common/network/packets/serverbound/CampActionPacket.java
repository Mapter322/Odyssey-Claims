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
import earth.terrarium.cadmus.common.camps.CampManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public record CampActionPacket(ChunkPos pos, boolean claim) implements Packet<CampActionPacket> {
    public static final ServerboundPacketType<CampActionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("camp_action"),
        ObjectByteCodec.create(
            ExtraByteCodecs.CHUNK_POS.fieldOf(CampActionPacket::pos),
            ByteCodec.BOOLEAN.fieldOf(CampActionPacket::claim),
            CampActionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (packet.claim()) {
                CampManager.create(serverPlayer, packet.pos());
            } else {
                CampManager.remove(serverPlayer);
            }
        })
    );

    @Override
    public PacketType<CampActionPacket> type() {
        return TYPE;
    }
}
