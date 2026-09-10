package earth.terrarium.cadmus.common.protections.types;

import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.tags.ModEntityTypeTags;
import earth.terrarium.cadmus.common.utils.CadmusGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;

public final class MobGriefingProtection implements Protection {

    @Override
    public SettingDefinition<Boolean> setting() {
        return SettingDefinitions.MOB_GRIEFING;
    }

    @Override
    public String permission() {
        return "cadmus.mob_griefing";
    }

    @Override
    public String personalPermission() {
        return "cadmus.personal.mob_griefing";
    }

    @Override
    public GameRules.Key<GameRules.BooleanValue> gameRule() {
        return CadmusGameRules.DO_CLAIMED_MOB_GRIEFING;
    }

    public boolean canMobGrief(Entity entity) {
        return canMobGrief(entity, entity.chunkPosition());
    }

    public boolean canMobGrief(Entity entity, BlockPos pos) {
        return canMobGrief(entity, new ChunkPos(pos));
    }

    public boolean canMobGrief(Entity entity, ChunkPos pos) {
        if (entity.getType().is(ModEntityTypeTags.CAN_GRIEF_ENTITIES)) return true;
        return entity.level().isClientSide() || getId(entity.level(), pos).map(id ->
            isEntityAllowed(entity, id)).orElse(true);
    }
}
