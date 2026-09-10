package earth.terrarium.cadmus.common.utils;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingValue;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.api.settings.types.ColorSetting;
import earth.terrarium.cadmus.api.settings.types.FloatSetting;
import earth.terrarium.cadmus.api.settings.types.StringSetting;
import earth.terrarium.cadmus.common.towns.Town;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class CadmusSaveData extends SaveHandler {

    private final Map<TeamId, Map<String, TriState>> settings = new HashMap<>();
    private final Object2BooleanMap<String> defaultSettings = new Object2BooleanArrayMap<>();
    private final Map<SettingScope, Map<TeamId, Map<String, SettingValue<?>>>> settingValues = new EnumMap<>(SettingScope.class);
    private final Map<SettingScope, Map<String, SettingValue<?>>> settingDefaults = new EnumMap<>(SettingScope.class);
    private final Map<TeamId, Set<ResourceLocation>> allowedBlocks = new HashMap<>();
    private final Set<UUID> bypassPlayers = new HashSet<>();
    private final Map<TeamId, Color> teamColors = new HashMap<>();
    private final Set<UUID> uniquePlayers = new HashSet<>();
    private final Map<UUID, Town> towns = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag settingsTag = tag.getCompound("settings");
        settingsTag.getAllKeys().forEach(provider -> {
            CompoundTag providerMap = settingsTag.getCompound(provider);
            providerMap.getAllKeys().forEach(id -> {
                CompoundTag claimSettingsTag = providerMap.getCompound(id);
                claimSettingsTag.getAllKeys().forEach(setting -> {
                    TriState value = TriState.valueOf(claimSettingsTag.getString(setting));
                    this.settings.computeIfAbsent(new TeamId(ResourceLocation.parse(provider), UUID.fromString(id)), ignored -> new HashMap<>()).put(setting, value);
                });
            });
        });

        CompoundTag defaultSettingsTag = tag.getCompound("defaultSettings");
        defaultSettingsTag.getAllKeys().forEach(setting ->
            defaultSettings.put(setting, defaultSettingsTag.getBoolean(setting)));

        CompoundTag allowedBlocksTag = tag.getCompound("allowedBlocks");
        allowedBlocksTag.getAllKeys().forEach(provider -> {
            CompoundTag providerMap = allowedBlocksTag.getCompound(provider);
            providerMap.getAllKeys().forEach(id -> {
                ListTag blockTag = providerMap.getList(provider, Tag.TAG_STRING);
                Set<ResourceLocation> blocks = new HashSet<>();
                blockTag.forEach(tagEntry ->
                    blocks.add(ResourceLocation.parse(tagEntry.getAsString())));
                allowedBlocks.put(new TeamId(ResourceLocation.parse(provider), UUID.fromString(id)), blocks);
            });
        });

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

        loadSettingValues(tag.getCompound("settingValues"));
        loadSettingDefaults(tag.getCompound("settingDefaults"));
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag settingsTag = new CompoundTag();
        this.settings.forEach((id, claimSettings) -> {
            CompoundTag claimSettingsTag = new CompoundTag();
            claimSettings.forEach((setting, value) -> claimSettingsTag.putString(setting, value.name()));
            settingsTag.put(id.toString(), claimSettingsTag);
        });
        tag.put("settings", settingsTag);

        CompoundTag defaultSettingsTag = new CompoundTag();
        this.defaultSettings.forEach(defaultSettingsTag::putBoolean);
        tag.put("defaultSettings", defaultSettingsTag);

        CompoundTag allowedBlocksTag = new CompoundTag();
        this.allowedBlocks.forEach((id, blocks) -> {
            ListTag blockTag = new ListTag();
            blocks.forEach(block -> blockTag.add(StringTag.valueOf(block.toString())));
            allowedBlocksTag.put(id.toString(), blockTag);
        });
        tag.put("allowedBlocks", allowedBlocksTag);

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

        tag.put("settingValues", saveSettingValues());
        tag.put("settingDefaults", saveSettingDefaults());
    }

    public static CadmusSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), SaveHandler.HandlerType.create(CadmusSaveData::new), "cadmus_data");
    }

    public static TriState getClaimSetting(MinecraftServer server, TeamId id, String setting) {
        return read(server).settings
            .computeIfAbsent(id, ignored -> new HashMap<>())
            .getOrDefault(setting, TriState.UNDEFINED);
    }

    public static void setClaimSetting(MinecraftServer server, TeamId id, String setting, TriState value) {
        var data = read(server);
        data.settings
            .computeIfAbsent(id, ignored -> new HashMap<>())
            .put(setting, value);
        data.setDirty();
    }

    public static boolean getClaimSettingOrDefault(MinecraftServer server, TeamId id, String setting) {
        TriState value = getClaimSetting(server, id, setting);
        return value.isUndefined() ? getDefaultClaimSetting(server, setting) : value.isTrue();
    }

    public static boolean getDefaultClaimSetting(MinecraftServer server, String setting) {
        return read(server).defaultSettings.getBoolean(setting);
    }

    public static void setDefaultClaimSetting(MinecraftServer server, String setting, boolean value) {
        var data = read(server);
        data.defaultSettings.put(setting, value);
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
        SettingValue<?> defaultValue = data.settingDefaults
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(definition.id(), definition.defaultValue());
        return data.settingValues
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(id, Map.of())
            .getOrDefault(definition.id(), defaultValue) instanceof SettingValue<?> value
            ? (SettingValue<T>) value
            : definition.defaultValue();
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

    @SuppressWarnings("unchecked")
    public static <T> SettingValue<T> getDefaultSettingValue(MinecraftServer server, SettingDefinition<T> definition) {
        return read(server).settingDefaults
            .getOrDefault(definition.scope(), Map.of())
            .getOrDefault(definition.id(), definition.defaultValue()) instanceof SettingValue<?> value
            ? (SettingValue<T>) value
            : definition.defaultValue();
    }

    public static <T> void setDefaultSettingValue(MinecraftServer server, SettingDefinition<T> definition, SettingValue<T> value) {
        var data = read(server);
        data.settingDefaults
            .computeIfAbsent(definition.scope(), ignored -> new HashMap<>())
            .put(definition.id(), value);
        data.setDirty();
    }

    public static void resetDefaultSettingValue(MinecraftServer server, SettingDefinition<?> definition) {
        var data = read(server);
        Map<String, SettingValue<?>> scopeDefaults = data.settingDefaults.get(definition.scope());
        if (scopeDefaults != null) scopeDefaults.remove(definition.id());
        data.setDirty();
    }

    public static void addAllowedBlock(MinecraftServer server, TeamId player, Block block) {
        var data = read(server);
        data.allowedBlocks.computeIfAbsent(player, ignored -> new HashSet<>()).add(BuiltInRegistries.BLOCK.getKey(block));
        data.setDirty();
    }

    public static void removeAllowedBlock(MinecraftServer server, TeamId player, Block block) {
        var data = read(server);
        data.allowedBlocks.computeIfAbsent(player, ignored -> new HashSet<>()).remove(BuiltInRegistries.BLOCK.getKey(block));
        data.setDirty();
    }

    public static boolean isBlockAllowed(MinecraftServer server, TeamId player, Block block) {
        var data = read(server);
        return data.allowedBlocks.computeIfAbsent(player, ignored -> new HashSet<>()).contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static Set<ResourceLocation> getAllowedBlocks(MinecraftServer server, TeamId player) {
        return read(server).allowedBlocks.computeIfAbsent(player, ignored -> new HashSet<>());
    }

    public static void removeTeam(MinecraftServer server, TeamId id) {
        var data = read(server);
        data.settings.remove(id);
        data.allowedBlocks.remove(id);
        data.setDirty();
    }

    public static void clearAll(MinecraftServer server) {
        var data = read(server);
        data.settings.clear();
        data.allowedBlocks.clear();
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

    private void loadSettingDefaults(CompoundTag root) {
        root.getAllKeys().forEach(scopeName -> {
            SettingScope scope = SettingScope.valueOf(scopeName);
            CompoundTag scopeTag = root.getCompound(scopeName);
            scopeTag.getAllKeys().forEach(id -> {
                SettingValue<?> value = readSettingValue(scopeTag.getCompound(id));
                if (value != null) settingDefaults.computeIfAbsent(scope, ignored -> new HashMap<>()).put(id, value);
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

    private CompoundTag saveSettingDefaults() {
        CompoundTag root = new CompoundTag();
        settingDefaults.forEach((scope, values) -> {
            CompoundTag scopeTag = new CompoundTag();
            values.forEach((id, value) -> scopeTag.put(id, writeSettingValue(value)));
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
