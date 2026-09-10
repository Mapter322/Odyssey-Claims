package earth.terrarium.cadmus.common.protections;

import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.protections.ProtectionApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProtectionApiImpl implements ProtectionApi {

    private final Map<String, Protection> protections = new HashMap<>();
    private final Set<String> settings = new HashSet<>();

    @Override
    public void register(Protection protection) {
        if (this.protections.containsKey(protection.setting().id())) {
            throw new IllegalArgumentException("Protection already registered: " + protection.setting());
        }
        this.protections.put(protection.setting().id(), protection);
        registerSetting(protection.setting().id());
    }

    @Override
    public void registerSetting(String setting) {
        this.settings.add(setting);
    }

    @Override
    public Protection getProtection(String setting) {
        return this.protections.get(setting);
    }

    @Override
    public Map<String, Protection> getProtections() {
        return this.protections;
    }

    @Override
    public Set<String> getSettings() {
        return this.settings;
    }
}
