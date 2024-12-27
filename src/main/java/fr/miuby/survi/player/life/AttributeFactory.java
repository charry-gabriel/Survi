package fr.miuby.survi.player.life;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.List;

public class AttributeFactory {
    private final List<AttributeModifier> attributeModifiers = new ArrayList<>();
    public AttributeFactory() {
        //region capitaine
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 0.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.MAX_HEALTH, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.ARMOR, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.5f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.ARMOR, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.ARMOR, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.FALL_DAMAGE_MULTIPLIER, 0.1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.ARMOR, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        //endregion

        //region pilote
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.9f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.ARMOR, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 0.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 2f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.ARMOR, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.7f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.ARMOR, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.FALL_DAMAGE_MULTIPLIER, 0.9f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.ARMOR, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        //endregion

        //region voyageur
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.ARMOR, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.ARMOR, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.ARMOR, 0.7f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.ARMOR, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 3f));
        //endregion
    }

    public List<AttributeModifier> getAttributeModifier(EWorld world, ERole role) {
        List<AttributeModifier> returnedModifiers = new ArrayList<>();
        for (AttributeModifier attributeModifier : attributeModifiers) {
            if ((world == attributeModifier.getWorld() || attributeModifier.getWorld() == EWorld.ALL) && (attributeModifier.getRole() == ERole.ALL || role == attributeModifier.getRole())) {
                returnedModifiers.add(attributeModifier);
            }
        }
        return returnedModifiers;
    }
}
