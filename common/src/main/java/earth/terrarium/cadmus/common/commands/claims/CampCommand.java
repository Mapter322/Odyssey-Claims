package earth.terrarium.cadmus.common.commands.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.common.camps.CampManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CampCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cadmus")
            .then(Commands.literal("claim")
                .then(Commands.literal("camp")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String error = CampManager.create(player, player.chunkPosition());
                        if (error != null) throw new SimpleCommandExceptionType(Component.translatable(error)).create();
                        context.getSource().sendSuccess(() -> ModUtils.translatableWithStyle("command.cadmus.info.camp_created"), false);
                        return 1;
                    }))
            ));
    }
}
