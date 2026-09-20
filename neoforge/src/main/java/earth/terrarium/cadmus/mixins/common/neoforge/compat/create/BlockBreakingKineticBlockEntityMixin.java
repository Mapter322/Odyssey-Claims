package earth.terrarium.cadmus.mixins.common.neoforge.compat.create;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import earth.terrarium.cadmus.api.protections.PlacerTracked;
import earth.terrarium.cadmus.common.compat.create.CreateProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public class BlockBreakingKineticBlockEntityMixin {

    @Shadow(remap = false)
    protected BlockPos breakingPos;

    @ModifyVariable(
            method = "tick",
            remap = false,
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            name = "stateToBreak"
    )
    private BlockState cadmus$protectClaimedBlock(BlockState original) {
        BlockBreakingKineticBlockEntity self = (BlockBreakingKineticBlockEntity) (Object) this;
        Level level = self.getLevel();

        if (level == null || level.isClientSide() || breakingPos == null) {
            return original;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return original;
        }

        UUID placer = self instanceof PlacerTracked tracked ? tracked.cadmus$getPlacerUUID() : null;
        if (CreateProtectionHelper.isBreakingAllowed(placer, serverLevel, breakingPos)) {
            return original;
        }

        return Blocks.BEDROCK.defaultBlockState();
    }
}
