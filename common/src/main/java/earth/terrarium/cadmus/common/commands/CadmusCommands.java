package earth.terrarium.cadmus.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import earth.terrarium.cadmus.common.commands.admin.*;
import earth.terrarium.cadmus.common.commands.claims.*;
import earth.terrarium.cadmus.common.commands.towns.TownCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public class CadmusCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        ClaimInfoCommand.register(dispatcher);
        UnclaimCommand.register(dispatcher);
        UnclaimAreaCommand.register(dispatcher);
        ClaimSettingsCommand.register(dispatcher);
        ClaimAllowedBlocksCommand.register(dispatcher, context);
        TownCommand.register(dispatcher);

        AdminCommands.register(dispatcher);
        DefaultSettingsCommand.register(dispatcher);
        AdminSettingsCommand.register(dispatcher);
        BypassCommand.register(dispatcher);
        FlagCommands.register(dispatcher);
        AdminClaimCommands.register(dispatcher);
    }
}
