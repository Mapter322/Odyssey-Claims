package earth.terrarium.cadmus.common.outposts;

import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import earth.terrarium.cadmus.api.teams.TeamId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OutpostSaveData extends SaveHandler {

    private final Map<UUID, Outpost> outposts = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag outpostsTag = tag.getCompound("outposts");
        outpostsTag.getAllKeys().forEach(idString -> {
            CompoundTag outpostTag = outpostsTag.getCompound(idString);
            TeamId team = new TeamId(ResourceLocation.parse(outpostTag.getString("provider")), UUID.fromString(outpostTag.getString("team")));
            ResourceLocation dimension = ResourceLocation.parse(outpostTag.getString("dimension"));
            Set<ChunkPos> chunks = new HashSet<>();
            outpostTag.getList("chunks", Tag.TAG_LONG).forEach(chunk -> {
                long value = ((LongTag) chunk).getAsLong();
                chunks.add(new ChunkPos(BlockPos.getX(value), BlockPos.getZ(value)));
            });
            UUID id = UUID.fromString(idString);
            outposts.put(id, new Outpost(id, team, dimension, chunks));
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag outpostsTag = new CompoundTag();
        outposts.forEach((id, outpost) -> {
            CompoundTag outpostTag = new CompoundTag();
            outpostTag.putString("provider", outpost.team().provider().toString());
            outpostTag.putString("team", outpost.team().id().toString());
            outpostTag.putString("dimension", outpost.dimension().toString());
            ListTag chunks = new ListTag();
            outpost.chunks().forEach(pos -> chunks.add(LongTag.valueOf(BlockPos.asLong(pos.x, 0, pos.z))));
            outpostTag.put("chunks", chunks);
            outpostsTag.put(id.toString(), outpostTag);
        });
        tag.put("outposts", outpostsTag);
    }

    public Map<UUID, Outpost> outposts() {
        return this.outposts;
    }

    public static OutpostSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), HandlerType.create(OutpostSaveData::new), "cadmus_outposts");
    }
}
