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
import earth.terrarium.cadmus.common.commands.wilderness.WildernessCommands;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.TargetConditions;
import earth.terrarium.cadmus.common.settings.WildernessTargets;
import earth.terrarium.cadmus.common.teams.WildernessTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record ModifyWildernessConditionPacket(String condition, boolean added) implements Packet<ModifyWildernessConditionPacket> {
    public static final ServerboundPacketType<ModifyWildernessConditionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("modify_wilderness_condition"),
        ObjectByteCodec.create(
            ByteCodec.STRING.fieldOf(ModifyWildernessConditionPacket::condition),
            ByteCodec.BOOLEAN.fieldOf(ModifyWildernessConditionPacket::added),
            ModifyWildernessConditionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.hasPermissions(2)) return;
            MinecraftServer server = serverPlayer.getServer();
            if (server == null) return;

            String condition = packet.condition();
            String parent = TargetConditions.parentOf(condition);
            String key = TargetConditions.keyOf(condition);
            if (parent == null || key == null || TargetConditions.create(parent, key) == null) return;

            TeamId id = WildernessTeamProvider.team();
            if (packet.added()) {
                if (SettingDefinitions.forScope(SettingScope.WILDERNESS).containsKey(condition)) return;
                CadmusSaveData.addCondition(server, id, condition);
                WildernessTargets.register(condition);
            } else {
                if (!CadmusSaveData.getConditions(server, id).contains(condition)) return;
                CadmusSaveData.removeCondition(server, id, condition);
                SettingDefinition<?> definition = SettingDefinitions.forScope(SettingScope.WILDERNESS).get(condition);
                if (definition != null) CadmusSaveData.resetSettingValue(server, id, definition);
                WildernessTargets.unregister(condition);
            }
            WildernessCommands.sendSettings(serverPlayer);
        })
    );

    @Override
    public PacketType<ModifyWildernessConditionPacket> type() {
        return TYPE;
    }
}
