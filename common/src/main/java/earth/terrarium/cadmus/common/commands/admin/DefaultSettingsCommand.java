package earth.terrarium.cadmus.common.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public class DefaultSettingsCommand {

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(SettingDefinitions.forScope(SettingScope.TOWN).keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("defaultsettings")
                .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                    .then(Commands.literal("set")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .suggests(SettingCommandSupport.valueSuggestions(SettingScope.TOWN))
                            .executes(context -> {
                                set(context.getSource(), StringArgumentType.getString(context, "setting"), StringArgumentType.getString(context, "value"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(context -> {
                            reset(context.getSource(), StringArgumentType.getString(context, "setting"));
                            return 1;
                        })
                    )
                    .executes(context -> {
                        get(context.getSource(), StringArgumentType.getString(context, "setting"));
                        return 1;
                    })
                )
            )
        );
    }

    private static void set(CommandSourceStack source, String id, String input) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), id);
        setDefault(source.getServer(), definition, SettingCommandSupport.parse(definition, input));
        SettingCommandSupport.send(source, "command.cadmus.setting.set", id, input);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setDefault(net.minecraft.server.MinecraftServer server, earth.terrarium.cadmus.api.settings.SettingDefinition<?> definition, earth.terrarium.cadmus.api.settings.SettingValue<?> value) {
        CadmusSaveData.setDefaultSettingValue(server, (earth.terrarium.cadmus.api.settings.SettingDefinition) definition, (earth.terrarium.cadmus.api.settings.SettingValue) value);
    }

    private static void reset(CommandSourceStack source, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), id);
        CadmusSaveData.resetDefaultSettingValue(source.getServer(), definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.reset", id);
    }

    private static void get(CommandSourceStack source, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), id);
        var value = CadmusSaveData.getDefaultSettingValue(source.getServer(), definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.get", id, SettingCommandSupport.valueToString(value));
    }
}
