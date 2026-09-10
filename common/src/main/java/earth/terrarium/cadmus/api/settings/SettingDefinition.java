package earth.terrarium.cadmus.api.settings;

import java.util.Objects;

public record SettingDefinition<T>(
    String id,
    SettingScope scope,
    SettingTarget target,
    SettingAccess access,
    SettingValue<T> defaultValue
) {

    public SettingDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(defaultValue, "defaultValue");
    }
}
