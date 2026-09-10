package earth.terrarium.cadmus.common.commands.settings;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.settings.types.ColorSetting;
import earth.terrarium.cadmus.api.settings.types.FloatSetting;
import earth.terrarium.cadmus.api.settings.types.StringSetting;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;

import java.util.List;
import java.util.Map;

public final class SettingCommandSupport {

    private static final SimpleCommandExceptionType INVALID_VALUE = new SimpleCommandExceptionType(Component.literal("Invalid setting value"));

    private SettingCommandSupport() {
    }

    public static SettingDefinition<?> find(Map<String, SettingDefinition<?>> definitions, String id) throws CommandSyntaxException {
        SettingDefinition<?> definition = definitions.get(id);
        if (definition == null) throw new SimpleCommandExceptionType(Component.literal("Unknown setting: " + id)).create();
        return definition;
    }

    public static SettingValue<?> parse(SettingDefinition<?> definition, String input) throws CommandSyntaxException {
        try {
            if (definition.defaultValue() instanceof BooleanSetting) {
                if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) throw INVALID_VALUE.create();
                return new BooleanSetting(Boolean.parseBoolean(input));
            }
            if (definition.defaultValue() instanceof FloatSetting) return new FloatSetting(Float.parseFloat(input));
            if (definition.defaultValue() instanceof ColorSetting) {
                Color color = Color.parse(input);
                if (color == null) throw INVALID_VALUE.create();
                return new ColorSetting(color);
            }
            if (definition.defaultValue() instanceof StringSetting) return new StringSetting(input);
        } catch (NumberFormatException exception) {
            throw INVALID_VALUE.create();
        }
        throw INVALID_VALUE.create();
    }

    public static String valueToString(SettingValue<?> value) {
        return String.valueOf(value.value());
    }

    public static SuggestionProvider<CommandSourceStack> valueSuggestions(SettingScope scope) {
        return (context, builder) -> {
            try {
                String id = StringArgumentType.getString(context, "setting");
                SettingDefinition<?> definition = SettingDefinitions.forScope(scope).get(id);
                if (definition != null && definition.defaultValue() instanceof BooleanSetting) {
                    return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
                }
            } catch (IllegalArgumentException ignored) {
            }
            return builder.buildFuture();
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void set(net.minecraft.server.MinecraftServer server, TeamId id, SettingDefinition<?> definition, SettingValue<?> value) {
        CadmusSaveData.setSettingValue(server, id, (SettingDefinition) definition, (SettingValue) value);
    }

    public static void checkTeamPermission(ServerPlayer player, TeamId id) throws CommandSyntaxException {
        if (!player.hasPermissions(2) && !TeamApi.API.canModifySettings(player, id)) {
            throw new SimpleCommandExceptionType(ConstantComponents.NO_PERMISSION_TEAM).create();
        }
    }

    public static void send(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable(key, args), false);
    }
}
