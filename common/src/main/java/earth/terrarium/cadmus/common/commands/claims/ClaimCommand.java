package earth.terrarium.cadmus.common.commands.claims;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.api.teams.TeamApi;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicInteger;

public class ClaimCommand {

    public static void checkClaimed(ServerLevel level, ChunkPos pos) throws CommandSyntaxException {
        var claim = ClaimApi.API.getClaim(level, pos);
        if (claim.isPresent()) {
            Component name = TeamApi.API.getName(level, claim.get().team());
            throw new SimpleCommandExceptionType(ModUtils.translatableWithStyle(
                "command.cadmus.exception.already_claimed",
                name
            )).create();
        }
    }

    public static int getClaimsCount(Level level, TeamId id, boolean chunkload) {
        var claims = ClaimApi.API.getOwnedClaims(level, id).orElse(null);
        if (claims == null) return 0;
        return chunkload ?
            (int) claims.values().stream().filter(loaded -> loaded).count() :
            claims.size();
    }

    public static int getClaimsCount(Player player, boolean chunkload) {
        AtomicInteger count = new AtomicInteger();
        TeamApi.API.getTeamsList(player).forEach(team -> {
            var claims = ClaimApi.API.getOwnedClaims(player.level(), team).orElse(null);
            if (claims != null) {
                if (chunkload) {
                    count.addAndGet((int) claims.values().stream().filter(loaded -> loaded).count());
                } else {
                    count.addAndGet(claims.size());
                }
            }
        });
        return count.get();
    }
}
