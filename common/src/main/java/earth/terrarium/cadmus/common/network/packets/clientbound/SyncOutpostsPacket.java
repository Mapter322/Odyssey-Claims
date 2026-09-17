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

public record SyncOutpostsPacket(String data) implements Packet<SyncOutpostsPacket> {
    public static final ClientboundPacketType<SyncOutpostsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_outposts"),
        ObjectByteCodec.create(ByteCodec.STRING.fieldOf(SyncOutpostsPacket::data), SyncOutpostsPacket::new),
        NetworkHandle.handle(packet -> CadmusClient.updateOutposts(packet.data()))
    );

    @Override
    public PacketType<SyncOutpostsPacket> type() {
        return TYPE;
    }
}
