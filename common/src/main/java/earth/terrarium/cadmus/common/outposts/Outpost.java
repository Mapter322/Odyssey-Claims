package earth.terrarium.cadmus.common.outposts;

import earth.terrarium.cadmus.api.teams.TeamId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record Outpost(UUID id, TeamId team, ResourceLocation dimension, Set<ChunkPos> chunks) {
    public Outpost(UUID id, TeamId team, ResourceLocation dimension) {
        this(id, team, dimension, new HashSet<>());
    }
}
