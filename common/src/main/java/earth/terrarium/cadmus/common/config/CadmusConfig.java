package earth.terrarium.cadmus.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teamresourceful.resourcefullib.common.exceptions.NotImplementedException;
import dev.architectury.injectables.annotations.ExpectPlatform;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CadmusConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CadmusConfig INSTANCE;

    public int defaultMaxClaims = 1096;
    public int defaultMaxChunkLoadedClaims = 64;
    public int minChunksBetweenTowns = 10;
    public int personalCampDurationSeconds = 300;
    public Map<String, Boolean> defaultClaimSettings = createDefaultClaimSettings();

    private static Map<String, Boolean> createDefaultClaimSettings() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.GLOBAL) return;
            if (definition.defaultValue() instanceof BooleanSetting setting) {
                defaults.put(id, setting.value());
            }
        });
        return defaults;
    }

    public static CadmusConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    @ExpectPlatform
    static Path configFolder() {
        throw new NotImplementedException();
    }

    private static Path configPath() {
        return configFolder().resolve(Cadmus.MOD_ID).resolve("cadmus-common.json");
    }

    private static CadmusConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                CadmusConfig config = GSON.fromJson(reader, CadmusConfig.class);
                if (config != null) {
                    if (config.defaultClaimSettings == null || config.defaultClaimSettings.isEmpty()) {
                        config.defaultClaimSettings = createDefaultClaimSettings();
                    }
                    config.save();
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                // ignored
            }
        }
        CadmusConfig config = new CadmusConfig();
        config.save();
        return config;
    }

    private void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
