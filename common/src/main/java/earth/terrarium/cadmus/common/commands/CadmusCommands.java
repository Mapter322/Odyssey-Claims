package earth.terrarium.cadmus.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import earth.terrarium.cadmus.common.commands.admin.*;
import earth.terrarium.cadmus.common.commands.claims.*;
import earth.terrarium.cadmus.common.commands.towns.TownCommand;
import net.minecraft.commands.CommandSourceStack;

public class CadmusCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ClaimInfoCommand.register(dispatcher);
        UnclaimCommand.register(dispatcher);
        UnclaimAreaCommand.register(dispatcher);
        ClaimSettingsCommand.register(dispatcher);
        TownCommand.register(dispatcher);

AdminCommands.register(dispatcher);
        AdminSettingsCommand.register(dispatcher);
        AdminClaimCommands.register(dispatcher);
    }
}
