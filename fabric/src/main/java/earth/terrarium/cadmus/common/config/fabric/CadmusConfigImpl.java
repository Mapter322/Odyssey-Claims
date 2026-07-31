package earth.terrarium.cadmus.common.config.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class CadmusConfigImpl {

    public static Path configFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
