package earth.terrarium.cadmus.common.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class AdminSettingsCommand {

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).keySet(), builder);

    private AdminSettingsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("adminclaims")
                .then(Commands.literal("settings")
                    .then(Commands.literal("list").executes(context -> {
                        list(context.getSource());
                        return 1;
                    }))
                    .then(Commands.literal("set")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(SettingCommandSupport.valueSuggestions(SettingScope.ADMIN_CLAIM))
                                .executes(context -> {
                                    set(context.getSource(), StringArgumentType.getString(context, "setting"), StringArgumentType.getString(context, "value"));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("reset")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                            .executes(context -> {
                                reset(context.getSource(), StringArgumentType.getString(context, "setting"));
                                return 1;
                            })
                        )
                    )
                )
            )
        );
    }

    private static TeamId adminTeam() {
        return TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
    }

    private static void set(CommandSourceStack source, String id, String input) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM), id);
        SettingCommandSupport.set(source.getServer(), adminTeam(), definition, SettingCommandSupport.parse(definition, input));
        SettingCommandSupport.send(source, "command.cadmus.setting.set", id, input);
    }

    private static void reset(CommandSourceStack source, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM), id);
        CadmusSaveData.resetSettingValue(source.getServer(), adminTeam(), definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.reset", id);
    }

    private static void list(CommandSourceStack source) {
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) ->
            SettingCommandSupport.send(source, "command.cadmus.setting.get", id,
                SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(source.getServer(), adminTeam(), definition)))
        );
    }
}
