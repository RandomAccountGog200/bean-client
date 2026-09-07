package club.bean.client.feature;

import club.bean.client.BeanClient;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Holds a transient modifier on one of the player's attributes, and takes it
 * off again.
 *
 * <p>This is the sibling of {@link VanillaOption}: same borrow-and-return
 * shape, different thing being borrowed. Where {@code VanillaOption} moves a
 * slider in the video settings, this hangs a modifier on an entity attribute -
 * reach, step height, gravity - which is how several of the Combat and Movement
 * modules avoid needing a mixin.
 *
 * <p>"Transient" is the important word. A transient modifier lives on the
 * client's copy of the attribute map and is never serialised, so unlike a
 * borrowed video option there is nothing that could outlive the session and
 * nothing to park in the config. Rejoining a world builds a fresh player with a
 * fresh attribute map, which is also why {@link #set} re-applies rather than
 * assuming a previous call stuck.
 *
 * <p>What it does <em>not</em> do is convince the server. The server keeps its
 * own attribute map and validates against that, so a modifier here changes what
 * your client will let you aim at or walk up, not what the server will accept.
 * Reach is the clearest case: the raycast gets longer, and the server throws the
 * attack away.
 */
public final class AttributeHold {
    private final Holder<Attribute> attribute;
    private final Identifier id;
    private final AttributeModifier.Operation operation;

    /** The amount currently applied, or NaN when nothing is held. */
    private double applied = Double.NaN;

    /**
     * @param name      a unique path fragment; becomes the modifier's id
     * @param attribute which attribute to hang the modifier on
     * @param operation ADD_VALUE for a flat offset, ADD_MULTIPLIED_* to scale
     */
    public AttributeHold(String name, Holder<Attribute> attribute, AttributeModifier.Operation operation) {
        this.attribute = attribute;
        this.id = BeanClient.id("hold/" + name);
        this.operation = operation;
    }

    /** Applies {@code amount}, replacing whatever this hold applied before. */
    public void set(LivingEntity entity, double amount) {
        AttributeInstance instance = instance(entity);
        if (instance == null) {
            return;
        }
        // The == is deliberate: the amount comes from a slider that produces the
        // same double until it moves. The hasModifier() half is what catches a
        // respawn, where the value is unchanged but the map underneath is new.
        if (applied == amount && instance.hasModifier(id)) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        applied = amount;
    }

    /** Removes the modifier. Safe to call when nothing is held. */
    public void clear(LivingEntity entity) {
        AttributeInstance instance = instance(entity);
        if (instance != null) {
            instance.removeModifier(id);
        }
        applied = Double.NaN;
    }

    public boolean isHeld() {
        return !Double.isNaN(applied);
    }

    private AttributeInstance instance(LivingEntity entity) {
        return entity == null ? null : entity.getAttribute(attribute);
    }
}
