package earth.terrarium.cadmus.common.compat.argonauts;

import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingState;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsHandler;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.cadmus.api.settings.SettingOverride;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.teams.TeamId;
import earth.terrarium.cadmus.client.CadmusClient;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.serverbound.RequestMemberSettingsPacket;
import earth.terrarium.cadmus.common.network.packets.serverbound.UpdateMemberSettingPacket;
import earth.terrarium.cadmus.common.settings.RoleSettingsResolver;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import java.util.UUID;

public final class CadmusMemberSettings {
    private static final ResourceLocation ARGONAUTS_TEAM = ResourceLocation.fromNamespaceAndPath("argonauts", "team");

    private CadmusMemberSettings() {
    }

    public static void register() {
        SettingDefinitions.forScope(SettingScope.TOWN).forEach((id, definition) -> {
            if (definition.target() != SettingTarget.PLAYER) return;
            MemberSettingsApi.API.register(new MemberSetting(
                id,
                Component.translatable("cadmus.setting." + id),
                Component.translatable("cadmus.setting." + id + ".description")
            ));
        });
        MemberSettingsApi.API.setHandler(new Handler());
        RoleSettingsResolver.set((level, team, player, setting) -> {
            if (!ARGONAUTS_TEAM.equals(team.provider())) return Optional.empty();
            return GuildApi.API.get(level, team.id())
                .map(guild -> guild.getPermission(player, setting) == TriState.TRUE);
        });
    }

    private static TeamId teamId(Team team) {
        return new TeamId(ARGONAUTS_TEAM, team.id());
    }

    private static final class Handler implements MemberSettingsHandler {
        @Override
        public MemberSettingState getState(Team team, UUID player, String setting) {
            TeamId id = teamId(team);
            String value = CadmusClient.MEMBER_SETTINGS.getOrDefault(new CadmusClient.MemberSettingKey(id, player), java.util.Map.of())
                .getOrDefault(setting, SettingOverride.INHERIT.name());
            return switch (value) {
                case "ALLOW" -> MemberSettingState.ALLOW;
                case "DENY" -> MemberSettingState.DENY;
                default -> MemberSettingState.INHERIT;
            };
        }

        @Override
        public void request(Team team, UUID player) {
            if (Minecraft.getInstance().level != null) {
                NetworkHandler.CHANNEL.sendToServer(new RequestMemberSettingsPacket(teamId(team), player));
            }
        }

        @Override
        public void setState(Team team, UUID player, String setting, MemberSettingState state) {
            NetworkHandler.CHANNEL.sendToServer(new UpdateMemberSettingPacket(teamId(team), player, setting, state.name()));
        }
    }
}
