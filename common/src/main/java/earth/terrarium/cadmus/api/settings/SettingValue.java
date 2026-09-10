package earth.terrarium.cadmus.api.settings;

/**
 * A typed value used by a Cadmus setting.
 *
 * @param <T> the Java type represented by the value
 */
public interface SettingValue<T> {

    T value();
}
