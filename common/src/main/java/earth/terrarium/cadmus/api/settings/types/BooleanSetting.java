package earth.terrarium.cadmus.api.settings.types;

import earth.terrarium.cadmus.api.settings.SettingValue;

import java.util.Objects;

public record BooleanSetting(Boolean value) implements SettingValue<Boolean> {

    public BooleanSetting {
        Objects.requireNonNull(value, "value");
    }
}
