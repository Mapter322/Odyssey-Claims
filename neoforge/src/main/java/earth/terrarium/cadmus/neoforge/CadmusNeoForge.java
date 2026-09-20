package earth.terrarium.cadmus.neoforge;

import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.client.neoforge.CadmusClientNeoForge;
import earth.terrarium.cadmus.common.camps.CampManager;
import earth.terrarium.cadmus.common.commands.CadmusCommands;
import earth.terrarium.cadmus.common.compat.cbc.CBCProtectionEvents;
import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Cadmus.MOD_ID)
public class CadmusNeoForge {
    public CadmusNeoForge() {
        Cadmus.init();
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onEnterSection);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onRightClick);
        if (ModList.get().isLoaded("createbigcannons")) {
            CBCProtectionEvents.register();
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Cadmus.onPlayerJoin(player);
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        Cadmus.onServerStarted(event.getServer());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        CampManager.tick(event.getServer());
    }

    private void onEnterSection(EntityEvent.EnteringSection event) {
        if (event.getEntity() instanceof Player player) {
            Cadmus.onEnterSection(player, event.getOldPos().chunk());
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CadmusCommands.register(event.getDispatcher());
    }

    private void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!Protections.ITEM_USE.canUseItem(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }
}
