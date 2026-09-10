package earth.terrarium.cadmus.mixins.client.compat.xaero;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import earth.terrarium.cadmus.client.compat.xaero.CadmusRightClickOptions;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.map.controls.ControlsRegister;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.SupportMods;
import xaero.map.mods.pac.SupportOpenPartiesAndClaims;

import java.util.ArrayList;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin {

    @Shadow
    private MapTileSelection mapTileSelection;

    @ModifyExpressionValue(
        method = "init",
        at = @At(value = "INVOKE",
                 target = "Lxaero/map/mods/SupportMods;pac()Z",
                 ordinal = 0
        )
    )
    private boolean cadmus$allowClaims(boolean original) {
        return true;
    }

    @ModifyExpressionValue(
        method = "init",
        at = @At(value = "INVOKE",
                 target = "Lxaero/map/mods/SupportMods;pac()Z",
                 ordinal = 1
        )
    )
    private boolean cadmus$showClaimsButton(boolean original) {
        return true;
    }

    @Redirect(
        method = "init",
        at = @At(value = "INVOKE",
                 target = "Lxaero/map/mods/pac/SupportOpenPartiesAndClaims;getPacClaimsKeyBinding()Lnet/minecraft/client/KeyMapping;"
        )
    )
    private KeyMapping cadmus$allowClaimsKeyMapping(SupportOpenPartiesAndClaims instance) {
        return SupportMods.minimap() ? SupportMods.xaeroMinimap.getToggleClaimsKey() : ControlsRegister.keyTogglePacChunkClaims;
    }

    @Inject(
        method = "getRightClickOptions",
        at = @At(value = "INVOKE",
                 target = "Lxaero/map/mods/SupportMods;pac()Z",
                 shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void cadmus$getRightClickOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir, @Local ArrayList<RightClickOption> options) {
        CadmusRightClickOptions.addRightClickOptions(
            (GuiMap) (Object) this,
            options,
            this.mapTileSelection
        );
    }
}