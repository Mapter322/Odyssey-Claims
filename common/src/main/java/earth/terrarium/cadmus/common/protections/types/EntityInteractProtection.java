package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.EntityCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class EntityInteractProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.ENTITY_INTERACTIONS;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_ENTITY_INTERACTIONS;
    }

    public boolean canInteractWithEntity(Player player, Entity entity) {
        return canInteractWithEntity(player.level(), player.getGameProfile(), entity);
    }

    public boolean canInteractWithEntity(Level level, GameProfile player, Entity entity) {
        if (level.isClientSide()) return true;
        return getId(level, entity.chunkPosition())
            .map(id -> isPlayerAllowed(level, player, id, specific(entity, id.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN)))
            .orElse(true);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(Entity entity, SettingScope scope) {
        SettingDefinition<Boolean> best = scope == SettingScope.ADMIN_CLAIM
            ? SettingDefinitions.ADMIN_ENTITY_INTERACTIONS
            : SettingDefinitions.ENTITY_INTERACTIONS;
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "entity-interactions")) {
            for (SettingCondition<?> condition : definition.conditions()) {
                if (condition instanceof EntityCondition entityCondition && entityCondition.matches(entity.getType()) && entityCondition.priority() > bestPriority) {
                    bestPriority = entityCondition.priority();
                    best = (SettingDefinition<Boolean>) definition;
                }
            }
        }
        return best;
    }
}
