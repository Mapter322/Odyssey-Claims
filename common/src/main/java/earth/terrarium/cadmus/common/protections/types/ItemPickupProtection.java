package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
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
    public String permission() {
        return "cadmus.item_pickup";
    }

    @Override
    public String personalPermission() {
        return "cadmus.personal.item_pickup";
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
        return Objects.equals(((ItemEntityAccessor) item).getThrower(), player.getId()) ||
            level.isClientSide() ||
            getId(level, item.chunkPosition()).map(id ->
                isPlayerAllowed(level, player, id)).orElse(true);
    }
}
