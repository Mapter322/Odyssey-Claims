package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.api.flags.types.ColorFlag;
import earth.terrarium.cadmus.api.flags.types.StringFlag;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.common.flags.Flags;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.api.teams.TeamId;
import com.teamresourceful.bytecodecs.base.ByteCodec;

public record UpdateAdminClaimInfoPacket(TeamId id, String name, Color color, String motd) implements Packet<UpdateAdminClaimInfoPacket> {
    public static final ServerboundPacketType<UpdateAdminClaimInfoPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("update_admin_claim_info"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(UpdateAdminClaimInfoPacket::id),
            ByteCodec.STRING.fieldOf(UpdateAdminClaimInfoPacket::name),
            Color.BYTE_CODEC.fieldOf(UpdateAdminClaimInfoPacket::color),
            ByteCodec.STRING.fieldOf(UpdateAdminClaimInfoPacket::motd),
            UpdateAdminClaimInfoPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!player.hasPermissions(2) || !packet.id().isAdmin() || !packet.id().id().equals(AdminTeamProvider.ADMIN_ID)) return;
            String name = packet.name().strip();
            String motd = packet.motd().strip();
            if (name.isBlank() || name.length() > 32 || motd.length() > 64) return;
            FlagApi.API.setFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.DISPLAY_NAME.id(), new StringFlag(Flags.DISPLAY_NAME.id(), name));
            FlagApi.API.setFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.COLOR.id(), new ColorFlag(Flags.COLOR.id(), packet.color()));
            FlagApi.API.setFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.MOTD.id(), new StringFlag(Flags.MOTD.id(), motd));
            TeamApi.API.syncTeamInfo(player.getServer(), TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID), true);
        })
    );

    @Override
    public PacketType<UpdateAdminClaimInfoPacket> type() {
        return TYPE;
    }
}
