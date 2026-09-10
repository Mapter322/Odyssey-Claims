package earth.terrarium.cadmus.api.settings.types;

import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.cadmus.api.settings.SettingValue;

import java.util.Objects;

public record ColorSetting(Color value) implements SettingValue<Color> {

    public ColorSetting {
        Objects.requireNonNull(value, "value");
    }
}
