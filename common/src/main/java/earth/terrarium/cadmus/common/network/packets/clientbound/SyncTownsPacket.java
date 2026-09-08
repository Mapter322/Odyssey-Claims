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

public record SyncTownsPacket(String data) implements Packet<SyncTownsPacket> {
    public static final ClientboundPacketType<SyncTownsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_towns"),
        ObjectByteCodec.create(ByteCodec.STRING.fieldOf(SyncTownsPacket::data), SyncTownsPacket::new),
        NetworkHandle.handle(packet -> CadmusClient.updateTowns(packet.data()))
    );

    @Override
    public PacketType<SyncTownsPacket> type() {
        return TYPE;
    }
}
