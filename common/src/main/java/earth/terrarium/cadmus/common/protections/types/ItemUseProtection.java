package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.ItemCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class ItemUseProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.USE;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return null;
    }

    public boolean canUseItem(Player player, ItemStack stack) {
        return canUseItem(player.level(), player.getGameProfile(), player.chunkPosition(), stack);
    }

    public boolean canUseItem(Level level, GameProfile player, ChunkPos pos, ItemStack stack) {
        if (level.isClientSide()) return true;
        return getId(level, pos)
            .map(id -> isPlayerAllowed(level, player, id, specific(stack, Settings.scopeOf(id))))
            .orElse(true);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(ItemStack stack, SettingScope scope) {
        SettingDefinition<Boolean> best = switch (scope) {
            case ADMIN_CLAIM -> SettingDefinitions.ADMIN_USE;
            case WILDERNESS -> SettingDefinitions.WILDERNESS_USE;
            case TOWN -> SettingDefinitions.USE;
        };
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "use")) {
            for (SettingCondition<?> condition : definition.conditions()) {
                if (condition instanceof ItemCondition itemCondition && itemCondition.matches(stack.getItem()) && itemCondition.priority() > bestPriority) {
                    bestPriority = itemCondition.priority();
                    best = (SettingDefinition<Boolean>) definition;
                }
            }
        }
        return best;
    }
}
