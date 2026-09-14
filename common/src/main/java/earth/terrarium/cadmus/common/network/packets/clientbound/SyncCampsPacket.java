package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.client.CadmusClient;

public record SyncCampsPacket(String data) implements Packet<SyncCampsPacket> {
    public static final ClientboundPacketType<SyncCampsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_camps"),
        ObjectByteCodec.create(ByteCodec.STRING.fieldOf(SyncCampsPacket::data), SyncCampsPacket::new),
        NetworkHandle.handle(packet -> CadmusClient.updateCamps(packet.data()))
    );

    @Override
    public PacketType<SyncCampsPacket> type() {
        return TYPE;
    }
}
