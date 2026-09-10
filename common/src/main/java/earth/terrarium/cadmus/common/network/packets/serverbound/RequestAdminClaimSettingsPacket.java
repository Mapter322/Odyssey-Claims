package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.commands.admin.AdminClaimCommands;
import net.minecraft.server.level.ServerPlayer;

public record RequestAdminClaimSettingsPacket() implements Packet<RequestAdminClaimSettingsPacket> {
    public static final ServerboundPacketType<RequestAdminClaimSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("request_admin_claim_settings"),
        ByteCodec.unit(RequestAdminClaimSettingsPacket::new),
        NetworkHandle.handle((packet, player) -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2)) {
                AdminClaimCommands.sendSettings(serverPlayer);
            }
        })
    );

    @Override
    public PacketType<RequestAdminClaimSettingsPacket> type() {
        return TYPE;
    }
}
