package earth.terrarium.cadmus.api.settings;

import java.util.List;
import java.util.Objects;

public record SettingDefinition<T>(
    String id,
    SettingScope scope,
    SettingTarget target,
    SettingAccess access,
    SettingValue<T> defaultValue,
    String parent,
    List<SettingCondition<?>> conditions
) {

    public SettingDefinition(String id, SettingScope scope, SettingTarget target, SettingAccess access, SettingValue<T> defaultValue) {
        this(id, scope, target, access, defaultValue, "", List.of());
    }

    public SettingDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(defaultValue, "defaultValue");
        if (parent == null) parent = "";
        if (conditions == null) conditions = List.of();
    }

    public boolean hasParent() {
        return !this.parent.isEmpty();
    }

    public boolean hasConditions() {
        return !this.conditions.isEmpty();
    }
}
