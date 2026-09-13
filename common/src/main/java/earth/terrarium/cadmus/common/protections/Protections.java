package earth.terrarium.cadmus.common.protections;

import earth.terrarium.cadmus.api.protections.Protection;
import earth.terrarium.cadmus.api.protections.ProtectionApi;
import earth.terrarium.cadmus.common.protections.types.*;

public class Protections {

    public static final BlockBreakProtection BLOCK_BREAKING = register(new BlockBreakProtection());
    public static final BlockPlaceProtection BLOCK_PLACING = register(new BlockPlaceProtection());
    public static final BlockInteractProtection BLOCK_INTERACTIONS = register(new BlockInteractProtection());
    public static final BlockExplosionProtection BLOCK_EXPLOSIONS = register(new BlockExplosionProtection());
    public static final EntityExplosionProtection ENTITY_EXPLOSIONS = register(new EntityExplosionProtection());
    public static final EntityInteractProtection ENTITY_INTERACTIONS = register(new EntityInteractProtection());
    public static final EntityDamageProtection ENTITY_DAMAGE = register(new EntityDamageProtection());
    public static final MobGriefingProtection MOB_GRIEFING = register(new MobGriefingProtection());
    public static final ItemPickupProtection ITEM_PICKUP = register(new ItemPickupProtection());
    public static final ItemUseProtection ITEM_USE = register(new ItemUseProtection());

    public static <T extends Protection> T register(T protection) {
        ProtectionApi.API.register(protection);
        return protection;
    }

    public static void init() {} // NO-OP
}
