package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.admin.AdminClaimCommands;
import earth.terrarium.cadmus.common.settings.AdminClaimTargets;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.TargetConditions;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record ModifyAdminClaimConditionPacket(String condition, boolean added) implements Packet<ModifyAdminClaimConditionPacket> {
    public static final ServerboundPacketType<ModifyAdminClaimConditionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("modify_admin_claim_condition"),
        ObjectByteCodec.create(
            ByteCodec.STRING.fieldOf(ModifyAdminClaimConditionPacket::condition),
            ByteCodec.BOOLEAN.fieldOf(ModifyAdminClaimConditionPacket::added),
            ModifyAdminClaimConditionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.hasPermissions(2)) return;
            MinecraftServer server = serverPlayer.getServer();
            if (server == null) return;

            String condition = packet.condition();
            String parent = TargetConditions.parentOf(condition);
            String key = TargetConditions.keyOf(condition);
            if (parent == null || key == null || TargetConditions.create(parent, key) == null) return;

            TeamId id = TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
            if (packet.added()) {
                if (SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).containsKey(condition)) return;
                CadmusSaveData.addCondition(server, id, condition);
                AdminClaimTargets.register(condition);
            } else {
                if (!CadmusSaveData.getConditions(server, id).contains(condition)) return;
                CadmusSaveData.removeCondition(server, id, condition);
                if (!CadmusSaveData.getAllConditions(server).contains(condition)) {
                    SettingDefinition<?> definition = SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).get(condition);
                    if (definition != null) CadmusSaveData.resetSettingValue(server, id, definition);
                    AdminClaimTargets.unregister(condition);
                }
            }
            AdminClaimCommands.sendSettings(serverPlayer);
        })
    );

    @Override
    public PacketType<ModifyAdminClaimConditionPacket> type() {
        return TYPE;
    }
}
