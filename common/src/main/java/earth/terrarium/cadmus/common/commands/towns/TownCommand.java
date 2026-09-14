package earth.terrarium.cadmus.common.commands.towns;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.api.settings.ClaimSettingsTarget;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.packets.serverbound.RequestClaimSettingsPacket;
import earth.terrarium.cadmus.common.towns.Town;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class TownCommand {

    private static final SuggestionProvider<CommandSourceStack> TOWN_NAME_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        TeamId team = playerTeam(player);
        if (team == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(TownManager.getTowns(player.server, team).stream().map(Town::name).toList(), builder);
    };

    private TownCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var create = Commands.literal("create")
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(context -> create(context.getSource(), StringArgumentType.getString(context, "name"))));

        var claim = Commands.literal("claim")
            .then(Commands.literal("town")
                .then(Commands.argument("name", StringArgumentType.string()).suggests(TOWN_NAME_SUGGESTIONS)
                    .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "name")))));

        var settings = Commands.argument("name", StringArgumentType.string()).suggests(TOWN_NAME_SUGGESTIONS)
            .then(Commands.literal("settings")
                .executes(context -> openSettings(context.getSource(), StringArgumentType.getString(context, "name"))));

        dispatcher.register(Commands.literal("cadmus").then(claim));
        dispatcher.register(Commands.literal("cadmus").then(Commands.literal("town").then(create).then(settings)));
    }

    private static int create(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component error = TownManager.create(player, player.chunkPosition(), player.chunkPosition(), name);
        if (error != null) throw new SimpleCommandExceptionType(error).create();
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.town_created", name), false);
        return 1;
    }

    private static int add(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Town town = townOrThrow(player, name);
        Component error = TownManager.add(player, town.id(), player.chunkPosition(), player.chunkPosition());
        if (error != null) throw new SimpleCommandExceptionType(error).create();
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.town_expanded", town.name()), false);
        return 1;
    }

    private static int openSettings(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Town town = townOrThrow(player, name);
        SettingCommandSupport.checkTeamPermission(player, town.team());
        RequestClaimSettingsPacket.send(player, new ClaimSettingsTarget(town.team(), town.id()));
        return 1;
    }

    private static Town townOrThrow(ServerPlayer player, String name) throws CommandSyntaxException {
        TeamId team = playerTeam(player);
        if (team == null) throw new SimpleCommandExceptionType(ConstantComponents.TEAM_DOES_NOT_EXIST).create();
        Town town = TownManager.getTownByName(player.server, team, name);
        if (town == null) throw new SimpleCommandExceptionType(Component.translatable(TownManager.ERR_TOWN_NOT_FOUND)).create();
        return town;
    }

    @Nullable
    private static TeamId playerTeam(ServerPlayer player) {
        return TeamApi.API.getTeamsList(player).stream().findFirst().orElse(null);
    }
}
