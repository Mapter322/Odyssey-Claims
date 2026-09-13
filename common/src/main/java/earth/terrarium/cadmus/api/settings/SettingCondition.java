package earth.terrarium.cadmus.api.settings;

public interface SettingCondition<T> {

    boolean matches(T value);

    String display();
}
