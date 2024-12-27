package fr.miuby.survi.player.life;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.List;

public class AttributeFactory {
    private final List<AttributeModifier> attributeModifiers = new ArrayList<>();
    public AttributeFactory() {
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.MAIRE, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.MAIRE, Attribute.MAX_HEALTH, 0.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.ALL, Attribute.MAX_HEALTH, 0.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.ALL, ERole.ALL, Attribute.MOVEMENT_SPEED, 20f));
    }

    public AttributeModifier getAttributeModifier(EWorld world, ERole role) {
        for (AttributeModifier attributeModifier : attributeModifiers) {
            if ((world == attributeModifier.getWorld() || attributeModifier.getWorld() == EWorld.ALL) && (attributeModifier.getRole() == ERole.ALL || role == attributeModifier.getRole())) {
                return attributeModifier;
            }
        }
        return null;
    }
}
