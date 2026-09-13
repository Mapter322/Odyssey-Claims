package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.BlockCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.tags.ModBlockTags;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class BlockPlaceProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.BLOCK_PLACE;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_BLOCK_PLACING;
    }

    public boolean canPlaceBlock(@NotNull Entity entity, BlockPos pos, BlockState state) {
        if (entity.level().isClientSide()) return true;
        TeamId id = getId(entity.level(), pos).orElse(null);
        if (id == null) return true;
        if (isBlockAllowed(entity.level(), id, state)) return true;

        return entity instanceof Player player ?
            isPlayerAllowed(entity.level(), player.getGameProfile(), id, specific(state, id.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN)) :
            isEntityAllowed(entity, id);
    }

    public boolean canPlaceBlock(Level level, GameProfile player, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return true;
        TeamId id = getId(level, pos).orElse(null);
        if (id == null) return true;
        if (isBlockAllowed(level, id, state)) return true;

        return isPlayerAllowed(level, player, id, specific(state, id.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN));
    }

    public boolean canPlaceBlock(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return true;
        TeamId id = getId(level, pos).orElse(null);
        if (id == null) return true;
        if (isBlockAllowed(level, id, state)) return true;
        return Settings.getForTeam(level.getServer(), id, SettingDefinitions.NON_PLAYERS_PLACE);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(BlockState state, SettingScope scope) {
        SettingDefinition<Boolean> best = scope == SettingScope.ADMIN_CLAIM
            ? SettingDefinitions.ADMIN_BLOCK_PLACE
            : SettingDefinitions.BLOCK_PLACE;
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "block-place")) {
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
