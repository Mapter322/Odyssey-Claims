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
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.towns.TownManager;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record BulkClaimSettingsPacket(ClaimSettingsTarget target, Map<String, String> values, Set<String> resets) implements Packet<BulkClaimSettingsPacket> {
    public static final ServerboundPacketType<BulkClaimSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("update_bulk_claim_settings"),
        ObjectByteCodec.create(
            ClaimSettingsTarget.BYTE_CODEC.fieldOf(BulkClaimSettingsPacket::target),
            ByteCodec.mapOf(ByteCodec.STRING, ByteCodec.STRING).fieldOf(BulkClaimSettingsPacket::values),
            ByteCodec.STRING.setOf().fieldOf(BulkClaimSettingsPacket::resets),
            BulkClaimSettingsPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            TeamId teamId = packet.target().team();
            UUID townId = packet.target().townId().orElse(null);
            if (townId != null && TownManager.getTown(server, townId).filter(town -> town.team().equals(teamId)).isEmpty()) return;

            SettingScope scope = Settings.scopeOf(teamId);
            packet.values().forEach((setting, value) -> apply(packet, player, scope, teamId, townId, setting, value));
            packet.resets().forEach(setting -> reset(packet, player, scope, teamId, townId, setting));
        })
    );

    public BulkClaimSettingsPacket(TeamId id, Map<String, String> values, Set<String> resets) {
        this(new ClaimSettingsTarget(id), values, resets);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void apply(BulkClaimSettingsPacket packet, Player player, SettingScope scope, TeamId teamId, @Nullable UUID townId, String setting, String value) {
        SettingDefinition<?> definition = SettingDefinitions.forScope(scope).get(setting);
        if (definition == null || !canModify(player, definition, teamId)) return;
        if (definition.target() != SettingTarget.GLOBAL && !teamId.isAdmin() && !teamId.isWilderness()) return;
        try {
            SettingValue<?> parsed = SettingCommandSupport.parse(definition, value);
            if (townId == null) {
                SettingCommandSupport.set(player.getServer(), teamId, definition, parsed);
            } else {
                CadmusSaveData.setTownSettingValue(player.getServer(), townId, (SettingDefinition) definition, (SettingValue) parsed);
            }
        } catch (CommandSyntaxException ignored) {
        }
    }

    private static void reset(BulkClaimSettingsPacket packet, Player player, SettingScope scope, TeamId teamId, @Nullable UUID townId, String setting) {
        SettingDefinition<?> definition = SettingDefinitions.forScope(scope).get(setting);
        if (definition == null || !canModify(player, definition, teamId)) return;
        if (definition.target() != SettingTarget.GLOBAL && !teamId.isAdmin() && !teamId.isWilderness()) return;
        if (townId == null) {
            CadmusSaveData.resetSettingValue(player.getServer(), teamId, definition);
        } else {
            CadmusSaveData.resetTownSettingValue(player.getServer(), townId, definition);
        }
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
