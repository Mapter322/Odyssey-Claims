package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.BlockCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.config.CadmusConfig;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.tags.ModBlockTags;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import earth.terrarium.cadmus.common.utils.CadmusNotifications;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockInteractProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.BLOCK_INTERACTIONS;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_BLOCK_INTERACTIONS;
    }

    public boolean canInteractWithBlock(Player player, BlockPos pos, BlockState state) {
        return canInteractWithBlock(player.level(), player.getGameProfile(), pos, state);
    }

    public boolean canInteractWithBlock(Level level, GameProfile player, BlockPos pos, BlockState state) {
        if (state.is(ModBlockTags.ALLOWS_CLAIM_INTERACTIONS)) return true;
        if (level.isClientSide()) return true;
        if (CadmusConfig.get().isPublicBlockInteraction(state)) return true;
        boolean allowed = getId(level, pos)
            .map(id -> isPlayerAllowed(level, player, id, specific(state, Settings.scopeOf(id))))
            .orElse(true);
        if (!allowed) CadmusNotifications.noAccess(level, player);
        return allowed;
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(BlockState state, SettingScope scope) {
        SettingDefinition<Boolean> best = switch (scope) {
            case ADMIN_CLAIM -> SettingDefinitions.ADMIN_BLOCK_INTERACTIONS;
            case WILDERNESS -> SettingDefinitions.WILDERNESS_BLOCK_INTERACTIONS;
            case TOWN -> SettingDefinitions.BLOCK_INTERACTIONS;
        };
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "block-interactions")) {
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
