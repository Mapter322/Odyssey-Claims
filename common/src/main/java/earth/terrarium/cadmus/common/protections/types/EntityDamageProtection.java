package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.EntityCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.tags.ModEntityTypeTags;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class EntityDamageProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.ENTITY_DAMAGE;
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_ENTITY_DAMAGE;
    }

    public boolean canDamageEntity(Player player, Entity entity) {
        return canDamageEntity(player.level(), player.getGameProfile(), entity);
    }

    public boolean canDamageEntity(Level level, GameProfile player, Entity entity) {
        if (entity.getType().is(ModEntityTypeTags.ALLOWS_CLAIM_DAMAGE_ENTITIES)) return true;
        if (level.isClientSide()) return true;
        return getId(level, entity.chunkPosition()).map(team ->
            checkFlags(level.getServer(), entity, team, specific(entity, team.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN))
                && isPlayerAllowed(level, player, team, specific(entity, team.isAdmin() ? SettingScope.ADMIN_CLAIM : SettingScope.TOWN))).orElse(true);
    }

    @SuppressWarnings("unchecked")
    private static SettingDefinition<Boolean> specific(Entity entity, SettingScope scope) {
        SettingDefinition<Boolean> best = scope == SettingScope.ADMIN_CLAIM
            ? SettingDefinitions.ADMIN_ENTITY_DAMAGE
            : SettingDefinitions.ENTITY_DAMAGE;
        int bestPriority = 0;
        for (SettingDefinition<?> definition : SettingDefinitions.childrenOf(scope, "entity-damage")) {
            for (SettingCondition<?> condition : definition.conditions()) {
                if (condition instanceof EntityCondition entityCondition && entityCondition.matches(entity.getType()) && entityCondition.priority() > bestPriority) {
                    bestPriority = entityCondition.priority();
                    best = (SettingDefinition<Boolean>) definition;
                }
            }
        }
        return best;
    }

    private boolean checkFlags(MinecraftServer server, Entity entity, TeamId team, SettingDefinition<Boolean> definition) {
        if (!team.isAdmin()) return true;

        if (entity instanceof Player) return Settings.getForTeam(server, team, SettingDefinitions.PVP);

        if (entity instanceof Enemy || entity.getType().is(ModEntityTypeTags.MONSTERS)) {
            return Settings.getForTeam(server, team, SettingDefinitions.ADMIN_MONSTER_DAMAGE);
        } else {
            if (entity instanceof Mob || entity.getType().is(ModEntityTypeTags.CREATURES)) {
                return Settings.getForTeam(server, team, SettingDefinitions.ADMIN_CREATURE_DAMAGE);
            }

            return Settings.getForTeam(server, team, definition);
        }
    }
}
