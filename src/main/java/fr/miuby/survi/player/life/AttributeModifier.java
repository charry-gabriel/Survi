package fr.miuby.survi.player.life;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

public class AttributeModifier {
    private final EWorld world;
    private final ERole role;
    private final Attribute attributeType;
    private final float attributeModifier;

    public AttributeModifier(EWorld world, ERole role, Attribute attributeType, float attributeModifier) {
        this.world = world;
        this.role = role;
        this.attributeModifier = attributeModifier;
        this.attributeType = attributeType;
    }

    public Attribute getAttributeType() {
        return attributeType;
    }

    public float getAttributeModifier() {
        return attributeModifier;
    }

    public ERole getRole() {
        return role;
    }

    public EWorld getWorld() {
        return world;
    }
}
