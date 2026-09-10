package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class BlockBreakProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.BLOCK_BREAK;
    }

    @Override
    public String permission() {
        return "cadmus.block_breaking";
    }

    @Override
    public String personalPermission() {
        return "cadmus.personal.block_breaking";
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_BLOCK_BREAKING;
    }

    public boolean canBreakBlock(Level level, GameProfile player, BlockPos pos) {
        return level.isClientSide() || getId(level, pos).map(id ->
            isPlayerAllowed(level, player, id) || isBlockAllowed(level, id, pos)).orElse(true);
    }

    public boolean canBreakBlock(Player player, BlockPos pos) {
        return canBreakBlock(player.level(), player.getGameProfile(), pos);
    }
}
