package earth.terrarium.cadmus.common.settings;

import earth.terrarium.cadmus.api.settings.BlockTagCondition;
import earth.terrarium.cadmus.api.settings.BlockValueCondition;
import earth.terrarium.cadmus.api.settings.EntityTagCondition;
import earth.terrarium.cadmus.api.settings.EntityValueCondition;
import earth.terrarium.cadmus.api.settings.ItemTagCondition;
import earth.terrarium.cadmus.api.settings.ItemValueCondition;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class TargetConditions {

    private static final Map<String, TargetKind> KINDS = Map.of(
        "block-break", TargetKind.BLOCK,
        "block-place", TargetKind.BLOCK,
        "block-interactions", TargetKind.BLOCK,
        "entity-interactions", TargetKind.ENTITY,
        "entity-damage", TargetKind.ENTITY,
        "item-pickup", TargetKind.ITEM,
        "use", TargetKind.ITEM
    );

    private TargetConditions() {
    }

    public static boolean isValidParent(String parent) {
        return KINDS.containsKey(parent);
    }

    @Nullable
    public static String parentOf(String condition) {
        int index = condition.indexOf('/');
        if (index <= 0 || index >= condition.length() - 1) return null;
        return condition.substring(0, index);
    }

    @Nullable
    public static String keyOf(String condition) {
        int index = condition.indexOf('/');
        if (index <= 0 || index >= condition.length() - 1) return null;
        return condition.substring(index + 1);
    }

    @Nullable
    public static SettingCondition<?> create(String parent, String key) {
        TargetKind kind = KINDS.get(parent);
        if (kind == null) return null;
        boolean tag = key.startsWith("#");
        ResourceLocation location = ResourceLocation.tryParse(tag ? key.substring(1) : key);
        if (location == null) return null;
        return switch (kind) {
            case BLOCK -> tag
                ? new BlockTagCondition(TagKey.create(Registries.BLOCK, location))
                : new BlockValueCondition(location);
            case ITEM -> tag
                ? new ItemTagCondition(TagKey.create(Registries.ITEM, location))
                : new ItemValueCondition(location);
            case ENTITY -> tag
                ? new EntityTagCondition(TagKey.create(Registries.ENTITY_TYPE, location))
                : new EntityValueCondition(location);
        };
    }

    private enum TargetKind {
        BLOCK,
        ITEM,
        ENTITY
    }
}
