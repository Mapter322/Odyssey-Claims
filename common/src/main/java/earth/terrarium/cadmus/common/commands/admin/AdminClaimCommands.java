package earth.terrarium.cadmus.common.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommand;
import earth.terrarium.cadmus.common.commands.claims.ForceloadCommand;
import earth.terrarium.cadmus.common.commands.settings.SettingCommandSupport;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.OpenAdminClaimSettingsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.teams.AdminTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;

public class AdminClaimCommands {

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).keySet(), builder);

    private static final SimpleCommandExceptionType NOT_ADMIN_CLAIM = new SimpleCommandExceptionType(ConstantComponents.NOT_ADMIN_CLAIM);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("adminclaim")
                .requires(source -> source.hasPermission(2))
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
                .then(Commands.literal("forceload")
                    .then(Commands.argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            forceload(context.getSource(), context.getSource().getPlayerOrException().chunkPosition(), BoolArgumentType.getBool(context, "state"));
                            return 1;
                        }))
                    .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(context -> {
                            ChunkPos pos = ColumnPosArgument.getColumnPos(context, "pos").toChunkPos();
                            forceload(context.getSource(), pos, !ForceloadCommand.isChunkLoaded(context.getSource(), pos));
                            return 1;
                        })
                        .then(Commands.argument("state", BoolArgumentType.bool())
                            .executes(context -> {
                                ChunkPos pos = ColumnPosArgument.getColumnPos(context, "pos").toChunkPos();
                                forceload(context.getSource(), pos, BoolArgumentType.getBool(context, "state"));
                                return 1;
                            })))
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ChunkPos pos = source.getPlayerOrException().chunkPosition();
                        forceload(source, pos, !ForceloadCommand.isChunkLoaded(source, pos));
                        return 1;
                    }))
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
                                .suggests(SettingCommandSupport.valueSuggestions(SettingScope.ADMIN_CLAIM))
                                .executes(context -> {
                                    set(context.getSource(), StringArgumentType.getString(context, "setting"), StringArgumentType.getString(context, "value"));
                                    return 1;
                                }))))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTING_SUGGESTIONS)
                            .executes(context -> {
                                reset(context.getSource(), StringArgumentType.getString(context, "setting"));
                                return 1;
                            }))))
                .then(Commands.literal("adminmode")
                    .executes(context -> toggleAdminMode(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> toggleAdminMode(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            ));
    }

    private static void claimFixed(CommandSourceStack source, ChunkPos pos) throws CommandSyntaxException {
        requireAdmin(source);
        if (!CadmusConfig.get().isClaimingAllowed(source.getLevel())) {
            throw new SimpleCommandExceptionType(Component.translatable("command.cadmus.exception.dimension_blocked")).create();
        }
        AdminTeamProvider.ensureAdminTeam(source.getServer());
        ClaimCommand.checkClaimed(source.getLevel(), pos);
        ClaimApi.API.claim(source.getLevel(), TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID), pos, false);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.claimed_admin_chunk_at", pos.x, pos.z), false);
    }

    private static void forceload(CommandSourceStack source, ChunkPos pos, boolean state) throws CommandSyntaxException {
        requireAdmin(source);
        var claim = ClaimApi.API.getClaim(source.getLevel(), pos);
        if (claim.isEmpty()) throw ForceloadCommand.NOT_CLAIMED.create();
        if (!claim.get().team().isAdmin()) throw NOT_ADMIN_CLAIM.create();
        ForceloadCommand.set(source, pos, state, adminTeam());
    }

    private static int openSettings(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        sendSettings(source.getPlayerOrException());
        return 1;
    }

    private static void set(CommandSourceStack source, String id, String input) throws CommandSyntaxException {
        requireAdmin(source);
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM), id);
        SettingCommandSupport.set(source.getServer(), adminTeam(), definition, SettingCommandSupport.parse(definition, input));
        SettingCommandSupport.send(source, "command.cadmus.setting.set", id, input);
    }

    private static void reset(CommandSourceStack source, String id) throws CommandSyntaxException {
        requireAdmin(source);
        var definition = SettingCommandSupport.find(SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM), id);
        CadmusSaveData.resetSettingValue(source.getServer(), adminTeam(), definition);
        SettingCommandSupport.send(source, "command.cadmus.setting.reset", id);
    }

    private static void list(CommandSourceStack source) throws CommandSyntaxException {
        requireAdmin(source);
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((id, definition) ->
            SettingCommandSupport.send(source, "command.cadmus.setting.get", id,
                String.valueOf(Settings.getForTeam(source.getServer(), adminTeam(), definition)))
        );
    }

    public static void sendSettings(ServerPlayer player) {
        AdminTeamProvider.ensureAdminTeam(player.getServer());
        TeamId id = TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
        var settings = new HashMap<String, String>();
        SettingDefinitions.forScope(SettingScope.ADMIN_CLAIM).forEach((setting, definition) ->
            settings.put(setting, String.valueOf(Settings.getForTeam(player.getServer(), id, definition))));
        NetworkHandler.CHANNEL.sendToPlayer(new OpenAdminClaimSettingsPacket(
            id,
            SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.DISPLAY_NAME)),
            CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.COLOR).value(),
            SettingCommandSupport.valueToString(CadmusSaveData.getSettingValue(player.getServer(), id, SettingDefinitions.MOTD)),
            settings,
            CadmusSaveData.getConditions(player.getServer(), id)
        ), player);
    }

    private static TeamId adminTeam() {
        return TeamId.ofAdmin(AdminTeamProvider.ADMIN_ID);
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
