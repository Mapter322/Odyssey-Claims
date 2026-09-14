package earth.terrarium.cadmus.common.camps;

import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CampSaveData extends SaveHandler {

    private final Map<UUID, CampEntry> camps = new HashMap<>();

    public record CampEntry(ResourceLocation dimension, ChunkPos pos, long expiresAt) {}

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag campsTag = tag.getCompound("camps");
        campsTag.getAllKeys().forEach(owner -> {
            CompoundTag campTag = campsTag.getCompound(owner);
            camps.put(UUID.fromString(owner), new CampEntry(
                ResourceLocation.parse(campTag.getString("dimension")),
                new ChunkPos(campTag.getInt("x"), campTag.getInt("z")),
                campTag.getLong("expiresAt")
            ));
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag campsTag = new CompoundTag();
        camps.forEach((owner, camp) -> {
            CompoundTag campTag = new CompoundTag();
            campTag.putString("dimension", camp.dimension().toString());
            campTag.putInt("x", camp.pos().x);
            campTag.putInt("z", camp.pos().z);
            campTag.putLong("expiresAt", camp.expiresAt());
            campsTag.put(owner.toString(), campTag);
        });
        tag.put("camps", campsTag);
    }

    public Map<UUID, CampEntry> camps() {
        return this.camps;
    }

    public static CampSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), HandlerType.create(CampSaveData::new), "cadmus_camps");
    }
}
