package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.client.CadmusClient;

import java.util.Map;

public record SyncClaimSettingsPacket(
    ClaimSettingsTarget target,
    String name,
    boolean canEdit,
    Map<String, String> values,
    Map<String, String> inherited
) implements Packet<SyncClaimSettingsPacket> {

    public static final ClientboundPacketType<SyncClaimSettingsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("sync_claim_settings"),
        ObjectByteCodec.create(
            ClaimSettingsTarget.BYTE_CODEC.fieldOf(SyncClaimSettingsPacket::target),
            ByteCodec.STRING.fieldOf(SyncClaimSettingsPacket::name),
            ByteCodec.BOOLEAN.fieldOf(SyncClaimSettingsPacket::canEdit),
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(SyncClaimSettingsPacket::values),
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(SyncClaimSettingsPacket::inherited),
            SyncClaimSettingsPacket::new
        ),
        NetworkHandle.handle(CadmusClient::openClaimSettings)
    );

    @Override
    public ClientboundPacketType<SyncClaimSettingsPacket> type() {
        return TYPE;
    }
}
