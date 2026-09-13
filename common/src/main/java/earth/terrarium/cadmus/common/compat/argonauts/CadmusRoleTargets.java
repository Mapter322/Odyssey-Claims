package earth.terrarium.cadmus.common.compat.argonauts;

import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.common.config.RoleDefaultsConfig;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.api.events.CadmusEvents;
import earth.terrarium.cadmus.api.settings.BlockTagCondition;
import earth.terrarium.cadmus.api.settings.BlockValueCondition;
import earth.terrarium.cadmus.api.settings.EntityTagCondition;
import earth.terrarium.cadmus.api.settings.EntityValueCondition;
import earth.terrarium.cadmus.api.settings.ItemTagCondition;
import earth.terrarium.cadmus.api.settings.ItemValueCondition;
import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.SyncMemberTargetsPacket;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CadmusRoleTargets {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cadmus.MOD_ID);
    private static final ResourceLocation ARGONAUTS_TEAM = ResourceLocation.fromNamespaceAndPath("argonauts", "team");
    private static final Map<String, TargetKind> KINDS = Map.of(
        "block-break", TargetKind.BLOCK,
        "block-place", TargetKind.BLOCK,
        "block-interactions", TargetKind.BLOCK,
        "entity-interactions", TargetKind.ENTITY,
        "entity-damage", TargetKind.ENTITY,
        "item-pickup", TargetKind.ITEM,
        "use", TargetKind.ITEM
    );

    private static final Set<String> CONFIG_CONDITIONS = new LinkedHashSet<>();
    private static final Set<String> GUILD_CONDITIONS = new LinkedHashSet<>();
    private static final Set<String> CLIENT_TARGETS = new LinkedHashSet<>();
    private static boolean eventsRegistered;

    private CadmusRoleTargets() {
    }

    public static void register() {
        RoleDefaultsConfig.ensureLoaded();
        for (RoleDefaultsConfig.RoleTarget target : RoleDefaultsConfig.targets()) {
            String id = target.parent() + "/" + target.key();
            if (!registerCondition(target.parent(), target.key())) continue;
            CONFIG_CONDITIONS.add(id);
            MemberSettingsApi.API.register(new MemberSetting(id, Component.literal(target.key()), Component.empty(), target.parent()));
        }
        registerEvents();
    }

    public static void registerGuilds(MinecraftServer server) {
        for (Guild guild : GuildApi.API.getAll(server.overworld())) {
            registerGuild(guild);
        }
    }

    public static void registerGuild(Guild guild) {
        for (String id : guild.getConditions()) {
            int index = id.indexOf('/');
            if (index <= 0) continue;
            if (registerCondition(id.substring(0, index), id.substring(index + 1))) {
                GUILD_CONDITIONS.add(id);
            }
        }
    }

    public static void prune(MinecraftServer server) {
        if (GUILD_CONDITIONS.isEmpty()) return;
        Set<String> used = new HashSet<>(CONFIG_CONDITIONS);
        for (Guild guild : GuildApi.API.getAll(server.overworld())) {
            used.addAll(guild.getConditions());
        }
        GUILD_CONDITIONS.removeIf(id -> {
            if (used.contains(id)) return false;
            SettingDefinitions.unregister(SettingScope.TOWN, id);
            return true;
        });
    }

    private static boolean registerCondition(String parent, String key) {
        TargetKind kind = KINDS.get(parent);
        if (kind == null) {
            LOGGER.warn("Skipping role condition '{}' with unknown parent '{}'", key, parent);
            return false;
        }
        SettingCondition<?> condition = condition(kind, key);
        if (condition == null) return false;
        String id = parent + "/" + key;
        SettingDefinitions.register(new SettingDefinition<>(id, SettingScope.TOWN, SettingTarget.PLAYER,
            SettingAccess.PLAYER, new BooleanSetting(true), parent, List.of(condition)));
        return true;
    }

    private static void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        CadmusEvents.TeamChangedEvent.register((server, teamId) -> {
            if (!ARGONAUTS_TEAM.equals(teamId.provider())) return;
            GuildApi.API.get(server.overworld(), teamId.id()).ifPresent(CadmusRoleTargets::registerGuild);
            prune(server);
        });
        CadmusEvents.RemoveTeamEvent.register((server, teamId) -> {
            if (!ARGONAUTS_TEAM.equals(teamId.provider())) return;
            prune(server);
        });
    }

    public static void sync(ServerPlayer player) {
        List<String> ids = RoleDefaultsConfig.targets().stream()
            .map(target -> target.parent() + "/" + target.key())
            .toList();
        if (NetworkHandler.CHANNEL.canSendToPlayer(player, SyncMemberTargetsPacket.TYPE)) {
            NetworkHandler.CHANNEL.sendToPlayer(new SyncMemberTargetsPacket(ids), player);
        }
    }

    public static void syncClient(List<String> ids) {
        clearClient();
        for (String id : ids) {
            int index = id.indexOf('/');
            if (index <= 0) continue;
            String parent = id.substring(0, index);
            String key = id.substring(index + 1);
            MemberSettingsApi.API.register(new MemberSetting(id, Component.literal(key), Component.empty(), parent));
            CLIENT_TARGETS.add(id);
        }
    }

    public static void clearClient() {
        for (String id : CLIENT_TARGETS) {
            MemberSettingsApi.API.unregister(id);
        }
        CLIENT_TARGETS.clear();
    }

    private static SettingCondition<?> condition(TargetKind kind, String key) {
        boolean tag = key.startsWith("#");
        ResourceLocation location = ResourceLocation.tryParse(tag ? key.substring(1) : key);
        if (location == null) {
            LOGGER.warn("Skipping invalid role condition '{}'", key);
            return null;
        }
        return switch (kind) {
            case BLOCK -> tag ? new BlockTagCondition(TagKey.create(Registries.BLOCK, location)) : new BlockValueCondition(location);
            case ITEM -> tag ? new ItemTagCondition(TagKey.create(Registries.ITEM, location)) : new ItemValueCondition(location);
            case ENTITY -> tag ? new EntityTagCondition(TagKey.create(Registries.ENTITY_TYPE, location)) : new EntityValueCondition(location);
        };
    }

    private enum TargetKind {
        BLOCK, ITEM, ENTITY
    }
}
