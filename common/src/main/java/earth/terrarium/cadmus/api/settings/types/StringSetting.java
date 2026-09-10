package earth.terrarium.cadmus.api.settings.types;

import earth.terrarium.cadmus.api.settings.SettingValue;

import java.util.Objects;

public record StringSetting(String value) implements SettingValue<String> {

    public StringSetting {
        Objects.requireNonNull(value, "value");
    }
}
