package earth.terrarium.cadmus.api.protections;

import java.util.UUID;

public interface PlacerTracked {

    String PLACER_KEY = "cadmus:placer";

    UUID cadmus$getPlacerUUID();

    void cadmus$setPlacerUUID(UUID uuid);
}
