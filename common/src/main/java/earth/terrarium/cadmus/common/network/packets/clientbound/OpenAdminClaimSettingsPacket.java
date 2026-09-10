package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.client.CadmusClient;

import java.util.Map;

public record OpenAdminClaimSettingsPacket(TeamId id, String name, Color color, String motd, Map<String, TriState> settings) implements Packet<OpenAdminClaimSettingsPacket> {
    public static final ClientboundPacketType<OpenAdminClaimSettingsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("open_admin_claim_settings"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(OpenAdminClaimSettingsPacket::id),
            ByteCodec.STRING.fieldOf(OpenAdminClaimSettingsPacket::name),
            Color.BYTE_CODEC.fieldOf(OpenAdminClaimSettingsPacket::color),
            ByteCodec.STRING.fieldOf(OpenAdminClaimSettingsPacket::motd),
            ByteCodec.mapOf(ByteCodec.STRING, TriState.BYTE_CODEC).fieldOf(OpenAdminClaimSettingsPacket::settings),
            OpenAdminClaimSettingsPacket::new
        ),
        NetworkHandle.handle(CadmusClient::openAdminClaimSettings)
    );

    @Override
    public ClientboundPacketType<OpenAdminClaimSettingsPacket> type() {
        return TYPE;
    }
}
