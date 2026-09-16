package earth.terrarium.cadmus.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class WildernessDefaultsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cadmus.MOD_ID);
    private static final Map<String, Boolean> VALUES = new LinkedHashMap<>();
    private static boolean loaded;

    private WildernessDefaultsConfig() {
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
        return CadmusConfig.configFolder().resolve(Cadmus.MOD_ID).resolve("wildernessdefaults.json");
    }

    private static Map<String, Boolean> load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                return parse(Files.readString(path));
            } catch (Exception e) {
                LOGGER.warn("Failed to read wilderness defaults from {} ({}), using built-in defaults", path, e.toString());
                return generate();
            }
        }

        Map<String, Boolean> defaults = generate();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, write(defaults));
        } catch (Exception e) {
            LOGGER.warn("Failed to write wilderness defaults to {} ({})", path, e.toString());
        }
        return defaults;
    }

    private static Map<String, Boolean> generate() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        SettingDefinitions.forScope(SettingScope.WILDERNESS).forEach((id, definition) -> {
            if (definition.defaultValue() instanceof BooleanSetting setting) {
                values.put(id, setting.value());
            }
        });
        return values;
    }

    private static Map<String, Boolean> parse(String text) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        JsonObject root = JsonParser.parseString(stripComments(text)).getAsJsonObject();
        JsonElement element = root.get("settings");
        if (element == null || !element.isJsonObject()) {
            LOGGER.warn("Wilderness defaults have no 'settings' object, using built-in defaults");
            return generate();
        }
        JsonObject settings = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
            String parent = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                if (object.has("value")) {
                    Boolean state = parseState(parent, object.get("value"));
                    if (state != null) values.put(parent, state);
                }
                for (Map.Entry<String, JsonElement> child : object.entrySet()) {
                    if (child.getKey().equals("value")) continue;
                    Boolean state = parseState(parent + "/" + child.getKey(), child.getValue());
                    if (state != null) values.put(parent + "/" + child.getKey(), state);
                }
            } else {
                Boolean state = parseState(parent, value);
                if (state != null) values.put(parent, state);
            }
        }
        return values;
    }

    @Nullable
    private static Boolean parseState(String key, JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            LOGGER.warn("Invalid value for wilderness default '{}', skipping", key);
            return null;
        }
        return switch (element.getAsString().toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> true;
            case "deny", "false" -> false;
            case "inherit", "undefined" -> null;
            default -> {
                LOGGER.warn("Unknown wilderness default value '{}' for '{}', skipping", element.getAsString(), key);
                yield null;
            }
        };
    }

    private static String write(Map<String, Boolean> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Cadmus wilderness defaults.\n");
        sb.append("// Values: allow, deny, inherit; inherit falls back to the parent setting.\n");
        sb.append("// Applied to wilderness settings in place of the built-in defaults.\n");
        sb.append("{\n");
        sb.append("  \"settings\": {");

        boolean first = true;
        for (Map.Entry<String, Entry> group : group(values).entrySet()) {
            sb.append(first ? "\n" : ",\n");
            first = false;
            Entry entry = group.getValue();
            if (entry.children.isEmpty()) {
                sb.append("    \"").append(group.getKey()).append("\": \"").append(stateName(entry.value)).append("\"");
                continue;
            }
            sb.append("    \"").append(group.getKey()).append("\": {\n");
            if (entry.value != null) {
                sb.append("      \"value\": \"").append(stateName(entry.value)).append("\",\n");
            }
            int index = 0;
            for (Map.Entry<String, Boolean> child : entry.children.entrySet()) {
                sb.append("      \"").append(child.getKey()).append("\": \"").append(stateName(child.getValue())).append("\"");
                sb.append(++index < entry.children.size() ? ",\n" : "\n");
            }
            sb.append("    }");
        }
        sb.append("\n  }\n}\n");
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

    private static String stripComments(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            int index = line.indexOf("//");
            sb.append(index < 0 ? line : line.substring(0, index)).append('\n');
        }
        return sb.toString();
    }

    private static final class Entry {
        private Boolean value;
        private final Map<String, Boolean> children = new LinkedHashMap<>();
    }
}
