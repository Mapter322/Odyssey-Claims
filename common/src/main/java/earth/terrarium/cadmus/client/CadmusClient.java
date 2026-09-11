package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.claims.ClaimSaveData;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.ChatClaimPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.TownActionPacket;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncClaimSettingsPacket;
import earth.terrarium.cadmus.common.teams.TeamInfo;
import earth.terrarium.argonauts.client.NotificationManager;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class CadmusClient {

    public static final Map<TeamId, TeamInfo> TEAM_INFO = new HashMap<>();
    public static final Map<UUID, ClientTown> TOWNS = new HashMap<>();

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
    }

    public static void openClaimMap() {
        Minecraft.getInstance().setScreen(new ClaimMapScreen());
    }

public static void openClaimSettings(SyncClaimSettingsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ClaimMapScreen)) {
            minecraft.setScreen(new ClaimMapScreen());
        }
        if (minecraft.screen instanceof ClaimMapScreen screen) {
            minecraft.setScreen(new ClaimConfigModal(screen, packet.id(), packet.settings()));
        }
    }

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
        if (!(minecraft.screen instanceof ClaimMapScreen)) {
            minecraft.setScreen(new ClaimMapScreen());
        }
        if (minecraft.screen instanceof ClaimMapScreen screen) {
            minecraft.setScreen(new AdminClaimConfigModal(screen, packet));
        }
    }

    public static void sendTownCreate(String name, ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_CREATE, name, start, end));
    }

    public static void sendTownAdd(UUID town, ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_ADD, town.toString(), start, end));
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
}
