package earth.terrarium.cadmus.common.protections.types;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
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
    public String permission() {
        return "cadmus.entity_damage";
    }

    @Override
    public String personalPermission() {
        return "cadmus.personal.entity_damage";
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
        return level.isClientSide() || getId(level, entity.chunkPosition()).map(team ->
            checkFlags(level.getServer(), entity, team) && isPlayerAllowed(level, player, team)).orElse(true);
    }

    private boolean checkFlags(MinecraftServer server, Entity entity, TeamId team) {
        if (!team.isAdmin()) return true;

        if (entity instanceof Player) return Settings.getForTeam(server, team, SettingDefinitions.PVP);

        if (entity instanceof Enemy || entity.getType().is(ModEntityTypeTags.MONSTERS)) {
            return Settings.getForTeam(server, team, SettingDefinitions.ADMIN_MONSTER_DAMAGE);
        } else {
            if (entity instanceof Mob || entity.getType().is(ModEntityTypeTags.CREATURES)) {
                return Settings.getForTeam(server, team, SettingDefinitions.ADMIN_CREATURE_DAMAGE);
            }

            return Settings.getForTeam(server, team, SettingDefinitions.ENTITY_DAMAGE);
        }
    }
}
