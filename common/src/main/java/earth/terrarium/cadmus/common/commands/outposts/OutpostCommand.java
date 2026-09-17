package earth.terrarium.cadmus.common.commands.outposts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.common.outposts.OutpostManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class OutpostCommand {

    private OutpostCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("outpost")
                .then(Commands.literal("create")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        create(context.getSource(), player.chunkPosition(), player.chunkPosition());
                        return 1;
                    })
                    .then(Commands.argument("startPos", ColumnPosArgument.columnPos())
                        .then(Commands.argument("endPos", ColumnPosArgument.columnPos())
                            .executes(context -> {
                                ChunkPos startPos = ColumnPosArgument.getColumnPos(context, "startPos").toChunkPos();
                                ChunkPos endPos = ColumnPosArgument.getColumnPos(context, "endPos").toChunkPos();
                                create(context.getSource(), startPos, endPos);
                                return 1;
                            }))))));
    }

    private static void create(CommandSourceStack source, ChunkPos start, ChunkPos end) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component error = OutpostManager.claim(player, start, end);
        if (error != null) throw new SimpleCommandExceptionType(error).create();
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.outpost_claimed"), false);
    }
}
