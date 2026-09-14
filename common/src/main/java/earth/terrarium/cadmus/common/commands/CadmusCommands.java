package earth.terrarium.cadmus.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import earth.terrarium.cadmus.common.commands.admin.AdminClaimCommands;
import earth.terrarium.cadmus.common.commands.claims.CampCommand;
import earth.terrarium.cadmus.common.commands.claims.ClaimInfoCommand;
import earth.terrarium.cadmus.common.commands.claims.ClaimSettingsCommand;
import earth.terrarium.cadmus.common.commands.claims.UnclaimAreaCommand;
import earth.terrarium.cadmus.common.commands.claims.UnclaimCommand;
import earth.terrarium.cadmus.common.commands.towns.TownCommand;
import net.minecraft.commands.CommandSourceStack;

public class CadmusCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ClaimInfoCommand.register(dispatcher);
        UnclaimCommand.register(dispatcher);
        UnclaimAreaCommand.register(dispatcher);
        ClaimSettingsCommand.register(dispatcher);
        CampCommand.register(dispatcher);
        TownCommand.register(dispatcher);

        AdminClaimCommands.register(dispatcher);
    }
}
