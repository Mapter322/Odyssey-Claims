package earth.terrarium.cadmus.common.utils;

import com.mojang.authlib.GameProfile;
import earth.terrarium.argonauts.api.NotificationApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CadmusNotifications {

    public static final String NO_ACCESS = "text.cadmus.no_access";
    private static final long COOLDOWN_MS = 1500;
    private static final Map<UUID, Long> LAST_NOTIFIED = new HashMap<>();

    private CadmusNotifications() {
    }

    public static void noAccess(Level level, GameProfile player) {
        if (level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getId());
        if (serverPlayer == null) return;

        long now = System.currentTimeMillis();
        LAST_NOTIFIED.entrySet().removeIf(entry -> now - entry.getValue() > 10_000);
        Long last = LAST_NOTIFIED.get(player.getId());
        if (last != null && now - last < COOLDOWN_MS) return;
        LAST_NOTIFIED.put(player.getId(), now);
        NotificationApi.notify(serverPlayer, NO_ACCESS);
    }
}
