package earth.terrarium.cadmus.client.fabric;

import earth.terrarium.cadmus.client.CadmusClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public class CadmusClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CadmusClient.init();
        ClientTickEvents.START_CLIENT_TICK.register(client -> CadmusClient.onClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CadmusClient.onPlayerLoggedOut());
        registerClientCommands();
        KeyBindingHelper.registerKeyBinding(CadmusClient.KEY_OPEN_CLAIM_MAP);
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("cadmus")
                .then(ClientCommandManager.literal("claimmap")
                    .executes(context -> {
                        Minecraft.getInstance().tell(CadmusClient::openClaimMap);
                        return 0;
                    })
                )
            ));
    }
}
