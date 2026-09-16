package earth.terrarium.cadmus.common.settings;

import earth.terrarium.cadmus.api.settings.SettingAccess;
import earth.terrarium.cadmus.api.settings.SettingCondition;
import earth.terrarium.cadmus.api.settings.SettingDefinition;
import earth.terrarium.cadmus.api.settings.SettingScope;
import earth.terrarium.cadmus.api.settings.SettingTarget;
import earth.terrarium.cadmus.api.settings.types.BooleanSetting;
import earth.terrarium.cadmus.common.teams.WildernessTeamProvider;
import earth.terrarium.cadmus.common.utils.CadmusSaveData;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public final class WildernessTargets {

    private WildernessTargets() {
    }

    public static void registerAll(MinecraftServer server) {
        CadmusSaveData.getConditions(server, WildernessTeamProvider.team()).forEach(WildernessTargets::register);
    }

    public static boolean register(String condition) {
        String parent = TargetConditions.parentOf(condition);
        String key = TargetConditions.keyOf(condition);
        if (parent == null || key == null) return false;
        SettingCondition<?> target = TargetConditions.create(parent, key);
        if (target == null) return false;
        SettingDefinitions.register(new SettingDefinition<>(
            condition,
            SettingScope.WILDERNESS,
            SettingTarget.PLAYER,
            SettingAccess.ADMIN,
            new BooleanSetting(true),
            parent,
            List.of(target)
        ));
        return true;
    }

    public static void unregister(String condition) {
        SettingDefinitions.unregister(SettingScope.WILDERNESS, condition);
    }
}
