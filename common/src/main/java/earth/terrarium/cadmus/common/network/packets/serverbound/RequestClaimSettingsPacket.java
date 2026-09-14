package earth.terrarium.cadmus.common.network.packets.serverbound;

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
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncClaimSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.towns.TownManager;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record RequestClaimSettingsPacket(ClaimSettingsTarget target) implements Packet<RequestClaimSettingsPacket> {
    public static final ServerboundPacketType<RequestClaimSettingsPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("request_claim_settings"),
        ObjectByteCodec.create(
            ClaimSettingsTarget.BYTE_CODEC.fieldOf(RequestClaimSettingsPacket::target),
            RequestClaimSettingsPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            if (!player.hasPermissions(2) && !TeamApi.API.canModifySettings(player, packet.target().team())) return;

            UUID townId = packet.target().townId().orElse(null);
            if (townId != null && TownManager.getTown(server, townId).filter(town -> town.team().equals(packet.target().team())).isEmpty()) return;

            Map<String, String> values = new HashMap<>();
            Map<String, String> inherited = new HashMap<>();
            SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
                if (definition.target() != SettingTarget.GLOBAL) return;
                if (definition.access() == SettingAccess.ADMIN && !player.hasPermissions(2)) return;

                SettingValue<?> explicit = explicit(server, packet.target(), townId, definition);
                if (explicit != null) values.put(id, SettingCommandSupport.valueToString(explicit));
                inherited.put(id, SettingCommandSupport.valueToString(
                    Settings.resolveInherited(server, packet.target().team(), townId, definition)));
            });

            String name = townId == null
                ? TeamApi.API.getName(server, packet.target().team()).getString()
                : TownManager.getTown(server, townId).map(town -> town.name()).orElse("");
            boolean canEdit = player.hasPermissions(2) || TeamApi.API.canModifySettings(player, packet.target().team());
            NetworkHandler.CHANNEL.sendToPlayer(new SyncClaimSettingsPacket(packet.target(), name, canEdit, values, inherited), player);
        })
    );

    public RequestClaimSettingsPacket(TeamId id) {
        this(new ClaimSettingsTarget(id));
    }

    @Nullable
    private static SettingValue<?> explicit(MinecraftServer server, ClaimSettingsTarget target, @Nullable UUID townId, SettingDefinition<?> definition) {
        if (townId != null) {
            return CadmusSaveData.hasTownSettingValue(server, townId, definition) ? CadmusSaveData.getTownSettingValue(server, townId, definition) : null;
        }
        return CadmusSaveData.hasSettingValue(server, target.team(), definition) ? CadmusSaveData.getSettingValue(server, target.team(), definition) : null;
    }

    @Override
    public PacketType<RequestClaimSettingsPacket> type() {
        return TYPE;
    }
}
