package earth.terrarium.cadmus.common.compat.argonauts;

import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.common.config.RoleDefaultsConfig;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.settings.BlockTagCondition;
import earth.terrarium.cadmus.api.settings.BlockValueCondition;
import earth.terrarium.cadmus.api.settings.EntityTagCondition;
import earth.terrarium.cadmus.api.settings.EntityValueCondition;
import earth.terrarium.cadmus.api.settings.ItemTagCondition;
import earth.terrarium.cadmus.api.settings.ItemValueCondition;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public final class CadmusRoleTargets {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cadmus.MOD_ID);
    private static final Map<String, TargetKind> KINDS = Map.of(
        "block-break", TargetKind.BLOCK,
        "block-place", TargetKind.BLOCK,
        "block-interactions", TargetKind.BLOCK,
        "entity-interactions", TargetKind.ENTITY,
        "entity-damage", TargetKind.ENTITY,
        "item-pickup", TargetKind.ITEM,
        "use", TargetKind.ITEM
    );

    private CadmusRoleTargets() {
    }

    public static void register() {
        RoleDefaultsConfig.ensureLoaded();
        for (RoleDefaultsConfig.RoleTarget target : RoleDefaultsConfig.targets()) {
            TargetKind kind = KINDS.get(target.parent());
            if (kind == null) {
                LOGGER.warn("Skipping role default target '{}' with unknown parent '{}'", target.key(), target.parent());
                continue;
            }
            SettingCondition<?> condition = condition(kind, target.key());
            if (condition == null) continue;
            String id = target.parent() + "/" + target.key();
            SettingDefinitions.register(new SettingDefinition<>(id, SettingScope.TOWN, SettingTarget.PLAYER,
                SettingAccess.PLAYER, new BooleanSetting(true), target.parent(), List.of(condition)));
            MemberSettingsApi.API.register(new MemberSetting(id, Component.literal(target.key()), Component.empty(), target.parent()));
        }
    }

    private static SettingCondition<?> condition(TargetKind kind, String key) {
        boolean tag = key.startsWith("#");
        ResourceLocation location = ResourceLocation.tryParse(tag ? key.substring(1) : key);
        if (location == null) {
            LOGGER.warn("Skipping invalid role default target '{}'", key);
            return null;
        }
        return switch (kind) {
            case BLOCK -> tag ? new BlockTagCondition(TagKey.create(Registries.BLOCK, location)) : new BlockValueCondition(location);
            case ITEM -> tag ? new ItemTagCondition(TagKey.create(Registries.ITEM, location)) : new ItemValueCondition(location);
            case ENTITY -> tag ? new EntityTagCondition(TagKey.create(Registries.ENTITY_TYPE, location)) : new EntityValueCondition(location);
        };
    }

    private enum TargetKind {
        BLOCK, ITEM, ENTITY
    }
}
