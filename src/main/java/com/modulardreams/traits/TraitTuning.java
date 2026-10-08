package com.modulardreams.traits;

/**
 * All tunable numbers of the trait effects. Every value marked
 * "FLAGGED FOR USER" is a sensible default chosen where the spec did not
 * give an exact number - adjust here, nowhere else.
 *
 * <p>Rebalance note (2026-10): traits and modifiers have NO levels - a
 * modifier is either applied or not. All former per-level values are now
 * single flat amounts.
 */
public final class TraitTuning {

    // ---- smithing modifier magnitudes ----
    /** Quartz "Power": flat attack damage bonus. */
    public static final float POWER_DAMAGE = 1.0F;
    /** Flint "Swift": attack speed bonus. */
    public static final float SWIFT_SPEED = 0.25F;
    /** Redstone "Haste": +15% mining speed - a multiplier on the
     *  BLOCK_BREAK_SPEED attribute (its base 1.0 IS the speed multiplier). */
    public static final double HASTE_BREAK_MULT = 0.15D;
    /** Emerald "Reinforced": max-durability multiplier (+20%). */
    public static final double DURABLE_MULT = 0.20D;
    /** Lapis "Fortune": counts as Fortune I for block loot, Looting I for kills. */
    public static final int LUCK_LEVEL = 1;
    /** Bone "Piercing": 20% of the target's ARMOR reduction and 20% of the
     *  target's Protection reduction are ignored (flat, separate layers). */
    public static final double PIERCING_ARMOR_FRACTION = 0.20D;
    public static final double PIERCING_PROTECTION_FRACTION = 0.20D;
    /** Amethyst "Resonance": attack speed gained per chained hit; the chain
     *  window is 3 s and the bonus caps at +0.6 (6 chained hits). */
    public static final double RESONANCE_SPEED_PER_HIT = 0.10D;
    public static final int RESONANCE_WINDOW_TICKS = 60; // 3 s
    public static final int RESONANCE_MAX_CHAIN = 6;

    // ---- modifier slot caps ----
    /** A tool takes at most 3 applied modifiers; the nether star / elytra /
     *  dragon egg unlocks raise the cap to 4 / 5 / 6 (see ModularData). */
    public static final int BASE_MODIFIER_CAP = 3;

    // ---- material trait numbers ----
    /** Wood "Regenerative": repair interval, condition: feet in water + head in sunlight. */
    public static final int WOOD_REPAIR_INTERVAL_TICKS = 60; // 3 s
    public static final int WOOD_REPAIR_AMOUNT = 1;
    /** Stone "Shadowed": below Y 63 + no sky light above the eyes. */
    public static final double SHADOWED_MINING_SPEED = 10.0D;
    public static final double SHADOWED_ARMOR_BONUS = 2.0D;
    public static final int SHADOWED_Y_LEVEL = 63;
    /** Copper "Powered": scan radius, immunity window after leaving redstone. */
    public static final int COPPER_SCAN_RADIUS = 10;
    public static final int COPPER_SCAN_INTERVAL_TICKS = 40; // 2 s; immunity lasts 10 s
    public static final int COPPER_IMMUNITY_TICKS = 200; // 10 s
    /** Iron "Last Stand": strength / resistance at or below this HP. */
    public static final float LAST_STAND_HP = 4.0F;
    public static final int LAST_STAND_EFFECT_TICKS = 60;
    /** Gold "Piglin's Favor": Absorption I for 5 min 10 s, refreshed on a
     *  5-min schedule - the 10 s of overlap covers server lag, so the hearts
     *  never flicker off between refreshes. Hearts lost to damage only come
     *  back at the next refresh (absorption HP do not regenerate). */
    public static final int GOLD_ABSORPTION_DURATION_TICKS = 6200; // 5 min 10 s
    public static final int GOLD_ABSORPTION_REFRESH_TICKS = 6000; // 5 min
    /** Diamond "Fortunate": durability-save chance + conditions. */
    public static final float FORTUNATE_SAVE_CHANCE = 0.5F;
    public static final float FORTUNATE_MOB_HP_LIMIT = 20.0F;
    public static final float FORTUNATE_ARMOR_DAMAGE_LIMIT = 2.0F;
    /** Netherite "Netherforged": NO player fire immunity (that would make
     *  Fire Resistance potions useless). Items are the fireproof ones (the
     *  baked DamageResistant component) and, IN THE NETHER, extreme heat
     *  empowers the held item: +20% attack damage on weapons, +20% mining
     *  speed on tools, and every netherite item takes 50% less durability
     *  damage while the heat is near. */
    public static final int NETHER_HEAT_SCAN_INTERVAL_TICKS = 40; // 2 s; heat changes slowly
    public static final int NETHER_HEAT_RADIUS = 8;
    public static final double NETHER_HEAT_ATTACK_MULT = 0.20D;
    public static final double NETHER_HEAT_MINING_MULT = 0.20D;
    public static final double NETHER_HEAT_DURABILITY_REDUCTION = 0.50D;
    /** Blaze rod "Blazing": fire ticks applied to mobs hit (3 s). */
    public static final int BLAZING_FIRE_TICKS = 60;
    /** Breeze rod "Windborne": upward launch strength + 2 s internal cooldown
     *  (so the gust is a jump tool, not unrestricted flight). */
    public static final double WINDBORNE_LAUNCH_POWER = 1.1D; // FLAGGED FOR USER
    public static final int WINDBORNE_COOLDOWN_TICKS = 40; // 2 s

    // ---- lining trait numbers ----
    /** Magma cream "Heatproof": fraction of fire/lava damage removed (50%). */
    public static final double HEATPROOF_REDUCTION = 0.50D;
    /** Phantom membrane: slow-fall duration while crouching in the End. */
    public static final int PHANTOM_SLOW_FALL_TICKS = 80;
    /**
     * Slime "Bouncy": the bounce scales with the impact speed. The bounce is
     * impact speed (blocks/tick) times RESTITUTION, clamped to [MIN, MAX].
     * MIN keeps small hops at the old feel; MAX stops long falls from launching
     * the player absurdly high. All three are FLAGGED FOR USER.
     */
    public static final double BOUNCY_RESTITUTION = 0.7D; // FLAGGED FOR USER
    public static final double BOUNCY_MIN_BOUNCE = 0.6D; // FLAGGED FOR USER
    public static final double BOUNCY_MAX_BOUNCE = 2.0D; // FLAGGED FOR USER

    private TraitTuning() {}
}
