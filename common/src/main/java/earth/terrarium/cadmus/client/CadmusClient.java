package earth.terrarium.cadmus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.claims.ClaimSaveData;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.ChatClaimPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.TownActionPacket;
import earth.terrarium.cadmus.common.protections.SettingsData;
import earth.terrarium.cadmus.common.teams.TeamInfo;
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

    public static void updateClaimMapSettings(Map<TeamId, SettingsData> settings) {
        if(Minecraft.getInstance().screen instanceof ClaimMapScreen screen) {
            screen.updateSettings(settings);
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

    public static void sendTownCreate(ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_CREATE, "", start, end));
    }

    public static void sendTownAdd(UUID town, ChunkPos start, ChunkPos end) {
        NetworkHandler.CHANNEL.sendToServer(new TownActionPacket(ClaimCommandType.TOWN_ADD, town.toString(), start, end));
    }

    public static void showNotification(Component message) {
        if (Minecraft.getInstance().screen instanceof ClaimMapScreen screen) {
            screen.showNotification(message);
        } else if (player() != null) {
            player().displayClientMessage(message, true);
        }
    }

    public static void updateTowns(String encoded) {
        TOWNS.clear();
        if (encoded.isBlank()) return;
        for (String townValue : encoded.split("/")) {
            String[] fields = townValue.split("\\|", 4);
            if (fields.length != 4) continue;
            try {
                UUID townId = UUID.fromString(fields[0]);
                TeamId team = new TeamId(ResourceLocation.parse(fields[1]), UUID.fromString(fields[2]));
                Set<ChunkPos> chunks = new HashSet<>();
                if (!fields[3].isBlank()) {
                    for (String chunk : fields[3].split(";")) {
                        String[] position = chunk.split(",", 2);
                        chunks.add(new ChunkPos(Integer.parseInt(position[0]), Integer.parseInt(position[1])));
                    }
                }
                TOWNS.put(townId, new ClientTown(townId, team, chunks));
            } catch (RuntimeException ignored) {
                // Ignore malformed data from an incompatible server.
            }
        }
        if (Minecraft.getInstance().screen instanceof ClaimMapScreen screen) screen.refresh();
    }

    public record ClientTown(UUID id, TeamId team, Set<ChunkPos> chunks) {
        public Component name() {
            return Component.literal("Town " + id);
        }
    }
}
