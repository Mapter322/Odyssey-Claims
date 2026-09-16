package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.claims.ClaimSaveData;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.compat.argonauts.CadmusRoleTargets;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.ChatClaimPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.ForceloadActionPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.RequestClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.TownActionPacket;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncMemberSettingsPacket;
import earth.terrarium.cadmus.common.teams.TeamInfo;
import earth.terrarium.argonauts.client.NotificationManager;
import earth.terrarium.argonauts.client.screens.members.MembersScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class CadmusClient {

    public static final Map<TeamId, TeamInfo> TEAM_INFO = new HashMap<>();
    public static final Map<UUID, ClientTown> TOWNS = new HashMap<>();
    public static final Map<UUID, ClientCamp> CAMPS = new HashMap<>();
    public static final Map<MemberSettingKey, Map<String, String>> MEMBER_SETTINGS = new HashMap<>();

    public static final KeyMapping KEY_OPEN_CLAIM_MAP = new KeyMapping(
        ConstantComponents.OPEN_CLAIM_MAP_KEY.getString(),
        InputConstants.KEY_M,
        ConstantComponents.PROJECT_ODYSSEY_CATEGORY.getString());

    public static void init() {}

    public static void onClientTick() {
        if (KEY_OPEN_CLAIM_MAP.consumeClick()) {
            openClaimMap();
        }
    }

    public static void onPlayerLoggedOut() {
        ClaimSaveData.clearClientClaims();
        TEAM_INFO.clear();
        TOWNS.clear();
        CAMPS.clear();
        MEMBER_SETTINGS.clear();
        CadmusRoleTargets.clearClient();
    }

    public static void openClaimMap() {
        Minecraft.getInstance().setScreen(new ClaimMapScreen());
    }

    public static void openClaimMap(Screen parentScreen) {
        Minecraft.getInstance().setScreen(new ClaimMapScreen(parentScreen));
    }

    public static void openClaimSettings(SyncClaimSettingsPacket packet) {
        Minecraft.getInstance().setScreen(new ClaimSettingsScreen(packet));
    }

    public static void requestGuildClaimSettings(UUID guildId) {
        TeamId team = new TeamId(ResourceLocation.fromNamespaceAndPath("argonauts", "team"), guildId);
        NetworkHandler.CHANNEL.sendToServer(new RequestClaimSettingsPacket(new ClaimSettingsTarget(team)));
    }

    public static void syncMemberSettings(SyncMemberSettingsPacket packet) {
        MEMBER_SETTINGS.computeIfAbsent(new MemberSettingKey(packet.team(), packet.player()), ignored -> new HashMap<>()).putAll(packet.settings());
        if (Minecraft.getInstance().screen instanceof MembersScreen screen) screen.refreshMemberSettings();
    }

    public static void syncMemberTargets(List<String> targets) {
        CadmusRoleTargets.syncClient(targets);
    }

    public record MemberSettingKey(TeamId team, UUID player) {}

    public static void onEnterSection() {
        if (Minecraft.getInstance().screen instanceof ClaimMapScreen screen) {
            screen.refresh();
            screen.refreshMap();
        }
    }

    @NotNull
    public static Level level() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }

    public static Player player() {
        return Minecraft.getInstance().player;
    }

    public static void sendClaimCommand(ClaimCommandType type, TeamId id, String command) {
        NetworkHandler.CHANNEL.sendToServer(new ChatClaimPacket(type, id.asArg() + " " + command));
    }

    public static void sendTeamlessClaimCommand(ClaimCommandType type, String command) {
        NetworkHandler.CHANNEL.sendToServer(new ChatClaimPacket(type, command));
    }

    public static void openAdminClaimSettings(OpenAdminClaimSettingsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AdminClaimSettingsScreen screen && screen.matches(packet.id())) {
            screen.refresh(packet);
        } else {
            minecraft.setScreen(new AdminClaimSettingsScreen(packet));
        }
    }

    public static void sendTownCreate(String name, ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_CREATE, name, start, end));
    }

    public static void sendTownAdd(UUID town, ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_ADD, town.toString(), start, end));
    }

    public static void sendForceload(ChunkPos pos, boolean state) {
        NetworkHandler.CHANNEL.sendToServer(new ForceloadActionPacket(pos, state));
    }

    public static void showNotification(Component message) {
        NotificationManager.show(message);
    }

    public static void updateTowns(String encoded) {
        TOWNS.clear();
        if (encoded.isBlank()) return;
        for (String townValue : encoded.split("/")) {
            String[] fields = townValue.split("\\|", 5);
            if (fields.length != 5) continue;
            try {
                UUID townId = UUID.fromString(fields[0]);
                TeamId team = new TeamId(ResourceLocation.parse(fields[1]), UUID.fromString(fields[2]));
                Set<ChunkPos> chunks = new HashSet<>();
                if (!fields[4].isBlank()) {
                    for (String chunk : fields[4].split(";")) {
                        String[] position = chunk.split(",", 2);
                        chunks.add(new ChunkPos(Integer.parseInt(position[0]), Integer.parseInt(position[1])));
                    }
                }
                TOWNS.put(townId, new ClientTown(townId, team, fields[3], chunks));
            } catch (RuntimeException ignored) {
                // Ignore malformed data from an incompatible server.
            }
        }
        if (Minecraft.getInstance().screen instanceof ClaimMapScreen screen) screen.refresh();
    }

    public record ClientTown(UUID id, TeamId team, String name, Set<ChunkPos> chunks) {
        public Component displayName() {
            return Component.literal(name.isBlank() ? "Town " + id : name);
        }
    }

    public static void updateCamps(String encoded) {
        CAMPS.clear();
        if (encoded.isBlank()) return;
        for (String campValue : encoded.split("/")) {
            String[] fields = campValue.split("\\|", 3);
            if (fields.length != 3) continue;
            try {
                CAMPS.put(UUID.fromString(fields[0]), new ClientCamp(fields[1], Long.parseLong(fields[2])));
            } catch (RuntimeException ignored) {
                // Ignore malformed data from an incompatible server.
            }
        }
    }

    public record ClientCamp(String name, long expiresAt) {}
}
