package earth.terrarium.cadmus.fabric;

import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.camps.CampManager;
import earth.terrarium.cadmus.common.commands.CadmusCommands;
import earth.terrarium.cadmus.common.protections.Protections;
import earth.terrarium.cadmus.common.protections.types.fabric.BlockBreakProtectionImpl;
import earth.terrarium.cadmus.common.protections.types.fabric.BlockInteractProtectionImpl;
import earth.terrarium.cadmus.common.protections.types.fabric.EntityDamageProtectionImpl;
import earth.terrarium.cadmus.common.protections.types.fabric.EntityInteractProtectionImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

public class CadmusFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Cadmus.init();
        ServerLifecycleEvents.SERVER_STARTED.register(Cadmus::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(CampManager::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> CadmusCommands.register(dispatcher, context));

        BlockBreakProtectionImpl.register();
        BlockInteractProtectionImpl.register();
        EntityInteractProtectionImpl.register();
        EntityDamageProtectionImpl.register();

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            return Protections.ITEM_USE.canUseItem(player, stack)
                ? InteractionResultHolder.pass(stack)
                : InteractionResultHolder.fail(stack);
        });
    }
}
