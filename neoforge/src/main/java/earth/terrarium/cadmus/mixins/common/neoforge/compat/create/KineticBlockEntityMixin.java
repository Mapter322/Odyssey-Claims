package earth.terrarium.cadmus.mixins.common.neoforge.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import earth.terrarium.cadmus.api.protections.PlacerTracked;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = KineticBlockEntity.class, remap = false)
public class KineticBlockEntityMixin implements PlacerTracked {

    @Unique
    private UUID cadmus$placerUUID;

    @Override
    public UUID cadmus$getPlacerUUID() {
        return cadmus$placerUUID;
    }

    @Override
    public void cadmus$setPlacerUUID(UUID uuid) {
        this.cadmus$placerUUID = uuid;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void cadmus$onWrite(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (cadmus$placerUUID != null) {
            compound.putUUID(PlacerTracked.PLACER_KEY, cadmus$placerUUID);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void cadmus$onRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (compound.hasUUID(PlacerTracked.PLACER_KEY)) {
            cadmus$placerUUID = compound.getUUID(PlacerTracked.PLACER_KEY);
        }
    }
}
