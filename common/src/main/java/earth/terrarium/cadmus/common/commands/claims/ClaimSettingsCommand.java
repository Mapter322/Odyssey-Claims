package earth.terrarium.cadmus.common.commands.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ClaimSettingsCommand {

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(SettingDefinitions.forScope(SettingScope.TOWN).keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("claim")
                .then(Commands.literal("settings")
                    .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                        .then(Commands.literal("set")
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(SettingCommandSupport.valueSuggestions(SettingScope.TOWN))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    set(context.getSource(), player, currentTeam(context.getSource()),
                                        StringArgumentType.getString(context, "setting"),
                                        StringArgumentType.getString(context, "value"));
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("reset")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                reset(context.getSource(), player, currentTeam(context.getSource()),
                                    StringArgumentType.getString(context, "setting"));
                                return 1;
                            })
                        )
                        .then(Commands.argument("provider", ResourceLocationArgument.id()).suggests(TeamId.TEAM_PROVIDER_SUGGESTION_PROVIDER)
                            .then(Commands.argument("id", UuidArgument.uuid()).suggests(TeamId.TEAM_UUID_SUGGESTION_PROVIDER)
                                .then(Commands.literal("set")
                                    .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .suggests(SettingCommandSupport.valueSuggestions(SettingScope.TOWN))
                                        .executes(context -> {
                                            set(context.getSource(), context.getSource().getPlayerOrException(),
                                                TeamId.fromCommand(context), StringArgumentType.getString(context, "setting"),
                                                StringArgumentType.getString(context, "value"));
                                            return 1;
                                        })
                                    )
                                )
                                .then(Commands.literal("reset")
                                    .executes(context -> {
                                        reset(context.getSource(), context.getSource().getPlayerOrException(),
                                            TeamId.fromCommand(context), StringArgumentType.getString(context, "setting"));
                                        return 1;
                                    })
                                )
                                .executes(context -> {
                                    get(context.getSource(), TeamId.fromCommand(context), StringArgumentType.getString(context, "setting"));
                                    return 1;
                                })
                            )
                        )
                        .executes(context -> {
                            get(context.getSource(), currentTeam(context.getSource()), StringArgumentType.getString(context, "setting"));
                            return 1;
                        })
                    )
                )
            )
        );
    }

    private static void set(CommandSourceStack source, ServerPlayer player, TeamId id, String idString, String input) throws CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), idString);
        SettingCommandSupport.checkTeamPermission(player, id);
        SettingCommandSupport.set(source.getServer(), id, definition, SettingCommandSupport.parse(definition, input));
        SettingCommandSupport.send(source, "command.cadmus.setting.set", idString, input);
    }

    private static void reset(CommandSourceStack source, ServerPlayer player, TeamId id, String idString) throws CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), idString);
        SettingCommandSupport.checkTeamPermission(player, id);
        CadmusSaveData.resetSettingValue(source.getServer(), id, definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.reset", idString);
    }

    private static void get(CommandSourceStack source, TeamId id, String idString) throws CommandSyntaxException {
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.TOWN), idString);
        var value = CadmusSaveData.getSettingValue(source.getServer(), id, definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.get", idString, SettingCommandSupport.valueToString(value));
    }

    private static TeamId currentTeam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ChunkPos pos = player.chunkPosition();
        return ClaimApi.API.getClaim(source.getLevel(), pos)
            .map(claim -> claim.team())
            .orElseThrow(() -> new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(ConstantComponents.NOT_CLAIMED).create());
    }
}
