package earth.terrarium.cadmus.common.commands.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.claims.ClaimData;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimitApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ForceloadCommand {

    public static final SimpleCommandExceptionType NOT_CLAIMED = new SimpleCommandExceptionType(ConstantComponents.NOT_CLAIMED);
    public static final SimpleCommandExceptionType NOT_OWNER = new SimpleCommandExceptionType(ConstantComponents.NOT_OWNER);
    public static final SimpleCommandExceptionType NO_CLAIM_PERMISSION = new SimpleCommandExceptionType(ConstantComponents.NO_CLAIM_PERMISSION);
    public static final SimpleCommandExceptionType FORCELOAD_LIMIT = new SimpleCommandExceptionType(ConstantComponents.FORCELOAD_LIMIT);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("forceload")
                .then(Commands.argument("state", BoolArgumentType.bool())
                    .executes(context -> {
                        setForPlayer(context.getSource(), context.getSource().getPlayerOrException().chunkPosition(), BoolArgumentType.getBool(context, "state"));
                        return 1;
                    })
                )
                .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                    .executes(context -> {
                        ChunkPos pos = ColumnPosArgument.getColumnPos(context, "pos").toChunkPos();
                        setForPlayer(context.getSource(), pos, !isChunkLoaded(context.getSource(), pos));
                        return 1;
                    })
                    .then(Commands.argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            ChunkPos pos = ColumnPosArgument.getColumnPos(context, "pos").toChunkPos();
                            setForPlayer(context.getSource(), pos, BoolArgumentType.getBool(context, "state"));
                            return 1;
                        })
                    )
                )
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ChunkPos pos = source.getPlayerOrException().chunkPosition();
                    setForPlayer(source, pos, !isChunkLoaded(source, pos));
                    return 1;
                })
            ));
    }

    private static void setForPlayer(CommandSourceStack source, ChunkPos pos, boolean state) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var claim = ClaimApi.API.getClaim(source.getLevel(), pos);
        if (claim.isEmpty()) throw NOT_CLAIMED.create();

        var owned = ClaimApi.API.getOwnedClaims(player).orElse(null);
        if (owned == null || !owned.containsKey(pos)) throw NOT_OWNER.create();
        if (!TeamApi.API.canManageClaims(player, claim.get().team())) throw NO_CLAIM_PERMISSION.create();

        set(source, pos, state, claim.get().team());
    }

    public static void set(CommandSourceStack source, ChunkPos pos, boolean state, TeamId team) throws CommandSyntaxException {
        var claim = ClaimApi.API.getClaim(source.getLevel(), pos);
        if (claim.isEmpty()) throw NOT_CLAIMED.create();

        if (claim.get().isChunkLoaded() != state) {
            if (state && ClaimCommand.getClaimsCount(source.getLevel(), team, true) >= ClaimLimitApi.API.getMaxChunkLoadedClaims(team)) {
                throw FORCELOAD_LIMIT.create();
            }
            ClaimApi.API.setChunkLoaded(source.getLevel(), team, pos, state);
        }

        int loaded = ClaimCommand.getClaimsCount(source.getLevel(), team, true);
        int maxLoaded = ClaimLimitApi.API.getMaxChunkLoadedClaims(team);
        source.sendSuccess(() -> ModUtils.translatableWithStyle(
            state ? "command.cadmus.info.forceload_enabled" : "command.cadmus.info.forceload_disabled",
            pos.x, pos.z, loaded, maxLoaded
        ), false);
    }

    public static boolean isChunkLoaded(CommandSourceStack source, ChunkPos pos) {
        return ClaimApi.API.getClaim(source.getLevel(), pos).map(ClaimData::isChunkLoaded).orElse(false);
    }
}
