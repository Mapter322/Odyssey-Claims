package earth.terrarium.cadmus.common.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.api.flags.types.ColorFlag;
import earth.terrarium.cadmus.api.flags.types.StringFlag;
import earth.terrarium.cadmus.api.protections.ProtectionApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommand;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.flags.Flags;
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
        var settings = new java.util.HashMap<String, com.teamresourceful.resourcefullib.common.utils.TriState>();
        ProtectionApi.API.getSettings().forEach(setting -> settings.put(setting, CadmusSaveData.getClaimSetting(player.getServer(), id, setting)));
        NetworkHandler.CHANNEL.sendToPlayer(new OpenAdminClaimSettingsPacket(
            id,
            FlagApi.API.<String>getFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.DISPLAY_NAME.id()).value(),
            FlagApi.API.<Color>getFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.COLOR.id()).value(),
            FlagApi.API.<String>getFlag(player.getServer(), AdminTeamProvider.ADMIN_ID, Flags.MOTD.id()).value(),
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
