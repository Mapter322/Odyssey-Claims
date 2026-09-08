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
import net.minecraft.network.chat.Component;

public record ClaimMapNotificationPacket(String key) implements Packet<ClaimMapNotificationPacket> {
    public static final ClientboundPacketType<ClaimMapNotificationPacket> TYPE = CodecPacketType.Client.create(
        Cadmus.id("claim_map_notification"),
        ObjectByteCodec.create(ByteCodec.STRING.fieldOf(ClaimMapNotificationPacket::key), ClaimMapNotificationPacket::new),
        NetworkHandle.handle(packet -> CadmusClient.showNotification(Component.translatable(packet.key())))
    );

    @Override
    public PacketType<ClaimMapNotificationPacket> type() {
        return TYPE;
    }
}
