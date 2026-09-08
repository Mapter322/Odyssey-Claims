package earth.terrarium.cadmus.common.commands.towns;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class TownCommand {
    private TownCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var create = Commands.literal("create")
            .then(Commands.argument("start", ColumnPosArgument.columnPos())
                .then(Commands.argument("end", ColumnPosArgument.columnPos())
                    .executes(context -> create(context.getSource(),
                        ColumnPosArgument.getColumnPos(context, "start").toChunkPos(),
                        ColumnPosArgument.getColumnPos(context, "end").toChunkPos()))))
            .executes(context -> create(context.getSource(),
                context.getSource().getPlayerOrException().chunkPosition(),
                context.getSource().getPlayerOrException().chunkPosition()));

        var add = Commands.literal("add")
            .then(Commands.argument("town", UuidArgument.uuid())
                .then(Commands.argument("start", ColumnPosArgument.columnPos())
                    .then(Commands.argument("end", ColumnPosArgument.columnPos())
                        .executes(context -> add(context.getSource(),
                            UuidArgument.getUuid(context, "town"),
                            ColumnPosArgument.getColumnPos(context, "start").toChunkPos(),
                            ColumnPosArgument.getColumnPos(context, "end").toChunkPos())))));

        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("town").then(create).then(add)));
    }

    private static int create(CommandSourceStack source, ChunkPos start, ChunkPos end) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component error = TownManager.create(player, start, end);
        if (error != null) {
            throw new SimpleCommandExceptionType(error).create();
        }
        source.sendSuccess(() -> Component.literal("Town created"), false);
        return 1;
    }

    private static int add(CommandSourceStack source, java.util.UUID town, ChunkPos start, ChunkPos end) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component error = TownManager.add(player, town, start, end);
        if (error != null) {
            throw new SimpleCommandExceptionType(error).create();
        }
        source.sendSuccess(() -> Component.literal("Town expanded"), false);
        return 1;
    }
}
