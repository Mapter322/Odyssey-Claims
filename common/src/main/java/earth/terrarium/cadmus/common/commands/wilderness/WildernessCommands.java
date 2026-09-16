package earth.terrarium.cadmus.common.commands.wilderness;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenWildernessSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.teams.WildernessTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;

public class WildernessCommands {

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(SettingDefinitions.forScope(SettingScope.WILDERNESS).keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("wilderness")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("settings")
                    .executes(context -> openSettings(context.getSource()))
                    .then(Commands.literal("list")
                        .executes(context -> {
                            list(context.getSource());
                            return 1;
                        }))
                    .then(Commands.literal("set")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(SettingCommandSupport.valueSuggestions(SettingScope.WILDERNESS))
                                .executes(context -> {
                                    set(context.getSource(), StringArgumentType.getString(context, "setting"), StringArgumentType.getString(context, "value"));
                                    return 1;
                                }))))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                            .executes(context -> {
                                reset(context.getSource(), StringArgumentType.getString(context, "setting"));
                                return 1;
                            }))))));
    }

    private static int openSettings(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        sendSettings(source.getPlayerOrException());
        return 1;
    }

    private static void set(CommandSourceStack source, String id, String input) throws CommandSyntaxException {
        requireAdmin(source);
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.WILDERNESS), id);
        SettingCommandSupport.set(source.getServer(), wildernessTeam(), definition, SettingCommandSupport.parse(definition, input));
        SettingCommandSupport.send(source, "command.cadmus.setting.set", id, input);
    }

    private static void reset(CommandSourceStack source, String id) throws CommandSyntaxException {
        requireAdmin(source);
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.WILDERNESS), id);
        CadmusSaveData.resetSettingValue(source.getServer(), wildernessTeam(), definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.reset", id);
    }

    private static void list(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        SettingDefinitions.forScope(SettingScope.WILDERNESS).forEach((id, definition) ->
            SettingCommandSupport.send(source, "command.cadmus.setting.get", id,
                String.valueOf(Settings.getForTeam(source.getServer(), wildernessTeam(), definition)))
        );
    }

    public static void sendSettings(ServerPlayer player) {
        TeamId id = wildernessTeam();
        var settings = new HashMap<String, String>();
        SettingDefinitions.forScope(SettingScope.WILDERNESS).forEach((setting, definition) ->
            settings.put(setting, String.valueOf(Settings.getForTeam(player.getServer(), id, definition))));
        NetworkHandler.CHANNEL.sendToPlayer(new OpenWildernessSettingsPacket(
            settings,
            CadmusSaveData.getConditions(player.getServer(), id)
        ), player);
    }

    private static TeamId wildernessTeam() {
        return WildernessTeamProvider.team();
    }

    private static void requireAdmin(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.hasPermission(2)) throw new SimpleCommandExceptionType(ConstantComponents.NO_PERMISSION_ROLE).create();
    }
}
