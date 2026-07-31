package earth.terrarium.cadmus.common.config.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class CadmusConfigImpl {

    public static Path configFolder() {
        return FMLPaths.CONFIGDIR.get();
    }
}
