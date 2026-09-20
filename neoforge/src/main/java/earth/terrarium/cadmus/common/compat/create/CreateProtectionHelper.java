package earth.terrarium.cadmus.common.compat.create;

import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CreateProtectionHelper {

    public static boolean isBreakingAllowed(@Nullable UUID placer, Level level, BlockPos pos) {
        return Protections.BLOCK_BREAKING.canNonPlayerBreak(level, placer, pos);
    }

    public static boolean canInteract(ServerPlayer player, BlockPos pos) {
        return Protections.BLOCK_INTERACTIONS.canInteractWithBlock(player, pos, player.level().getBlockState(pos));
    }

    private CreateProtectionHelper() {
    }
}
