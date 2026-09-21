package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.settings.ChunkRef;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            if (player instanceof ServerPlayer serverPlayer) send(serverPlayer, packet.target());
        })
    );

    public static void send(ServerPlayer player, ClaimSettingsTarget target) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        if (!player.hasPermissions(2) && !TeamApi.API.canModifySettings(player, target.team())) return;

        if (target.chunk() != null) {
            sendChunk(player, server, target);
            return;
        }

        UUID townId = target.townId().orElse(null);
        if (townId != null && TownManager.getTown(server, townId).filter(town -> town.team().equals(target.team())).isEmpty()) return;

        Map<String, String> values = new HashMap<>();
        Map<String, String> inherited = new HashMap<>();
        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            if (definition.access() == SettingAccess.ADMIN && !player.hasPermissions(2)) return;

            if (townId == null) {
                SettingValue<?> value = explicit(server, target, null, definition);
                if (value == null) value = Settings.defaults(target.team(), definition);
                values.put(id, SettingCommandSupport.valueToString(value));
            } else {
                SettingValue<?> value = explicit(server, target, townId, definition);
                if (value != null) values.put(id, SettingCommandSupport.valueToString(value));
                inherited.put(id, SettingCommandSupport.valueToString(
                    Settings.resolveInherited(server, target.team(), townId, definition)));
            }
        });

        String name = townId == null
            ? TeamApi.API.getName(server, target.team()).getString()
            : TownManager.getTown(server, townId).map(town -> town.name()).orElse("");
        boolean canEdit = player.hasPermissions(2) || TeamApi.API.canModifySettings(player, target.team());
        NetworkHandler.CHANNEL.sendToPlayer(new SyncClaimSettingsPacket(target, name, canEdit, values, inherited), player);
    }

    public RequestClaimSettingsPacket(TeamId id) {
        this(new ClaimSettingsTarget(id));
    }

    private static void sendChunk(ServerPlayer player, MinecraftServer server, ClaimSettingsTarget target) {
        ChunkRef chunk = target.chunk();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, chunk.dimension()));
        if (level == null) return;
        ClaimData claim = ClaimApi.API.getClaim(level, chunk.pos()).orElse(null);
        if (claim == null || !claim.team().equals(target.team())) return;

        var town = TownManager.getTownAt(server, target.team(), chunk.pos());
        UUID townId = town == null ? null : town.id();
        ClaimSettingsTarget resolved = new ClaimSettingsTarget(
            target.team(), townId == null ? ClaimSettingsTarget.GLOBAL : townId, chunk);

        Map<String, String> values = new HashMap<>();
        Map<String, String> inherited = new HashMap<>();
        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            if (definition.access() == SettingAccess.ADMIN && !player.hasPermissions(2)) return;

            if (CadmusSaveData.hasChunkSettingValue(server, chunk, definition)) {
                values.put(id, SettingCommandSupport.valueToString(CadmusSaveData.getChunkSettingValue(server, chunk, definition)));
            }
            inherited.put(id, SettingCommandSupport.valueToString(
                Settings.resolve(server, target.team(), townId, definition)));
        });
        values.put(Settings.CHUNK_NAME, CadmusSaveData.getChunkName(server, chunk).orElse(""));

        String name = CadmusSaveData.getChunkName(server, chunk)
            .filter(value -> !value.isBlank())
            .orElse("[" + chunk.pos().x + ", " + chunk.pos().z + "]");
        boolean canEdit = player.hasPermissions(2) || TeamApi.API.canModifySettings(player, target.team());
        NetworkHandler.CHANNEL.sendToPlayer(new SyncClaimSettingsPacket(resolved, name, canEdit, values, inherited), player);
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
