package earth.terrarium.cadmus.mixins.common.neoforge.compat.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEditPacket;
import earth.terrarium.cadmus.common.compat.create.CreateProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClipboardEditPacket.class)
public class ClipboardEditPacketMixin {

    @Shadow(remap = false)
    private BlockPos targetedBlock;

    @Inject(method = "handle", remap = false, at = @At("HEAD"), cancellable = true)
    private void cadmus$onHandle(ServerPlayer player, CallbackInfo ci) {
        if (player == null) return;
        if (!CreateProtectionHelper.canInteract(player, targetedBlock)) ci.cancel();
    }
}
