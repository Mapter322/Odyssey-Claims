package earth.terrarium.cadmus.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AdminClaimDefaultsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cadmus.MOD_ID);
    private static final Map<String, Boolean> VALUES = new LinkedHashMap<>();
    private static boolean loaded;

    private AdminClaimDefaultsConfig() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        VALUES.putAll(load());
    }

    @Nullable
    public static Boolean get(String id) {
        ensureLoaded();
        return VALUES.get(id);
    }

    private static Path configPath() {
        return CadmusConfig.defaultFolder().resolve("adminclaims.toml");
    }

    private static Map<String, Boolean> load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                return parse(Files.readString(path));
            } catch (Exception e) {
                LOGGER.warn("Failed to read admin claim defaults from {} ({}), using built-in defaults", path, e.toString());
                return generate();
            }
        }

        Map<String, Boolean> defaults = generate();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, write(defaults));
        } catch (Exception e) {
            LOGGER.warn("Failed to write admin claim defaults to {} ({})", path, e.toString());
        }
        return defaults;
    }

    private static Map<String, Boolean> generate() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) -> {
            if (definition.defaultValue() instanceof BooleanSetting setting) {
                values.put(id, setting.value());
            }
        });
        return values;
    }

    private static Map<String, Boolean> parse(String text) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        CommentedConfig root = new TomlParser().parse(new StringReader(text));
        root.valueMap().forEach((key, value) -> {
            if (value instanceof Config table) {
                table.valueMap().forEach((childKey, childValue) -> {
                    String name = String.valueOf(childKey);
                    if (name.equals("value")) {
                        Boolean state = parseState(key, childValue);
                        if (state != null) values.put(key, state);
                    } else {
                        Boolean state = parseState(key + "/" + name, childValue);
                        if (state != null) values.put(key + "/" + name, state);
                    }
                });
            } else {
                Boolean state = parseState(key, value);
                if (state != null) values.put(key, state);
            }
        });
        return values;
    }

    @Nullable
    private static Boolean parseState(String key, Object element) {
        if (element instanceof Boolean bool) return bool;
        if (!(element instanceof String string)) {
            LOGGER.warn("Invalid value for admin claim default '{}', skipping", key);
            return null;
        }
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> true;
            case "deny", "false" -> false;
            case "inherit", "undefined" -> null;
            default -> {
                LOGGER.warn("Unknown admin claim default value '{}' for '{}', skipping", string, key);
                yield null;
            }
        };
    }

    private static String write(Map<String, Boolean> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Cadmus admin claim defaults.\n");
        sb.append("# Values: allow, deny, inherit; inherit falls back to the parent setting.\n");
        sb.append("# Applied to admin claim settings in place of the built-in defaults.\n");

        Map<String, Entry> groups = group(values);
        for (Map.Entry<String, Entry> group : groups.entrySet()) {
            if (!group.getValue().children.isEmpty()) continue;
            sb.append(quote(group.getKey())).append(" = \"").append(stateName(group.getValue().value)).append("\"\n");
        }
        for (Map.Entry<String, Entry> group : groups.entrySet()) {
            Entry entry = group.getValue();
            if (entry.children.isEmpty()) continue;
            sb.append("\n[").append(quote(group.getKey())).append("]\n");
            if (entry.value != null) {
                sb.append("value = \"").append(stateName(entry.value)).append("\"\n");
            }
            entry.children.forEach((key, value) ->
                sb.append(quote(key)).append(" = \"").append(stateName(value)).append("\"\n"));
        }
        return sb.toString();
    }

    private static Map<String, Entry> group(Map<String, Boolean> values) {
        Map<String, Entry> groups = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            String key = entry.getKey();
            int index = key.indexOf('/');
            if (index > 0) {
                groups.computeIfAbsent(key.substring(0, index), ignored -> new Entry())
                    .children.put(key.substring(index + 1), entry.getValue());
            } else {
                groups.computeIfAbsent(key, ignored -> new Entry()).value = entry.getValue();
            }
        }
        return groups;
    }

    private static String stateName(@Nullable Boolean value) {
        return value == null || value ? "allow" : "deny";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class Entry {
        private Boolean value;
        private final Map<String, Boolean> children = new LinkedHashMap<>();
    }
}
