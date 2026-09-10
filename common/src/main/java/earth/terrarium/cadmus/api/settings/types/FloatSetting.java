package earth.terrarium.cadmus.api.settings.types;

import earth.terrarium.cadmus.api.settings.SettingValue;

import java.util.Objects;

public record FloatSetting(Float value) implements SettingValue<Float> {

    public FloatSetting {
        Objects.requireNonNull(value, "value");
    }
}
