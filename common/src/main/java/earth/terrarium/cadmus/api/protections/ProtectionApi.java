package earth.terrarium.cadmus.api.protections;

import earth.terrarium.cadmus.api.ApiHelper;

public interface ProtectionApi {

    ProtectionApi API = ApiHelper.load(ProtectionApi.class);

    /**
     * Registers a protection.
     *
     * @param protection The protection.
     * @throws IllegalArgumentException if the protection is already registered.
     */
    void register(Protection protection);
}