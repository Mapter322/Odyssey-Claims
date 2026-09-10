package earth.terrarium.cadmus.common.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommand;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.common.utils.ModUtils;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

public class AdminClaimCommands {

    private static final SimpleCommandExceptionType ADMIN_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(ConstantComponents.ADMIN_TEAM_ALREADY_EXISTS);
    public static final SimpleCommandExceptionType ADMIN_TEAM_DOES_NOT_EXIST = new SimpleCommandExceptionType(ConstantComponents.ADMIN_TEAM_DOES_NOT_EXIST);

    public static final SuggestionProvider<CommandSourceStack> ADMIN_TEAM_SUGGESTION_PROVIDER = (context, builder) -> {
        Collection<String> names = FlagApi.API.getAllAdminTeamNames(context.getSource().getServer());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("adminclaim")
                .then(Commands.literal("claim")
                    .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(context -> {
                            claimFixed(context.getSource(), ColumnPosArgument.getColumnPos(context, "pos").toChunkPos());
                            return 1;
                        }))
                    .executes(context -> {
                        claimFixed(context.getSource(), context.getSource().getPlayerOrException().chunkPosition());
                        return 1;
                    }))
                .then(Commands.literal("settings")
                    .executes(context -> openSettings(context.getSource())))
.then(Commands.literal("adminmode")
                    .executes(context -> toggleAdminMode(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> toggleAdminMode(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            )
        );
    }

    private static void claimFixed(CommandSourceStack source, ChunkPos pos) throws CommandSyntaxException {
        requireAdmin(source);
        AdminTeamProvider.ensureAdminTeam(source.getServer());
        ClaimCommand.checkClaimed(source.getLevel(), pos);
        ClaimApi.API.claim(source.getLevel(), TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID), pos, false);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.claimed_admin_chunk_at", pos.x, pos.z), false);
    }

    private static int openSettings(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        sendSettings(source.getPlayerOrException());
        return 1;
    }

public static void sendSettings(ServerPlayer player) {
        AdminTeamProvider.ensureAdminTeam(player.getServer());
        TeamId id = TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
        var settings = new java.util.HashMap<String, String>();
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((setting, definition) ->
            settings.put(setting, SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(player.getServer(), id, definition))));
        NetworkHandler.CHANNEL.sendToPlayer(new OpenAdminClaimSettingsPacket(
            id,
            SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.DISPLAY_NAME)),
            CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.COLOR).value(),
            SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.MOTD)),
            settings
        ), player);
    }

    private static void requireAdmin(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.hasPermission(2)) throw new SimpleCommandExceptionType(ConstantComponents.NO_PERMISSION_ROLE).create();
    }

private static int toggleAdminMode(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        return toggleAdminMode(source, source.getPlayerOrException());
    }

    private static int toggleAdminMode(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        requireAdmin(source);
        CadmusSaveData.toggleBypass(source.getServer(), player.getUUID());
        boolean enabled = CadmusSaveData.canBypass(source.getServer(), player.getUUID());
        source.sendSuccess(() -> ModUtils.translatableWithStyle(
            enabled ? "command.cadmus.adminmode.enable" : "command.cadmus.adminmode.disable",
            player.getGameProfile().getName()
        ), false);
        return 1;
    }

}
