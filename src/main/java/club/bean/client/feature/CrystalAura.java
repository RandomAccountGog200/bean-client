package club.bean.client.feature;

import club.bean.client.module.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal Aura: places end crystals next to a target and breaks them.
 *
 * <p>Structurally this is the most involved module in the client, because
 * unlike Killaura it cannot just pick the nearest thing and swing. Placing a
 * crystal badly kills you, so the module is really a search: score every legal
 * position nearby by what the resulting explosion would do to the target and to
 * you, and act only on the best one that clears both thresholds.
 *
 * <h2>Where the placement rules come from</h2>
 *
 * <p>They are vanilla's, read out of {@code EndCrystalItem.useOn} rather than
 * remembered, because getting them slightly wrong means the client sends
 * placements the server rejects. A crystal goes on obsidian or bedrock when the
 * block above is empty and no entity is inside
 * {@code AABB(x, y, z, x+1, y+2, z+1)} of that empty block. The crystal itself
 * then sits at the centre of that column, half a block in on x and z.
 *
 * <h2>The damage estimate</h2>
 *
 * <p>The explosion maths is vanilla's too. A crystal explodes with power 6, and
 * vanilla derives damage from {@code f2 = radius * 2} - used both as the
 * distance divisor and as the final multiplier:
 *
 * <pre>{@code
 * d = (1 - distance / f2) * seenPercent
 * raw = (int) ((d * d + d) / 2 * 7 * f2 + 1)
 * }</pre>
 *
 * <p>{@link ServerExplosion#getSeenPercent} is public and static and only needs
 * the entity's own level, so the exposure term is the real one rather than an
 * approximation - which matters, because exposure is what makes a crystal
 * behind a block harmless.
 *
 * <p>Armour and toughness then go through vanilla's own
 * {@link CombatRules#getDamageAfterAbsorb}, and resistance is applied after.
 * <b>Enchantment protection is not.</b> Computing it needs a {@code ServerLevel}
 * the client does not have, so every estimate here is an upper bound. That
 * cuts the right way for your own safety - the module refuses placements that
 * are safer than it thinks - and the wrong way for the target, where a crystal
 * may do less than the minimum damage you asked for.
 */
public final class CrystalAura {
    private static final String MODULE_ID = "crystal_aura";

    /** An end crystal explodes with power 6. */
    private static final float CRYSTAL_POWER = 6.0f;
    /** Vanilla's {@code f2}: both the distance divisor and the damage scale. */
    private static final double BLAST = CRYSTAL_POWER * 2.0;

    private static long lastAction;

    private CrystalAura() {
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        if (!Settings.enabled(MODULE_ID)) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (System.currentTimeMillis() - lastAction < Settings.number(MODULE_ID, "Delay", 50)) {
            return;
        }
        LivingEntity victim = Targets.nearest(
                mc, Settings.number(MODULE_ID, "Target range", 12), Targets.Filter.PLAYERS);
        if (victim == null) {
            return;
        }
        double maxSelf = Settings.number(MODULE_ID, "Max self damage", 8);

        // Break before placing: an existing crystal is damage already sitting on
        // the board, and placing a second one next to it just wastes the first.
        if (Settings.flag(MODULE_ID, "Break", true) && breakCrystal(mc, player, maxSelf)) {
            lastAction = System.currentTimeMillis();
            return;
        }
        if (Settings.flag(MODULE_ID, "Place", true) && place(mc, player, victim, maxSelf)) {
            lastAction = System.currentTimeMillis();
        }
    }

    // ---- breaking ---------------------------------------------------------

    /** @return true if a crystal was hit */
    private static boolean breakCrystal(Minecraft mc, LocalPlayer player, double maxSelf) {
        double range = Settings.number(MODULE_ID, "Range", 4.5);
        double rangeSqr = range * range;
        Vec3 eye = player.getEyePosition();

        EndCrystal best = null;
        double nearest = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) {
                continue;
            }
            double distance = crystal.getBoundingBox().getCenter().distanceToSqr(eye);
            if (distance > rangeSqr || distance >= nearest) {
                continue;
            }
            // Breaking it sets it off where it stands, so the question is what
            // that explosion does to us - not to whoever placed it.
            if (damageTo(crystal.position(), player) > maxSelf) {
                continue;
            }
            best = crystal;
            nearest = distance;
        }
        if (best == null) {
            return false;
        }
        mc.gameMode.attack(player, best);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    // ---- placing ----------------------------------------------------------

    /** @return true if a crystal was placed */
    private static boolean place(Minecraft mc, LocalPlayer player, LivingEntity victim, double maxSelf) {
        InteractionHand hand = crystalHand(player);
        if (hand == null) {
            return false;
        }
        double range = Settings.number(MODULE_ID, "Range", 4.5);
        double minDamage = Settings.number(MODULE_ID, "Min damage", 6);
        Vec3 eye = player.getEyePosition();

        BlockPos best = null;
        double bestDamage = 0;

        BlockPos origin = player.blockPosition();
        int reach = (int) Math.ceil(range);

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            if (!canPlaceOn(mc, pos)) {
                continue;
            }
            // Range is measured to the block being clicked, the same way the
            // server measures it.
            if (Vec3.atCenterOf(pos).distanceToSqr(eye) > range * range) {
                continue;
            }
            // Where the crystal will actually sit: centred on the column, at
            // the foot of the empty block above.
            Vec3 centre = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

            if (damageTo(centre, player) > maxSelf) {
                continue;
            }
            double damage = damageTo(centre, victim);
            if (damage < minDamage || damage <= bestDamage) {
                continue;
            }
            bestDamage = damage;
            best = pos.immutable();
        }
        if (best == null) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best), Direction.UP, best, false);
        mc.gameMode.useItemOn(player, hand, hit);
        player.swing(hand);
        return true;
    }

    /**
     * Vanilla's placement rules, from {@code EndCrystalItem.useOn}.
     *
     * <p>Kept deliberately identical: a client-side filter that is looser than
     * the server sends placements that bounce, and one that is stricter quietly
     * loses positions that would have worked.
     */
    private static boolean canPlaceOn(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
            return false;
        }
        BlockPos above = pos.above();
        if (!mc.level.isEmptyBlock(above)) {
            return false;
        }
        AABB space = new AABB(
                above.getX(), above.getY(), above.getZ(),
                above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
        return mc.level.getEntities((Entity) null, space).isEmpty();
    }

    /** The hand holding end crystals, preferring the main one, or null. */
    private static InteractionHand crystalHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.END_CRYSTAL)) {
                return hand;
            }
        }
        return null;
    }

    // ---- damage -----------------------------------------------------------

    /**
     * What a crystal exploding at {@code centre} would do to {@code entity},
     * after armour and resistance.
     *
     * <p>See the class javadoc: this is an upper bound, because enchantment
     * protection cannot be computed client-side.
     */
    private static double damageTo(Vec3 centre, LivingEntity entity) {
        double distance = Math.sqrt(entity.distanceToSqr(centre)) / BLAST;
        if (distance > 1.0) {
            return 0;
        }
        float seen = ServerExplosion.getSeenPercent(centre, entity);
        double impact = (1.0 - distance) * seen;
        // The int cast is vanilla's, and it is load-bearing: the truncation is
        // why a crystal at the edge of its radius does exactly 1 damage.
        float raw = (int) ((impact * impact + impact) / 2.0 * 7.0 * BLAST + 1.0);
        return reduce(entity, raw);
    }

    private static float reduce(LivingEntity entity, float raw) {
        float damage = raw;
        DamageSource source = Explosion.getDefaultDamageSource(entity.level(), null);
        float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        damage = CombatRules.getDamageAfterAbsorb(
                entity, damage, source, entity.getArmorValue(), toughness);

        MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            int level = resistance.getAmplifier() + 1;
            damage = damage * Math.max(0, 25 - level * 5) / 25.0f;
        }
        return Math.max(0, damage);
    }
}
