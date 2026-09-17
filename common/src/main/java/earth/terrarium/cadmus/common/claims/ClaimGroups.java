package earth.terrarium.cadmus.common.claims;

import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.common.outposts.OutpostManager;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class ClaimGroups {

    private ClaimGroups() {}

    public static boolean canRemove(Level level, TeamId team, Set<ChunkPos> positions) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        MinecraftServer server = serverLevel.getServer();
        return TownManager.canRemove(server, team, positions)
            && OutpostManager.canRemove(server, serverLevel.dimension().location(), team, positions);
    }
}
