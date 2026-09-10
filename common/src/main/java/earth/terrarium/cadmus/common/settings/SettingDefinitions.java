package earth.terrarium.cadmus.common.settings;

import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
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

    public static final SettingDefinition<Boolean> BLOCK_BREAK = townBoolean("block-break", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> BLOCK_PLACE = townBoolean("block-place", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> BLOCK_INTERACTIONS = townBoolean("block-interactions", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> BLOCK_EXPLOSIONS = townBoolean("block-explosions", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Boolean> ENTITY_EXPLOSIONS = townBoolean("entity-explosions", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Boolean> ENTITY_INTERACTIONS = townBoolean("entity-interactions", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> ENTITY_DAMAGE = townBoolean("entity-damage", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> MOB_GRIEFING = townBoolean("mob-griefing", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Boolean> ITEM_PICKUP = townBoolean("item-pickup", SettingTarget.PLAYER, false);
    public static final SettingDefinition<Boolean> NON_PLAYERS_PLACE = townBoolean("non-players-place", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Boolean> FIRE_SPREAD = townBoolean("fire-spread", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> PVP = townBoolean("pvp", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ALLOW_ENTRY = townBoolean("allow-entry", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ALLOW_EXIT = townBoolean("allow-exit", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> USE = townBoolean("use", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> USE_CHESTS = townBoolean("use-chests", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> USE_DOORS = townBoolean("use-doors", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> USE_REDSTONE = townBoolean("use-redstone", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> USE_VEHICLES = townBoolean("use-vehicles", SettingTarget.PLAYER);

    public static final SettingDefinition<Boolean> ADMIN_BLOCK_BREAK = adminBoolean("block-break", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_BLOCK_PLACE = adminBoolean("block-place", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_BLOCK_INTERACTIONS = adminBoolean("block-interactions", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_BLOCK_EXPLOSIONS = adminBoolean("block-explosions", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_ENTITY_EXPLOSIONS = adminBoolean("entity-explosions", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_ENTITY_INTERACTIONS = adminBoolean("entity-interactions", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_ENTITY_DAMAGE = adminBoolean("entity-damage", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_MOB_GRIEFING = adminBoolean("mob-griefing", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_ITEM_PICKUP = adminBoolean("item-pickup", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_NON_PLAYERS_PLACE = adminBoolean("non-players-place", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Boolean> ADMIN_PVP = adminBoolean("pvp", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_MONSTER_DAMAGE = adminBoolean("monster-damage", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_CREATURE_DAMAGE = adminBoolean("creature-damage", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_FIRE_SPREAD = adminBoolean("fire-spread", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_ALLOW_ENTRY = adminBoolean("allow-entry", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_ALLOW_EXIT = adminBoolean("allow-exit", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ADMIN_USE = adminBoolean("use", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_USE_CHESTS = adminBoolean("use-chests", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_USE_DOORS = adminBoolean("use-doors", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_USE_REDSTONE = adminBoolean("use-redstone", SettingTarget.PLAYER);
    public static final SettingDefinition<Boolean> ADMIN_USE_VEHICLES = adminBoolean("use-vehicles", SettingTarget.PLAYER);

    public static final SettingDefinition<Boolean> SNOW_FALL = adminBoolean("snow-fall", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> SNOW_MELT = adminBoolean("snow-melt", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ICE_FORM = adminBoolean("ice-form", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> ICE_MELT = adminBoolean("ice-melt", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> LEAF_DECAY = adminBoolean("leaf-decay", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> LIGHTNING = adminBoolean("lightning", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> MONSTER_SPAWNING = adminBoolean("monster-spawning", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> CREATURE_SPAWNING = adminBoolean("creature-spawning", SettingTarget.GLOBAL);
    public static final SettingDefinition<Boolean> KEEP_INVENTORY = adminBoolean("keep-inventory", SettingTarget.GLOBAL, false);
    public static final SettingDefinition<Float> HEAL_RATE = adminFloat("heal-rate", SettingTarget.GLOBAL);
    public static final SettingDefinition<Float> FEED_RATE = adminFloat("feed-rate", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> ENTRY_DENY_MESSAGE = adminString("entry-deny-message", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> EXIT_DENY_MESSAGE = adminString("exit-deny-message", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> FAREWELL = adminString("farewell", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> GREETING = adminString("greeting", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> DISPLAY_NAME = adminString("display-name", SettingTarget.GLOBAL);
    public static final SettingDefinition<String> MOTD = adminString("motd", SettingTarget.GLOBAL);
    public static final SettingDefinition<Color> COLOR = register(new SettingDefinition<>(
        "color", SettingScope.ADMIN_CLAIM, SettingTarget.GLOBAL, SettingAccess.ADMIN, new ColorSetting(Color.DEFAULT)
    ));

    private SettingDefinitions() {
    }

    public static Map<SettingScope, Map<String, SettingDefinition<?>>> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    public static Map<String, SettingDefinition<?>> forScope(SettingScope scope) {
        return Collections.unmodifiableMap(DEFINITIONS.getOrDefault(scope, Map.of()));
    }

    private static SettingDefinition<Boolean> townBoolean(String id, SettingTarget target) {
        return townBoolean(id, target, true);
    }

    private static SettingDefinition<Boolean> townBoolean(String id, SettingTarget target, boolean value) {
        return register(new SettingDefinition<>(id, SettingScope.TOWN, target, SettingAccess.PLAYER, new BooleanSetting(value)));
    }

    private static SettingDefinition<Boolean> adminBoolean(String id, SettingTarget target) {
        return adminBoolean(id, target, true);
    }

    private static SettingDefinition<Boolean> adminBoolean(String id, SettingTarget target, boolean value) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, target, SettingAccess.ADMIN, new BooleanSetting(value)));
    }

    private static SettingDefinition<Float> adminFloat(String id, SettingTarget target) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, target, SettingAccess.ADMIN, new FloatSetting(0.0f)));
    }

    private static SettingDefinition<String> adminString(String id, SettingTarget target) {
        return register(new SettingDefinition<>(id, SettingScope.ADMIN_CLAIM, target, SettingAccess.ADMIN, new StringSetting("")));
    }

    private static <T> SettingDefinition<T> register(SettingDefinition<T> definition) {
        DEFINITIONS.computeIfAbsent(definition.scope(), ignored -> new LinkedHashMap<>())
            .put(definition.id(), definition);
        return definition;
    }
}
