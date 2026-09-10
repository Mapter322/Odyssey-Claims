package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;

public record BulkClaimSettingsPacket(TeamId id, Map<String, String> values, Set<String> resets) implements Packet<BulkClaimSettingsPacket> {
    public static final ServerboundPacketType<BulkClaimSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("update_bulk_claim_settings"),
        ObjectByteCodec.create(
            TeamId.BYTE_CODEC.fieldOf(BulkClaimSettingsPacket::id),
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(BulkClaimSettingsPacket::values),
            ByteCodec.STRING.setOf().fieldOf(BulkClaimSettingsPacket::resets),
            BulkClaimSettingsPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            SettingScope scope = packet.id().isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN;
            packet.values().forEach((setting, value) -> apply(packet, player, scope, setting, value));
            packet.resets().forEach(setting -> reset(packet, player, scope, setting));
        })
    );

    private static void apply(BulkClaimSettingsPacket packet, Player player, SettingScope scope, String setting, String value) {
        SettingDefinition<?> definition = SettingDefinitions.forScope(scope).get(setting);
        if (definition == null || !canModify(player, definition, packet.id())) return;
        try {
            SettingCommandSupport.set(player.getServer(), packet.id(), definition, SettingCommandSupport.parse(definition, value));
        } catch (CommandSyntaxException ignored) {
        }
    }

    private static void reset(BulkClaimSettingsPacket packet, Player player, SettingScope scope, String setting) {
        SettingDefinition<?> definition = SettingDefinitions.forScope(scope).get(setting);
        if (definition == null || !canModify(player, definition, packet.id())) return;
        CadmusSaveData.resetSettingValue(player.getServer(), packet.id(), definition);
    }

    private static boolean canModify(Player player, SettingDefinition<?> definition, TeamId id) {
        if (definition.access() == SettingAccess.ADMIN) return player.hasPermissions(2);
        return player.hasPermissions(2) || TeamApi.API.canModifySettings(player, id);
    }

    @Override
    public PacketType<BulkClaimSettingsPacket> type() {
        return TYPE;
    }
}