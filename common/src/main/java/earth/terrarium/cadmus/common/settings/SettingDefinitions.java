package earth.terrarium.cadmus.common.settings;

import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingCategory;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.settings.types.ColorSetting;
import earth.terrarium.cadmus.api.settings.types.FloatSetting;
import earth.terrarium.cadmus.api.settings.types.StringSetting;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingDefinitions {

    private static final Map<SettingScope, Map<String, SettingDefinition<?>>> DEFINITIONS = new EnumMap<>(SettingScope.class);

    public static final SettingDefinition<Boolean> BLOCK_BREAK = townBoolean("block-break", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> BLOCK_PLACE = townBoolean("block-place", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> BLOCK_INTERACTIONS = townBoolean("block-interactions", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> BLOCK_EXPLOSIONS = townBoolean("block-explosions", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> ENTITY_EXPLOSIONS = townBoolean("entity-explosions", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> ENTITY_INTERACTIONS = townBoolean("entity-interactions", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> ENTITY_DAMAGE = townBoolean("entity-damage", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> MOB_GRIEFING = townBoolean("mob-griefing", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> ITEM_PICKUP = townBoolean("item-pickup", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> FIRE_SPREAD = townBoolean("fire-spread", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> PVP = townBoolean("pvp", SettingCategory.PLAYERS);
    public static final SettingDefinition<Boolean> MONSTER_DAMAGE = townBoolean("monster-damage", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> CREATURE_DAMAGE = townBoolean("creature-damage", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> ALLOW_ENTRY = townBoolean("allow-entry", SettingCategory.ACCESS);
    public static final SettingDefinition<Boolean> ALLOW_EXIT = townBoolean("allow-exit", SettingCategory.ACCESS);
    public static final SettingDefinition<Boolean> USE = townBoolean("use", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> USE_CHESTS = townBoolean("use-chests", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> USE_DOORS = townBoolean("use-doors", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> USE_REDSTONE = townBoolean("use-redstone", SettingCategory.BLOCKS);
    public static final SettingDefinition<Boolean> USE_VEHICLES = townBoolean("use-vehicles", SettingCategory.BLOCKS);

    public static final SettingDefinition<Boolean> SNOW_FALL = adminBoolean("snow-fall", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> SNOW_MELT = adminBoolean("snow-melt", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> ICE_FORM = adminBoolean("ice-form", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> ICE_MELT = adminBoolean("ice-melt", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> LEAF_DECAY = adminBoolean("leaf-decay", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> LIGHTNING = adminBoolean("lightning", SettingCategory.NATURE);
    public static final SettingDefinition<Boolean> MONSTER_SPAWNING = adminBoolean("monster-spawning", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> CREATURE_SPAWNING = adminBoolean("creature-spawning", SettingCategory.ENTITIES);
    public static final SettingDefinition<Boolean> KEEP_INVENTORY = adminBoolean("keep-inventory", SettingCategory.PLAYERS, false);
    public static final SettingDefinition<Float> HEAL_RATE = adminFloat("heal-rate", SettingCategory.PLAYERS);
    public static final SettingDefinition<Float> FEED_RATE = adminFloat("feed-rate", SettingCategory.PLAYERS);
    public static final SettingDefinition<String> ENTRY_DENY_MESSAGE = adminString("entry-deny-message", SettingCategory.MESSAGES);
    public static final SettingDefinition<String> EXIT_DENY_MESSAGE = adminString("exit-deny-message", SettingCategory.MESSAGES);
    public static final SettingDefinition<String> FAREWELL = adminString("farewell", SettingCategory.MESSAGES);
    public static final SettingDefinition<String> GREETING = adminString("greeting", SettingCategory.MESSAGES);
    public static final SettingDefinition<String> DISPLAY_NAME = adminString("display-name", SettingCategory.IDENTITY);
    public static final SettingDefinition<String> MOTD = adminString("motd", SettingCategory.IDENTITY);
    public static final SettingDefinition<Color> COLOR = register(new SettingDefinition<>(
        "color", SettingScope.ADMIN_CLAIM, SettingCategory.IDENTITY, SettingAccess.ADMIN, new ColorSetting(Color.DEFAULT)
    ));

    private SettingDefinitions() {
    }

    public static Map<SettingScope, Map<String, SettingDefinition<?>>> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    public static Map<String, SettingDefinition<?>> forScope(SettingScope scope) {
        return Collections.unmodifiableMap(DEFINITIONS.getOrDefault(scope, Map.of()));
    }

    private static SettingDefinition<Boolean> townBoolean(String id, SettingCategory category) {
        return register(new SettingDefinition<>(id, SettingScope.TOWN, category, SettingAccess.PLAYER, new BooleanSetting(true)));
    }

    private static SettingDefinition<Boolean> adminBoolean(String id, SettingCategory category) {
        return adminBoolean(id, category, true);
    }

    private static SettingDefinition<Boolean> adminBoolean(String id, SettingCategory category, boolean value) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, category, SettingAccess.ADMIN, new BooleanSetting(value)));
    }

    private static SettingDefinition<Float> adminFloat(String id, SettingCategory category) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, category, SettingAccess.ADMIN, new FloatSetting(0.0f)));
    }

    private static SettingDefinition<String> adminString(String id, SettingCategory category) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, category, SettingAccess.ADMIN, new StringSetting("")));
    }

    private static <T> SettingDefinition<T> register(SettingDefinition<T> definition) {
        DEFINITIONS.computeIfAbsent(definition.scope(), ignored -> new LinkedHashMap<>())
            .put(definition.id(), definition);
        return definition;
    }
}
