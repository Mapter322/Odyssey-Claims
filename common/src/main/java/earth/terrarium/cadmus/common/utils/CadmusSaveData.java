package earth.terrarium.cadmus.common.utils;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.cadmus.api.settings.ChunkRef;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.settings.types.ColorSetting;
import earth.terrarium.cadmus.api.settings.types.FloatSetting;
import earth.terrarium.cadmus.api.settings.types.StringSetting;
import earth.terrarium.cadmus.common.towns.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class CadmusSaveData extends SaveHandler {

    private final Map<SettingScope, Map<TeamId, Map<String, SettingValue<?>>>> settingValues = new EnumMap<>(SettingScope.class);
    private final Map<SettingScope, Map<UUID, Map<String, SettingValue<?>>>> townSettingValues = new EnumMap<>(SettingScope.class);
    private final Map<SettingScope, Map<ChunkRef, Map<String, SettingValue<?>>>> chunkSettingValues = new EnumMap<>(SettingScope.class);
    private final Map<ChunkRef, String> chunkNames = new HashMap<>();
    private final Map<SettingScope, Map<TeamId, Map<UUID, Map<String, SettingOverride>>>> playerSettingOverrides = new EnumMap<>(SettingScope.class);
    private final Map<UUID, String> adminTeams = new HashMap<>();
    private final Set<UUID> bypassPlayers = new HashSet<>();
    private final Map<TeamId, Color> teamColors = new HashMap<>();
    private final Set<UUID> uniquePlayers = new HashSet<>();
    private final Map<UUID, Town> towns = new HashMap<>();
    private final Map<TeamId, Set<String>> teamConditions = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        ListTag bypassTag = tag.getList("bypass", Tag.TAG_STRING);
        bypassTag.forEach(tagEntry -> bypassPlayers.add(UUID.fromString(tagEntry.getAsString())));

        CompoundTag teamColorsTag = tag.getCompound("teamColors");
        teamColorsTag.getAllKeys().forEach(provider -> {
            CompoundTag providerMap = teamColorsTag.getCompound(provider);
            providerMap.getAllKeys().forEach(id -> {
                teamColors.put(new TeamId(ResourceLocation.parse(provider), UUID.fromString(id)), Color.parse(providerMap.getString(provider)));
            });
        });

        ListTag uniquePlayersTag = tag.getList("uniquePlayers", Tag.TAG_STRING);
        uniquePlayersTag.forEach(uuid -> uniquePlayers.add(UUID.fromString(uuid.getAsString())));

        CompoundTag townsTag = tag.getCompound("towns");
        townsTag.getAllKeys().forEach(idString -> {
            CompoundTag townTag = townsTag.getCompound(idString);
            TeamId team = new TeamId(ResourceLocation.parse(townTag.getString("provider")), UUID.fromString(townTag.getString("team")));
            Set<ChunkPos> chunks = new HashSet<>();
            townTag.getList("chunks", Tag.TAG_LONG).forEach(chunk -> {
                long value = ((LongTag) chunk).getAsLong();
                chunks.add(new ChunkPos(BlockPos.getX(value), BlockPos.getZ(value)));
            });
UUID id = UUID.fromString(idString);
            towns.put(id, new Town(id, team, townTag.getString("name"), chunks));
        });

        CompoundTag adminTeamsTag = tag.getCompound("adminTeams");
        adminTeamsTag.getAllKeys().forEach(name ->
            adminTeams.put(UUID.fromString(adminTeamsTag.getString(name)), name));

        CompoundTag conditionsTag = tag.getCompound("teamConditions");
        conditionsTag.getAllKeys().forEach(teamName -> {
            Set<String> conditions = new HashSet<>();
            conditionsTag.getCompound(teamName).getAllKeys().forEach(conditions::add);
            if (!conditions.isEmpty()) teamConditions.put(parseTeamId(teamName), conditions);
        });

        loadSettingValues(tag.getCompound("settingValues"));
        loadTownSettingValues(tag.getCompound("townSettings"));
        loadChunkSettingValues(tag.getCompound("chunkSettings"));
        loadChunkNames(tag.getCompound("chunkNames"));
        loadPlayerSettingOverrides(tag.getCompound("playerSettingOverrides"));
    }

    @Override
    public void saveData(CompoundTag tag) {
        ListTag bypassTag = new ListTag();
        bypassPlayers.forEach(uuid -> bypassTag.add(StringTag.valueOf(uuid.toString())));
        tag.put("bypass", bypassTag);

        CompoundTag teamColorsTag = new CompoundTag();
        teamColors.forEach((uuid, color) -> teamColorsTag.putString(uuid.toString(), color.toString()));
        tag.put("teamColors", teamColorsTag);

        ListTag uniquePlayersTag = new ListTag();
        uniquePlayers.forEach(uuid -> uniquePlayersTag.add(StringTag.valueOf(uuid.toString())));
        tag.put("uniquePlayers", uniquePlayersTag);

        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            townTag.putString("provider", town.team().provider().toString());
            townTag.putString("team", town.team().id().toString());
            townTag.putString("name", town.name());
            ListTag chunks = new ListTag();
            town.chunks().forEach(pos -> chunks.add(LongTag.valueOf(BlockPos.asLong(pos.x, 0, pos.z))));
            townTag.put("chunks", chunks);
            townsTag.put(id.toString(), townTag);
        });
tag.put("towns", townsTag);

        CompoundTag adminTeamsTag = new CompoundTag();
        adminTeams.forEach((id, name) -> adminTeamsTag.putString(name, id.toString()));
        tag.put("adminTeams", adminTeamsTag);

        CompoundTag conditionsTag = new CompoundTag();
        teamConditions.forEach((team, conditions) -> {
            CompoundTag teamTag = new CompoundTag();
            conditions.forEach(condition -> teamTag.putBoolean(condition, true));
            conditionsTag.put(teamKey(team), teamTag);
        });
        tag.put("teamConditions", conditionsTag);

        tag.put("settingValues", saveSettingValues());
        tag.put("townSettings", saveTownSettingValues());
        tag.put("chunkSettings", saveChunkSettingValues());
        tag.put("chunkNames", saveChunkNames());
        tag.put("playerSettingOverrides", savePlayerSettingOverrides());
    }

public static CadmusSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), SaveHandler.HandlerType.create(CadmusSaveData::new), "cadmus_data");
    }

    public static boolean isAdminClaim(MinecraftServer server, UUID id) {
        return read(server).adminTeams.containsKey(id);
    }

    public static UUID createAdminClaim(MinecraftServer server, String name) {
        var data = read(server);
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        data.adminTeams.put(id, name);
        data.setDirty();
        return id;
    }

    public static void removeAdminClaim(MinecraftServer server, UUID id) {
        var data = read(server);
        data.adminTeams.remove(id);
        data.setDirty();
    }

    public static Optional<UUID> getIdFromName(MinecraftServer server, String name) {
        return read(server).adminTeams.entrySet().stream()
            .filter(entry -> entry.getValue().equals(name))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public static Collection<String> getAllAdminTeamNames(MinecraftServer server) {
        return read(server).adminTeams.values();
    }

    public static Set<String> getConditions(MinecraftServer server, TeamId id) {
        return Set.copyOf(read(server).teamConditions.getOrDefault(id, Set.of()));
    }

    public static Set<String> getAllConditions(MinecraftServer server) {
        Set<String> result = new HashSet<>();
        read(server).teamConditions.values().forEach(result::addAll);
        return result;
    }

    public static void addCondition(MinecraftServer server, TeamId id, String condition) {
        var data = read(server);
        data.teamConditions.computeIfAbsent(id, ignored -> new HashSet<>()).add(condition);
        data.setDirty();
    }

    public static void removeCondition(MinecraftServer server, TeamId id, String condition) {
        var data = read(server);
        Set<String> conditions = data.teamConditions.get(id);
        if (conditions == null) return;
        conditions.remove(condition);
        if (conditions.isEmpty()) data.teamConditions.remove(id);
        data.setDirty();
    }

    public static boolean canBypass(MinecraftServer server, UUID player) {
        return read(server).bypassPlayers.contains(player);
    }

    public static boolean canBypass(ServerPlayer player) {
        return canBypass(player.getServer(), player.getUUID());
    }

    public static void toggleBypass(MinecraftServer server, UUID player) {
        var data = read(server);
        if (data.bypassPlayers.contains(player)) {
            data.bypassPlayers.remove(player);
        } else {
            data.bypassPlayers.add(player);
        }
        data.setDirty();
    }

    @SuppressWarnings("unchecked")
    public static <T> SettingValue<T> getSettingValue(MinecraftServer server, TeamId id, SettingDefinition<T> definition) {
        var data = read(server);
        return data.settingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(id, Map.of())
            .getOrDefault(definition.id(), definition.defaultValue()) instanceof SettingValue<?> value
            ? (SettingValue<T>) value
            : definition.defaultValue();
    }

    public static boolean hasSettingValue(MinecraftServer server, TeamId id, SettingDefinition<?> definition) {
        return read(server).settingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(id, Map.of())
            .containsKey(definition.id());
    }

    @SuppressWarnings("unchecked")
    public static <T> SettingValue<T> getTownSettingValue(MinecraftServer server, UUID town, SettingDefinition<T> definition) {
        var data = read(server);
        return data.townSettingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(town, Map.of())
            .getOrDefault(definition.id(), definition.defaultValue()) instanceof SettingValue<?> value
            ? (SettingValue<T>) value
            : definition.defaultValue();
    }

    public static boolean hasTownSettingValue(MinecraftServer server, UUID town, SettingDefinition<?> definition) {
        return read(server).townSettingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(town, Map.of())
            .containsKey(definition.id());
    }

    public static <T> void setTownSettingValue(MinecraftServer server, UUID town, SettingDefinition<T> definition, SettingValue<T> value) {
        var data = read(server);
        data.townSettingValues
            .computeIfAbsent(definition.scope(), ignored -> new HashMap<>())
            .computeIfAbsent(town, ignored -> new HashMap<>())
            .put(definition.id(), value);
        data.setDirty();
    }

    public static void resetTownSettingValue(MinecraftServer server, UUID town, SettingDefinition<?> definition) {
        var data = read(server);
        Map<UUID, Map<String, SettingValue<?>>> scopeValues = data.townSettingValues.get(definition.scope());
        if (scopeValues != null) {
            scopeValues.computeIfPresent(town, (ignored, values) -> {
                values.remove(definition.id());
                return values.isEmpty() ? null : values;
            });
        }
        data.setDirty();
    }

    public static void removeTownSettings(MinecraftServer server, UUID town) {
        var data = read(server);
        data.townSettingValues.values().forEach(values -> values.remove(town));
        data.setDirty();
    }

    @SuppressWarnings("unchecked")
    public static <T> SettingValue<T> getChunkSettingValue(MinecraftServer server, ChunkRef chunk, SettingDefinition<T> definition) {
        var data = read(server);
        return data.chunkSettingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(chunk, Map.of())
            .getOrDefault(definition.id(), definition.defaultValue()) instanceof SettingValue<?> value
            ? (SettingValue<T>) value
            : definition.defaultValue();
    }

    public static boolean hasChunkSettingValue(MinecraftServer server, ChunkRef chunk, SettingDefinition<?> definition) {
        return read(server).chunkSettingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(chunk, Map.of())
            .containsKey(definition.id());
    }

    public static <T> void setChunkSettingValue(MinecraftServer server, ChunkRef chunk, SettingDefinition<T> definition, SettingValue<T> value) {
        var data = read(server);
        data.chunkSettingValues
            .computeIfAbsent(definition.scope(), ignored -> new HashMap<>())
            .computeIfAbsent(chunk, ignored -> new HashMap<>())
            .put(definition.id(), value);
        data.setDirty();
    }

    public static void resetChunkSettingValue(MinecraftServer server, ChunkRef chunk, SettingDefinition<?> definition) {
        var data = read(server);
        Map<ChunkRef, Map<String, SettingValue<?>>> scopeValues = data.chunkSettingValues.get(definition.scope());
        if (scopeValues != null) {
            scopeValues.computeIfPresent(chunk, (ignored, values) -> {
                values.remove(definition.id());
                return values.isEmpty() ? null : values;
            });
        }
        data.setDirty();
    }

    public static Optional<String> getChunkName(MinecraftServer server, ChunkRef chunk) {
        return Optional.ofNullable(read(server).chunkNames.get(chunk));
    }

    public static void setChunkName(MinecraftServer server, ChunkRef chunk, String name) {
        var data = read(server);
        if (name.isBlank()) {
            data.chunkNames.remove(chunk);
        } else {
            data.chunkNames.put(chunk, name);
        }
        data.setDirty();
    }

    public static boolean removeChunkData(MinecraftServer server, ChunkRef chunk) {
        var data = read(server);
        boolean removed = data.chunkNames.remove(chunk) != null;
        for (Map<ChunkRef, Map<String, SettingValue<?>>> values : data.chunkSettingValues.values()) {
            removed |= values.remove(chunk) != null;
        }
        data.setDirty();
        return removed;
    }

    public static <T> void setSettingValue(MinecraftServer server, TeamId id, SettingDefinition<T> definition, SettingValue<T> value) {
        var data = read(server);
        data.settingValues
            .computeIfAbsent(definition.scope(), ignored -> new HashMap<>())
            .computeIfAbsent(id, ignored -> new HashMap<>())
            .put(definition.id(), value);
        data.setDirty();
    }

    public static void resetSettingValue(MinecraftServer server, TeamId id, SettingDefinition<?> definition) {
        var data = read(server);
        Map<TeamId, Map<String, SettingValue<?>>> scopeValues = data.settingValues.get(definition.scope());
        if (scopeValues != null) {
            scopeValues.computeIfPresent(id, (ignored, values) -> {
                values.remove(definition.id());
                return values.isEmpty() ? null : values;
            });
        }
        data.setDirty();
    }

    public static SettingOverride getPlayerSettingOverride(MinecraftServer server, TeamId team, UUID player, SettingDefinition<?> definition) {
        return read(server).playerSettingOverrides
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(team, Map.of())
            .getOrDefault(player, Map.of())
            .getOrDefault(definition.id(), SettingOverride.INHERIT);
    }

    public static void setPlayerSettingOverride(MinecraftServer server, TeamId team, UUID player, SettingDefinition<?> definition, SettingOverride override) {
        var data = read(server);
        var values = data.playerSettingOverrides
            .computeIfAbsent(definition.scope(), ignored -> new HashMap<>())
            .computeIfAbsent(team, ignored -> new HashMap<>())
            .computeIfAbsent(player, ignored -> new HashMap<>());
        if (override == SettingOverride.INHERIT) values.remove(definition.id());
        else values.put(definition.id(), override);
        if (values.isEmpty()) data.playerSettingOverrides.get(definition.scope()).get(team).remove(player);
        if (data.playerSettingOverrides.get(definition.scope()).get(team).isEmpty()) data.playerSettingOverrides.get(definition.scope()).remove(team);
        data.setDirty();
    }

    public static void removeTeam(MinecraftServer server, TeamId id) {
        var data = read(server);
        data.settingValues.values().forEach(values -> values.remove(id));
        data.playerSettingOverrides.values().forEach(values -> values.remove(id));
        data.teamConditions.remove(id);
        data.setDirty();
    }

    public static void clearAll(MinecraftServer server) {
        var data = read(server);
        data.settingValues.clear();
        data.townSettingValues.clear();
        data.chunkSettingValues.clear();
        data.chunkNames.clear();
        data.playerSettingOverrides.clear();
        data.adminTeams.clear();
        data.teamConditions.clear();
        data.setDirty();
    }

    public static void setTeamColor(MinecraftServer server, TeamId id, Color color) {
        var data = read(server);
        data.teamColors.put(id, color);
        data.setDirty();
    }

    public static Color getTeamColor(MinecraftServer server, TeamId id) {
        Map<TeamId, Color> colors = read(server).teamColors;
        colors.putIfAbsent(id, ModUtils.uuidToColor(id.id()));
        return colors.get(id);
    }

    public static void addUniquePlayer(ServerPlayer player) {
        var data = read(Objects.requireNonNull(player.getServer()));
        data.uniquePlayers.add(player.getUUID());
        data.setDirty();
    }

    public static Set<UUID> getUniquePlayers(MinecraftServer server) {
        return read(server).uniquePlayers;
    }

    public Map<UUID, Town> towns() {
        return towns;
    }

    public Map<ChunkRef, String> chunkNames() {
        return chunkNames;
    }

    private void loadSettingValues(CompoundTag root) {
        root.getAllKeys().forEach(scopeName -> {
            SettingScope scope = SettingScope.valueOf(scopeName);
            CompoundTag scopeTag = root.getCompound(scopeName);
            scopeTag.getAllKeys().forEach(teamName -> {
                CompoundTag teamTag = scopeTag.getCompound(teamName);
                TeamId team = parseTeamId(teamName);
                teamTag.getAllKeys().forEach(id -> {
                    SettingValue<?> value = readSettingValue(teamTag.getCompound(id));
                    if (value != null) {
                        settingValues.computeIfAbsent(scope, ignored -> new HashMap<>())
                            .computeIfAbsent(team, ignored -> new HashMap<>())
                            .put(id, value);
                    }
                });
            });
        });
    }

    private void loadTownSettingValues(CompoundTag root) {
        root.getAllKeys().forEach(scopeName -> {
            SettingScope scope = SettingScope.valueOf(scopeName);
            CompoundTag scopeTag = root.getCompound(scopeName);
            scopeTag.getAllKeys().forEach(townName -> {
                CompoundTag townTag = scopeTag.getCompound(townName);
                UUID town = UUID.fromString(townName);
                townTag.getAllKeys().forEach(id -> {
                    SettingValue<?> value = readSettingValue(townTag.getCompound(id));
                    if (value != null) {
                        townSettingValues.computeIfAbsent(scope, ignored -> new HashMap<>())
                            .computeIfAbsent(town, ignored -> new HashMap<>())
                            .put(id, value);
                    }
                });
            });
        });
    }

    private void loadPlayerSettingOverrides(CompoundTag root) {
        root.getAllKeys().forEach(scopeName -> {
            SettingScope scope = SettingScope.valueOf(scopeName);
            CompoundTag scopeTag = root.getCompound(scopeName);
            scopeTag.getAllKeys().forEach(teamName -> {
                TeamId team = parseTeamId(teamName);
                CompoundTag teamTag = scopeTag.getCompound(teamName);
                teamTag.getAllKeys().forEach(playerName -> {
                    UUID player = UUID.fromString(playerName);
                    CompoundTag playerTag = teamTag.getCompound(playerName);
                    playerTag.getAllKeys().forEach(id -> {
                        try {
                            playerSettingOverrides.computeIfAbsent(scope, ignored -> new HashMap<>())
                                .computeIfAbsent(team, ignored -> new HashMap<>())
                                .computeIfAbsent(player, ignored -> new HashMap<>())
                                .put(id, SettingOverride.valueOf(playerTag.getString(id)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
                });
            });
        });
    }

    private CompoundTag saveSettingValues() {
        CompoundTag root = new CompoundTag();
        settingValues.forEach((scope, teams) -> {
            CompoundTag scopeTag = new CompoundTag();
            teams.forEach((team, values) -> {
                CompoundTag teamTag = new CompoundTag();
                values.forEach((id, value) -> teamTag.put(id, writeSettingValue(value)));
                scopeTag.put(teamKey(team), teamTag);
            });
            root.put(scope.name(), scopeTag);
        });
        return root;
    }

    private CompoundTag saveTownSettingValues() {
        CompoundTag root = new CompoundTag();
        townSettingValues.forEach((scope, towns) -> {
            CompoundTag scopeTag = new CompoundTag();
            towns.forEach((town, values) -> {
                CompoundTag townTag = new CompoundTag();
                values.forEach((id, value) -> townTag.put(id, writeSettingValue(value)));
                scopeTag.put(town.toString(), townTag);
            });
            root.put(scope.name(), scopeTag);
        });
        return root;
    }

    private void loadChunkSettingValues(CompoundTag root) {
        root.getAllKeys().forEach(scopeName -> {
            SettingScope scope = SettingScope.valueOf(scopeName);
            CompoundTag scopeTag = root.getCompound(scopeName);
            scopeTag.getAllKeys().forEach(dimensionName -> {
                ResourceLocation dimension = ResourceLocation.tryParse(dimensionName);
                if (dimension == null) return;
                CompoundTag dimensionTag = scopeTag.getCompound(dimensionName);
                dimensionTag.getAllKeys().forEach(chunkKey -> {
                    ChunkPos pos = parseChunkKey(chunkKey);
                    if (pos == null) return;
                    ChunkRef chunk = new ChunkRef(dimension, pos);
                    CompoundTag chunkTag = dimensionTag.getCompound(chunkKey);
                    chunkTag.getAllKeys().forEach(id -> {
                        SettingValue<?> value = readSettingValue(chunkTag.getCompound(id));
                        if (value != null) {
                            chunkSettingValues.computeIfAbsent(scope, ignored -> new HashMap<>())
                                .computeIfAbsent(chunk, ignored -> new HashMap<>())
                                .put(id, value);
                        }
                    });
                });
            });
        });
    }

    private void loadChunkNames(CompoundTag root) {
        root.getAllKeys().forEach(dimensionName -> {
            ResourceLocation dimension = ResourceLocation.tryParse(dimensionName);
            if (dimension == null) return;
            CompoundTag dimensionTag = root.getCompound(dimensionName);
            dimensionTag.getAllKeys().forEach(chunkKey -> {
                ChunkPos pos = parseChunkKey(chunkKey);
                if (pos == null) return;
                chunkNames.put(new ChunkRef(dimension, pos), dimensionTag.getString(chunkKey));
            });
        });
    }

    private CompoundTag saveChunkSettingValues() {
        CompoundTag root = new CompoundTag();
        chunkSettingValues.forEach((scope, chunks) -> {
            CompoundTag scopeTag = new CompoundTag();
            chunks.forEach((chunk, values) -> {
                String dimension = chunk.dimension().toString();
                CompoundTag dimensionTag = scopeTag.getCompound(dimension);
                CompoundTag chunkTag = new CompoundTag();
                values.forEach((id, value) -> chunkTag.put(id, writeSettingValue(value)));
                dimensionTag.put(chunk.key(), chunkTag);
                scopeTag.put(dimension, dimensionTag);
            });
            root.put(scope.name(), scopeTag);
        });
        return root;
    }

    private CompoundTag saveChunkNames() {
        CompoundTag root = new CompoundTag();
        chunkNames.forEach((chunk, name) -> {
            String dimension = chunk.dimension().toString();
            CompoundTag dimensionTag = root.getCompound(dimension);
            dimensionTag.putString(chunk.key(), name);
            root.put(dimension, dimensionTag);
        });
        return root;
    }

    @Nullable
    private static ChunkPos parseChunkKey(String key) {
        int separator = key.indexOf(',');
        if (separator < 0) return null;
        try {
            return new ChunkPos(Integer.parseInt(key.substring(0, separator)), Integer.parseInt(key.substring(separator + 1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private CompoundTag savePlayerSettingOverrides() {
        CompoundTag root = new CompoundTag();
        playerSettingOverrides.forEach((scope, teams) -> {
            CompoundTag scopeTag = new CompoundTag();
            teams.forEach((team, players) -> {
                CompoundTag teamTag = new CompoundTag();
                players.forEach((player, values) -> {
                    CompoundTag playerTag = new CompoundTag();
                    values.forEach((id, value) -> playerTag.putString(id, value.name()));
                    teamTag.put(player.toString(), playerTag);
                });
                scopeTag.put(teamKey(team), teamTag);
            });
            root.put(scope.name(), scopeTag);
        });
        return root;
    }

    private static CompoundTag writeSettingValue(SettingValue<?> value) {
        CompoundTag tag = new CompoundTag();
        if (value instanceof BooleanSetting setting) {
            tag.putString("type", "boolean");
            tag.putBoolean("value", setting.value());
        } else if (value instanceof StringSetting setting) {
            tag.putString("type", "string");
            tag.putString("value", setting.value());
        } else if (value instanceof FloatSetting setting) {
            tag.putString("type", "float");
            tag.putFloat("value", setting.value());
        } else if (value instanceof ColorSetting setting) {
            tag.putString("type", "color");
            Color.CODEC.encodeStart(NbtOps.INSTANCE, setting.value()).result()
                .ifPresent(encoded -> tag.put("value", encoded));
        } else {
            throw new IllegalArgumentException("Unsupported setting value: " + value.getClass().getName());
        }
        return tag;
    }

    private static SettingValue<?> readSettingValue(CompoundTag tag) {
        return switch (tag.getString("type")) {
            case "boolean" -> new BooleanSetting(tag.getBoolean("value"));
            case "string" -> new StringSetting(tag.getString("value"));
            case "float" -> new FloatSetting(tag.getFloat("value"));
            case "color" -> Color.CODEC.parse(NbtOps.INSTANCE, tag.get("value"))
                .result().map(ColorSetting::new).orElse(null);
            default -> null;
        };
    }

    private static TeamId parseTeamId(String value) {
        int separator = value.indexOf('|');
        if (separator < 0) throw new IllegalArgumentException("Invalid team id: " + value);
        return new TeamId(ResourceLocation.parse(value.substring(0, separator)), UUID.fromString(value.substring(separator + 1)));
    }

    private static String teamKey(TeamId team) {
        return team.provider() + "|" + team.id();
    }
}
