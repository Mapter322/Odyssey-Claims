package earth.terrarium.cadmus.common.protections.types.neoforge;

import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import earth.terrarium.cadmus.common.protections.Protections;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = Cadmus.MOD_ID)
final class EntityDamageProtectionImpl {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onAttackEntity(AttackEntityEvent event) {
        if (!Protections.ENTITY_DAMAGE.canDamageEntity(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
        LightningBolt lightning = event.getLightning();
        if ((lightning.getCause() != null &&
            !Protections.ENTITY_DAMAGE.canDamageEntity(lightning.getCause(), lightning.getCause())) ||
            !Protections.MOB_GRIEFING.canMobGrief(lightning)) {
            event.setCanceled(true);
        }

        event.setCanceled(!lightning.level().isClientSide() &&
            !Settings.isEnabledAt(lightning.level(), lightning.chunkPosition(), SettingDefinitions.LIGHTNING));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().getOwner() instanceof Player player &&
            !Protections.ENTITY_DAMAGE.canDamageEntity(player, event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
