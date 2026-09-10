package earth.terrarium.cadmus.common.utils;

import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class AdminUtils {

    public static void preventAdminChunkEntry(ServerPlayer player, ChunkPos lastChunkPos) {
        if (lastChunkPos == null) return;
        if (CadmusSaveData.canBypass(player)) return;
        if (player.isSpectator() || Settings.isEnabledAt(player.serverLevel(), player.chunkPosition(), SettingDefinitions.ALLOW_ENTRY)) {
            return;
        }

        String message = Settings.getAt(player.level(), player.chunkPosition(), SettingDefinitions.ENTRY_DENY_MESSAGE);
        if (!message.isBlank()) {
            player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }

        BlockPos currentPos = player.blockPosition();
        BlockPos lastPos = lastChunkPos.getMiddleBlockPosition(player.getBlockY());
        BlockPos betweenPos = new BlockPos(
            currentPos.getX() + (lastPos.getX() - currentPos.getX()) / 4,
            currentPos.getY(),
            currentPos.getZ() + (lastPos.getZ() - currentPos.getZ()) / 4
        );

        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(betweenPos.getX(), betweenPos.getY(), betweenPos.getZ());
    }

    public static void preventAdminChunkExit(ServerPlayer player, ChunkPos lastChunkPos) {
        if (lastChunkPos == null) return;
        if (CadmusSaveData.canBypass(player)) return;
        if (player.isSpectator() || Settings.isEnabledAt(player.serverLevel(), lastChunkPos, SettingDefinitions.ALLOW_EXIT)) {
            return;
        }

        String message = Settings.getAt(player.level(), lastChunkPos, SettingDefinitions.EXIT_DENY_MESSAGE);
        if (!message.isBlank()) {
            player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }

        BlockPos currentPos = player.blockPosition();
        BlockPos lastPos = lastChunkPos.getMiddleBlockPosition(player.getBlockY());
        BlockPos betweenPos = new BlockPos(
            currentPos.getX() + (lastPos.getX() - currentPos.getX()) / 4,
            currentPos.getY(),
            currentPos.getZ() + (lastPos.getZ() - currentPos.getZ()) / 4
        );

        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(betweenPos.getX(), betweenPos.getY(), betweenPos.getZ());
    }

    public static void checkAccess(ServerPlayer player, ChunkPos lastChunkPos) {
        preventAdminChunkEntry(player, lastChunkPos);
        preventAdminChunkExit(player, lastChunkPos);
    }
}
