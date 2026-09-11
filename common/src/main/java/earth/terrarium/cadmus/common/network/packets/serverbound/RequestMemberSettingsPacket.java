package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncMemberSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record RequestMemberSettingsPacket(TeamId team, UUID player) implements Packet<RequestMemberSettingsPacket> {
    public static final ServerboundPacketType<RequestMemberSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("request_member_settings"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(RequestMemberSettingsPacket::team),
            ByteCodec.UUID.fieldOf(RequestMemberSettingsPacket::player),
            RequestMemberSettingsPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !canAccess(serverPlayer, packet.team(), packet.player())) return;
            Map<String, String> values = new HashMap<>();
            SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
                if (definition.target() != SettingTarget.PLAYER || definition.access() == SettingAccess.ADMIN) return;
                values.put(id, CadmusSaveData.getPlayerSettingOverride(serverPlayer.getServer(), packet.team(), packet.player(), definition).name());
            });
            NetworkHandler.CHANNEL.sendToPlayer(new SyncMemberSettingsPacket(packet.team(), packet.player(), values), serverPlayer);
        })
    );

    private static boolean canAccess(ServerPlayer actor, TeamId team, UUID target) {
        return TeamApi.API.canModifySettings(actor, team) && TeamApi.API.isMember(actor.level(), actor.getGameProfile(), team)
            && TeamApi.API.getMembers(actor.level(), team).contains(target);
    }

    @Override
    public PacketType<RequestMemberSettingsPacket> type() {
        return TYPE;
    }
}
