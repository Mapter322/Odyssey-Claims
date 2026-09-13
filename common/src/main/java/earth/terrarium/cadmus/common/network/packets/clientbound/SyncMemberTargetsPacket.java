package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.client.CadmusClient;

import java.util.List;

public record SyncMemberTargetsPacket(List<String> targets) implements Packet<SyncMemberTargetsPacket> {

    public static final ClientboundPacketType<SyncMemberTargetsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_member_targets"),
        ObjectByteCodec.create(
            ByteCodec.STRING.listOf().fieldOf(SyncMemberTargetsPacket::targets),
            SyncMemberTargetsPacket::new
        ),
        NetworkHandle.handle(packet -> CadmusClient.syncMemberTargets(packet.targets()))
    );

    @Override
    public ClientboundPacketType<SyncMemberTargetsPacket> type() {
        return TYPE;
    }
}
