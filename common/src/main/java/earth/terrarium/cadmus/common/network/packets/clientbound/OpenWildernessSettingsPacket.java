package earth.terrarium.cadmus.common.network.packets.clientbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.client.CadmusClient;

import java.util.Map;
import java.util.Set;

public record OpenWildernessSettingsPacket(Map<String, String> settings, Set<String> conditions) implements Packet<OpenWildernessSettingsPacket> {
    public static final ClientboundPacketType<OpenWildernessSettingsPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("open_wilderness_settings"),
        ObjectByteCodec.create(
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(OpenWildernessSettingsPacket::settings),
            ByteCodec.STRING.setOf().fieldOf(OpenWildernessSettingsPacket::conditions),
            OpenWildernessSettingsPacket::new
        ),
        NetworkHandle.handle(CadmusClient::openWildernessSettings)
    );

    @Override
    public ClientboundPacketType<OpenWildernessSettingsPacket> type() {
        return TYPE;
    }
}
