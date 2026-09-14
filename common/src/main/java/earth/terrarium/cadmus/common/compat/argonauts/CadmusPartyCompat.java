package earth.terrarium.cadmus.common.compat.argonauts;

import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.cadmus.common.teams.CampTeamProvider;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CadmusPartyCompat {

    private CadmusPartyCompat() {
    }

    public static void register() {
        CampTeamProvider.setPartyResolver(new CampTeamProvider.PartyResolver() {
            @Override
            public boolean isInSameParty(Level level, UUID owner, UUID player) {
                if (owner.equals(player)) return true;
                Optional<Party> ownerParty = PartyApi.API.getPlayerParty(level, owner);
                Optional<Party> playerParty = PartyApi.API.getPlayerParty(level, player);
                return ownerParty.isPresent() && playerParty.isPresent() && ownerParty.get().id().equals(playerParty.get().id());
            }

            @Override
            public Set<UUID> getPartyMembers(Level level, UUID owner) {
                return PartyApi.API.getPlayerParty(level, owner)
                    .map(party -> Set.copyOf(party.members().keySet()))
                    .orElse(Set.of());
            }
        });
    }
}
