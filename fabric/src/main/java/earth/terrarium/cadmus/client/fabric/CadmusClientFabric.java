package earth.terrarium.cadmus.client.fabric;

import earth.terrarium.cadmus.client.CadmusClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public class CadmusClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CadmusClient.init();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CadmusClient.onPlayerLoggedOut());
        registerClientCommands();
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("claimmap")
                .executes(context -> {
                    Minecraft.getInstance().tell(CadmusClient::openClaimMap);
                    return 0;
                })
            ));
    }
}
