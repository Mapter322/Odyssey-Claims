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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CadmusConfig {

    private static CadmusConfig INSTANCE;

    public int minChunksBetweenTowns = 10;
    public int maxTownSize = 0;
    public int maxOutpostSize = 4;
    public int personalCampDurationSeconds = 86400;
    public List<String> blockedClaimDimensions = new ArrayList<>();
    public List<String> publicBlockInteractions = new ArrayList<>(List.of("minecraft:crafting_table"));
    public List<String> publicEntityInteractions = new ArrayList<>(List.of("minecraft:boat", "minecraft:chest_boat"));
    public Map<String, Boolean> defaultClaimSettings = createDefaultClaimSettings();

    public boolean isClaimingAllowed(Level level) {
        return !blockedClaimDimensions.contains(level.dimension().location().toString());
    }

    public boolean isPublicBlockInteraction(BlockState state) {
        for (String entry : publicBlockInteractions) {
            if (entry.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                if (id != null && state.is(TagKey.create(Registries.BLOCK, id))) return true;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null && id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
            }
        }
        return false;
    }

    public boolean isPublicEntityInteraction(EntityType<?> type) {
        for (String entry : publicEntityInteractions) {
            if (entry.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                if (id != null && type.is(TagKey.create(Registries.ENTITY_TYPE, id))) return true;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
            }
        }
        return false;
    }

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
                config.maxTownSize = root.getIntOrElse("claims.max-town-size", config.maxTownSize);
                config.maxOutpostSize = root.getIntOrElse("claims.max-outpost-size", config.maxOutpostSize);
                config.personalCampDurationSeconds = root.getIntOrElse("camps.camp-duration-seconds", config.personalCampDurationSeconds);
                Object blocked = root.get("claims.blocked-dimensions");
                if (blocked instanceof List<?> list) {
                    List<String> dimensions = new ArrayList<>();
                    list.forEach(value -> {
                        if (value instanceof String string && ResourceLocation.tryParse(string) != null) {
                            dimensions.add(string);
                        }
                    });
                    config.blockedClaimDimensions = dimensions;
                }
                Object publicInteractions = root.get("claims.public-block-interactions");
                if (publicInteractions instanceof List<?> list) {
                    List<String> blocks = new ArrayList<>();
                    list.forEach(value -> {
                        if (value instanceof String string && isValidEntry(string)) {
                            blocks.add(string);
                        }
                    });
                    config.publicBlockInteractions = blocks;
                }
                Object publicEntityInteractions = root.get("claims.public-entity-interactions");
                if (publicEntityInteractions instanceof List<?> list) {
                    List<String> entities = new ArrayList<>();
                    list.forEach(value -> {
                        if (value instanceof String string && isValidEntry(string)) {
                            entities.add(string);
                        }
                    });
                    config.publicEntityInteractions = entities;
                }
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
        sb.append("# Minimum distance in chunks between towns, outposts and camps.\n");
        sb.append("min-chunks-between-towns = ").append(minChunksBetweenTowns).append("\n\n");
        sb.append("# Maximum number of chunks a single town can contain (0 = unlimited).\n");
        sb.append("max-town-size = ").append(maxTownSize).append("\n\n");
        sb.append("# Maximum number of chunks a single outpost can contain.\n");
        sb.append("max-outpost-size = ").append(maxOutpostSize).append("\n\n");
        sb.append("# Dimensions where claiming chunks is disabled; claiming is allowed everywhere else.\n");
        sb.append("# Example: blocked-dimensions = [\"minecraft:the_end\"]\n");
        sb.append("blocked-dimensions = [");
        for (int i = 0; i < blockedClaimDimensions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(quote(blockedClaimDimensions.get(i)));
        }
        sb.append("]\n\n");
        sb.append("# Blocks that everyone can interact with, ignoring claim permissions.\n");
        sb.append("# Accepts block ids (minecraft:lever) and tags (#minecraft:buttons).\n");
        sb.append("public-block-interactions = [");
        for (int i = 0; i < publicBlockInteractions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(quote(publicBlockInteractions.get(i)));
        }
        sb.append("]\n\n");
        sb.append("# Entities that everyone can interact with, ignoring claim permissions.\n");
        sb.append("# Accepts entity ids (minecraft:horse) and tags (#minecraft:boats).\n");
        sb.append("public-entity-interactions = [");
        for (int i = 0; i < publicEntityInteractions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(quote(publicEntityInteractions.get(i)));
        }
        sb.append("]\n\n");

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

    private static boolean isValidEntry(String entry) {
        return entry.startsWith("#")
            ? ResourceLocation.tryParse(entry.substring(1)) != null
            : ResourceLocation.tryParse(entry) != null;
    }
}
