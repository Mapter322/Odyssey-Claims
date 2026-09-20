package earth.terrarium.cadmus.common.compat.cbc;

import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import rbasamoyai.createbigcannons.events.ProjectileDamageEvent;

public final class CBCProtectionEvents {

    public static void register() {
        NeoForge.EVENT_BUS.register(CBCProtectionEvents.class);
    }

    public static boolean isProtectedPosition(Level level, BlockPos pos) {
        return !Protections.BLOCK_EXPLOSIONS.canExplodeBlock(level, pos);
    }

    @SubscribeEvent
    public static void onProjectileDamage(ProjectileDamageEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (isProtectedPosition(level, event.getPos())) event.setCanceled(true);
    }

    private CBCProtectionEvents() {
    }
}
