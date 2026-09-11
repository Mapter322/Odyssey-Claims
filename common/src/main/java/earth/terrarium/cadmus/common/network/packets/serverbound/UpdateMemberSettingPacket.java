package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncMemberSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;

public record UpdateMemberSettingPacket(TeamId team, UUID player, String setting, String value) implements Packet<UpdateMemberSettingPacket> {
    public static final ServerboundPacketType<UpdateMemberSettingPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("update_member_setting"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(UpdateMemberSettingPacket::team),
            ByteCodec.UUID.fieldOf(UpdateMemberSettingPacket::player),
            ByteCodec.STRING.fieldOf(UpdateMemberSettingPacket::setting),
            ByteCodec.STRING.fieldOf(UpdateMemberSettingPacket::value),
            UpdateMemberSettingPacket::new
        ),
        NetworkHandle.handle((packet, actor) -> {
            if (!(actor instanceof ServerPlayer serverPlayer) || !TeamApi.API.canModifySettings(serverPlayer, packet.team())) return;
            var definition = SettingDefinitions.forScope(packet.team().isAdmin() ? earth.terrarium.cadmus.api.settings.SettingScope.ADMIN_CLAIM : earth.terrarium.cadmus.api.settings.SettingScope.TOWN).get(packet.setting());
            if (definition == null || definition.target() != SettingTarget.PLAYER || packet.team().isAdmin() || !TeamApi.API.getMembers(serverPlayer.level(), packet.team()).contains(packet.player())) return;
            SettingOverride override;
            try {
                override = SettingOverride.valueOf(packet.value());
            } catch (IllegalArgumentException ignored) {
                return;
            }
            CadmusSaveData.setPlayerSettingOverride(serverPlayer.getServer(), packet.team(), packet.player(), definition, override);
            NetworkHandler.CHANNEL.sendToPlayer(new SyncMemberSettingsPacket(packet.team(), packet.player(), Map.of(packet.setting(), override.name())), serverPlayer);
        })
    );

    @Override
    public PacketType<UpdateMemberSettingPacket> type() {
        return TYPE;
    }
}
