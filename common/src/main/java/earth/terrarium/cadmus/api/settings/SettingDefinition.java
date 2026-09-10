package earth.terrarium.cadmus.api.settings;

import java.util.Objects;

public record SettingDefinition<T>(
    String id,
    SettingScope scope,
    SettingCategory category,
    SettingAccess access,
    SettingValue<T> defaultValue
) {

    public SettingDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(defaultValue, "defaultValue");
    }
}
