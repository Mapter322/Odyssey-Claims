package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.client.CadmusClient;
import java.util.Map;
import java.util.UUID;

public record SyncMemberSettingsPacket(TeamId team, UUID player, Map<String, String> settings) implements Packet<SyncMemberSettingsPacket> {
    public static final ClientboundPacketType<SyncMemberSettingsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_member_settings"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(SyncMemberSettingsPacket::team),
            ByteCodec.UUID.fieldOf(SyncMemberSettingsPacket::player),
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(SyncMemberSettingsPacket::settings),
            SyncMemberSettingsPacket::new
        ),
        NetworkHandle.handle(CadmusClient::syncMemberSettings)
    );

    @Override
    public ClientboundPacketType<SyncMemberSettingsPacket> type() {
        return TYPE;
    }
}
