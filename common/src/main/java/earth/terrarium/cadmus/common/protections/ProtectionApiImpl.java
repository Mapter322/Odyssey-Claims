package earth.terrarium.cadmus.common.protections;

import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.protections.ProtectionApi;

import java.util.HashMap;
import java.util.Map;

public class ProtectionApiImpl implements ProtectionApi {

    private final Map<String, Protection> protections = new HashMap<>();

    @Override
    public void register(Protection protection) {
        if (this.protections.containsKey(protection.setting().id())) {
            throw new IllegalArgumentException("Protection already registered: " + protection.setting());
        }
        this.protections.put(protection.setting().id(), protection);
    }
}