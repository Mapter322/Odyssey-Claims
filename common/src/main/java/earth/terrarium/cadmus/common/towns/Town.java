package earth.terrarium.cadmus.common.towns;

import earth.terrarium.cadmus.api.teams.TeamId;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record Town(UUID id, TeamId team, Set<ChunkPos> chunks) {
    public Town(UUID id, TeamId team) {
        this(id, team, new HashSet<>());
    }
}
