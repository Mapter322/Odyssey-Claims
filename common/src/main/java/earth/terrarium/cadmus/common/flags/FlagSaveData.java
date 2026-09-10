package earth.terrarium.cadmus.common.flags;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import earth.terrarium.cadmus.api.flags.Flag;
import earth.terrarium.cadmus.api.flags.FlagApi;
import earth.terrarium.cadmus.common.utils.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FlagSaveData extends SaveHandler {

    private final Map<UUID, Map<String, Flag<?>>> flags = new HashMap<>();
    private final BiMap<UUID, String> idToNameMap = HashBiMap.create();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag names = tag.getCompound("names");
        names.getAllKeys().forEach(name ->
            this.idToNameMap.put(names.getUUID(name), name));

        CompoundTag adminTag = tag.getCompound("flags");
        adminTag.getAllKeys().forEach(id -> {
            CompoundTag flagsTag = adminTag.getCompound(id);
            Map<String, Flag<?>> flags = new HashMap<>();
            flagsTag.getAllKeys().forEach(flag -> {
                Flag<?> value = FlagApi.API.getDefaultValue(flag).deserialize(flagsTag);
                flags.put(flag, value);
            });
            this.flags.put(UUID.fromString(id), flags);
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag names = new CompoundTag();
        this.idToNameMap.forEach((id, name) -> names.putUUID(name, id));
        tag.put("names", names);

        CompoundTag adminTag = new CompoundTag();
        this.flags.forEach((id, flags) -> {
            CompoundTag flagsTag = new CompoundTag();
            flags.forEach((flagId, flag) -> flag.serialize(flagsTag));
            adminTag.put(id.toString(), flagsTag);
        });
        tag.put("flags", adminTag);
    }

    public static FlagSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), HandlerType.create(FlagSaveData::new), "cadmus_flags");
    }

    public static Optional<UUID> getIdFromName(MinecraftServer server, String name) {
        return Optional.ofNullable(read(server).idToNameMap.inverse().get(name));
    }

    public static boolean isAdminClaim(MinecraftServer server, UUID id) {
        return read(server).idToNameMap.containsKey(id);
    }

    public static UUID createAdminClaim(MinecraftServer server, String name) {
        var data = read(server);
        UUID id = ModUtils.stringToUUID(name);

        data.flags.put(id, new HashMap<>());
        data.idToNameMap.put(id, name);

        data.setDirty();
        return id;
    }

    public static void removeAdminClaim(MinecraftServer server, UUID id) {
        var data = read(server);
        data.flags.remove(id);
        data.idToNameMap.remove(id);
        data.setDirty();
    }

    public static Map<UUID, Map<String, Flag<?>>> getAll(MinecraftServer server) {
        return read(server).flags;
    }

    public static Collection<String> getAllAdminTeamNames(MinecraftServer server) {
        return read(server).idToNameMap.values();
    }

    @NotNull
    public static Flag<?> getFlag(MinecraftServer server, UUID id, String flagName) {
        var data = read(server);
        Flag<?> flag = data.flags.computeIfAbsent(id, k -> new HashMap<>()).get(flagName);
        return flag == null ? FlagApi.API.getDefaultValue(flagName) : flag;
    }

    public static void setFlag(MinecraftServer server, UUID id, String flagName, Flag<?> flag) {
        var data = read(server);
        data.flags.computeIfAbsent(id, ignored -> new HashMap<>()).put(flagName, flag);
        data.setDirty();
    }

    public static void removeFlag(MinecraftServer server, UUID id, String flagName) {
        var data = read(server);
        data.flags.computeIfAbsent(id, ignored -> new HashMap<>()).remove(flagName);
        data.setDirty();
    }

    public static void clearAll(MinecraftServer server) {
        read(server).flags.clear();
        read(server).idToNameMap.clear();
        read(server).setDirty();
    }
}
