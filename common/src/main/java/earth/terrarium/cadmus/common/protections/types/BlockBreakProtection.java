package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.BlockCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBreakProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.BLOCK_BREAK;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_BLOCK_BREAKING;
    }

    public boolean canBreakBlock(Level level, GameProfile player, BlockPos pos) {
        if (level.isClientSide()) return true;
        BlockState state = level.getBlockState(pos);
        return getId(level, pos)
            .map(id -> isPlayerAllowed(level, player, id, specific(state, Settings.scopeOf(id))))
            .orElse(true);
    }

    public boolean canBreakBlock(Player player, BlockPos pos) {
        return canBreakBlock(player.level(), player.getGameProfile(), pos);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(BlockState state, SettingScope scope) {
        SettingDefinition<Boolean> best = switch (scope) {
            case ADMIN_CLAIM -> SettingDefinitions.ADMIN_BLOCK_BREAK;
            case WILDERNESS -> SettingDefinitions.WILDERNESS_BLOCK_BREAK;
            case TOWN -> SettingDefinitions.BLOCK_BREAK;
        };
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "block-break")) {
            for (SettingCondition<?> condition : definition.conditions()) {
                if (condition instanceof BlockCondition block && block.matches(state.getBlock()) && block.priority() > bestPriority) {
                    bestPriority = block.priority();
                    best = (SettingDefinition<Boolean>) definition;
                }
            }
        }
        return best;
    }
}
