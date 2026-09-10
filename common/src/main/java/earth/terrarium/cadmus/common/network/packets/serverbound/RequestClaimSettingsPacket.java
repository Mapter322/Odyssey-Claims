package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncClaimSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public record RequestClaimSettingsPacket(TeamId id) implements Packet<RequestClaimSettingsPacket> {
    public static final ServerboundPacketType<RequestClaimSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("request_claim_settings"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(RequestClaimSettingsPacket::id),
            RequestClaimSettingsPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!player.hasPermissions(2) && !TeamApi.API.canModifySettings(player, packet.id())) return;
            Map<String, String> settings = new HashMap<>();
            SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
                if (definition.access() == SettingAccess.ADMIN && !player.hasPermissions(2)) return;
                settings.put(id, SettingCommandSupport.valueToString(
                    CadmusSaveData.getSettingValue(player.getServer(), packet.id(), definition)));
            });
            NetworkHandler.CHANNEL.sendToPlayer(new SyncClaimSettingsPacket(packet.id(), settings), player);
        })
    );

    @Override
    public PacketType<RequestClaimSettingsPacket> type() {
        return TYPE;
    }
}