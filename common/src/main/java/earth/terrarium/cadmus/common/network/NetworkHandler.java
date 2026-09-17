package earth.terrarium.cadmus.common.network;


import com.teamresourceful.resourcefullib.common.network.Network;
import com.teamresourceful.resourcefullib.common.network.Packet;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.network.packets.clientbound.*;
import earth.terrarium.cadmus.common.network.packets.serverbound.*;
import net.minecraft.server.MinecraftServer;

public class NetworkHandler {

    public static final Network CHANNEL = new Network(Cadmus.id("main"), 1, true);

    public static void init() {
        CHANNEL.register(AddBulkClaimsPacket.TYPE);
        CHANNEL.register(AddClaimPacket.TYPE);
        CHANNEL.register(ClearClaimsPacket.TYPE);
        CHANNEL.register(RemoveBulkClaimsPacket.TYPE);
        CHANNEL.register(RemoveClaimPacket.TYPE);
        CHANNEL.register(SyncAllMaxClaimsPacket.TYPE);
        CHANNEL.register(SyncAllTeamInfoPacket.TYPE);
        CHANNEL.register(SyncClaimSettingsPacket.TYPE);
        CHANNEL.register(SyncClaimsPacket.TYPE);
        CHANNEL.register(SyncMaxClaimsPacket.TYPE);
        CHANNEL.register(SyncTeamInfo.TYPE);
        CHANNEL.register(SyncTownsPacket.TYPE);
        CHANNEL.register(SyncCampsPacket.TYPE);
        CHANNEL.register(SyncOutpostsPacket.TYPE);
        CHANNEL.register(OpenAdminClaimSettingsPacket.TYPE);
        CHANNEL.register(OpenWildernessSettingsPacket.TYPE);

        CHANNEL.register(BulkClaimSettingsPacket.TYPE);
        CHANNEL.register(ChatClaimPacket.TYPE);
        CHANNEL.register(RequestClaimSettingsPacket.TYPE);
        CHANNEL.register(RequestAdminClaimSettingsPacket.TYPE);
        CHANNEL.register(TownActionPacket.TYPE);
        CHANNEL.register(OutpostActionPacket.TYPE);
        CHANNEL.register(AdminClaimActionPacket.TYPE);
        CHANNEL.register(CampActionPacket.TYPE);
        CHANNEL.register(ForceloadActionPacket.TYPE);
        CHANNEL.register(UpdateAdminClaimInfoPacket.TYPE);
        CHANNEL.register(ModifyAdminClaimConditionPacket.TYPE);
        CHANNEL.register(ModifyWildernessConditionPacket.TYPE);
        CHANNEL.register(SyncMemberSettingsPacket.TYPE);
        CHANNEL.register(SyncMemberTargetsPacket.TYPE);
        CHANNEL.register(RequestMemberSettingsPacket.TYPE);
        CHANNEL.register(UpdateMemberSettingPacket.TYPE);
    }

    /**
     * Sends to all clients that have Cadmus installed
     */
    public static <T extends Packet<T>> void sendToAllClientPlayers(T packet, MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            if (CHANNEL.canSendToPlayer(player, packet.type())) {
                CHANNEL.sendToPlayer(packet, player);
            }
        });
    }
}
