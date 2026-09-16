package earth.terrarium.cadmus.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.teamresourceful.resourcefullib.common.exceptions.NotImplementedException;
import dev.architectury.injectables.annotations.ExpectPlatform;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CadmusConfig {

    private static CadmusConfig INSTANCE;

    public int minChunksBetweenTowns = 10;
    public int personalCampDurationSeconds = 86400;
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

    public static Path odysseyFolder() {
        return configFolder().resolve("odyssey");
    }

    public static Path defaultFolder() {
        return odysseyFolder().resolve("default");
    }

    private static Path configPath() {
        return odysseyFolder().resolve("cadmus-server.toml");
    }

    private static CadmusConfig load() {
        CadmusConfig config = new CadmusConfig();
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                CommentedConfig root = new TomlParser().parse(reader);
                config.minChunksBetweenTowns = root.getIntOrElse("claims.min-chunks-between-towns", config.minChunksBetweenTowns);
                config.personalCampDurationSeconds = root.getIntOrElse("camps.camp-duration-seconds", config.personalCampDurationSeconds);
                Object settings = root.get("town-settings");
                if (settings instanceof Config table && !table.isEmpty()) {
                    Map<String, Boolean> defaults = new LinkedHashMap<>();
                    table.valueMap().forEach((id, value) -> {
                        if (value instanceof Boolean bool) defaults.put(id, bool);
                    });
                    if (!defaults.isEmpty()) config.defaultClaimSettings = defaults;
                }
            } catch (Exception ignored) {
            }
        }
        config.save();
        return config;
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Cadmus server configuration.\n");
        sb.append("# Generated at config/odyssey/cadmus-server.toml; edit values below and restart the server.\n\n");

        sb.append("[claims]\n");
        sb.append("# Minimum distance in chunks between towns and camps.\n");
        sb.append("min-chunks-between-towns = ").append(minChunksBetweenTowns).append("\n\n");

        sb.append("[camps]\n");
        sb.append("# Lifetime of a personal camp in seconds (86400 = 24 hours).\n");
        sb.append("camp-duration-seconds = ").append(personalCampDurationSeconds).append("\n\n");

        sb.append("[town-settings]\n");
        sb.append("# Default global settings of new towns. Values: allow, deny.\n");
        sb.append("# In-game: /cadmus town settings set <setting> <value>.\n");
        defaultClaimSettings.forEach((id, value) ->
            sb.append(quote(id)).append(" = ").append(value).append("\n"));

        try {
            Files.createDirectories(configPath().getParent());
            Files.writeString(configPath(), sb.toString());
        } catch (Exception ignored) {
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
