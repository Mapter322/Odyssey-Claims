package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.ItemCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.tags.ModItemTags;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import earth.terrarium.cadmus.mixins.common.ItemEntityAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class ItemPickupProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.ITEM_PICKUP;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_ITEM_PICKUP;
    }

    public boolean canPickupItem(Player player, ItemEntity item) {
        return canPickupItem(player.level(), player.getGameProfile(), item);
    }

    public boolean canPickupItem(Level level, GameProfile player, ItemEntity item) {
        if (item.getItem().is(ModItemTags.ALLOWS_CLAIM_PICKUP)) return true;
        if (Objects.equals(((ItemEntityAccessor) item).getThrower(), player.getId())) return true;
        if (level.isClientSide()) return true;
        return getId(level, item.chunkPosition()).map(id ->
            isPlayerAllowed(level, player, id, specific(item, id.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN))).orElse(true);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(ItemEntity item, SettingScope scope) {
        SettingDefinition<Boolean> best = scope == SettingScope.ADMIN_CLAIM
            ? SettingDefinitions.ADMIN_ITEM_PICKUP
            : SettingDefinitions.ITEM_PICKUP;
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "item-pickup")) {
            for (SettingCondition<?> condition : definition.conditions()) {
                if (condition instanceof ItemCondition itemCondition && itemCondition.matches(item.getItem().getItem()) && itemCondition.priority() > bestPriority) {
                    bestPriority = itemCondition.priority();
                    best = (SettingDefinition<Boolean>) definition;
                }
            }
        }
        return best;
    }
}
