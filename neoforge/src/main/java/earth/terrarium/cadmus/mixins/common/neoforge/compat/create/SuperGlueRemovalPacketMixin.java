package earth.terrarium.cadmus.mixins.common.neoforge.compat.create;

import com.simibubi.create.content.contraptions.glue.SuperGlueRemovalPacket;
import earth.terrarium.cadmus.common.compat.create.CreateProtectionHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SuperGlueRemovalPacket.class)
public class SuperGlueRemovalPacketMixin {

    @Shadow(remap = false)
    private int entityId;

    @Inject(method = "handle", remap = false, at = @At("HEAD"), cancellable = true)
    private void cadmus$onHandle(ServerPlayer player, CallbackInfo ci) {
        if (player == null) return;
        Entity entity = player.level().getEntity(entityId);
        if (entity == null) return;
        if (!CreateProtectionHelper.canInteract(player, entity.blockPosition())) ci.cancel();
    }
}
