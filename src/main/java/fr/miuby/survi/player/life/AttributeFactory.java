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
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 0.075f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.MAX_HEALTH, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.ARMOR, 30f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.CAPITAINE, Attribute.SCALE, 1.1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.CAPITAINE, Attribute.SCALE, 1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.ARMOR, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.FALL_DAMAGE_MULTIPLIER, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.CAPITAINE, Attribute.SCALE, 0.5f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.MOVEMENT_SPEED, 0.11f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.ARMOR, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.CAPITAINE, Attribute.SCALE, 2f));
        //endregion

        //region pilote
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.09f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.ARMOR, 15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.PILOTE, Attribute.SCALE, 0.9f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.08f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.PILOTE, Attribute.SCALE, 0.8f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.07f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.ARMOR, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.FALL_DAMAGE_MULTIPLIER, 0.9f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.PILOTE, Attribute.SCALE, 1.3f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.MOVEMENT_SPEED, 0.05f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.PILOTE, Attribute.SCALE, 1.75f));
        //endregion

        //region voyageur
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.VOYAGEUR, Attribute.SCALE, 1f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.VOYAGEUR, Attribute.SCALE, 0.9f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.VOYAGEUR, Attribute.SCALE, 0.85f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.MOVEMENT_SPEED, 0.08f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.ATTACK_DAMAGE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.BLOCK_BREAK_SPEED, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.VOYAGEUR, Attribute.SCALE, 1.5f));
        //endregion

        //region dragon
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.ARMOR, 30f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.ATTACK_DAMAGE, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.MAX_HEALTH, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.MOVEMENT_SPEED, 0.20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.SCALE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.BLOCK_BREAK_SPEED, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.DRAGON, Attribute.SAFE_FALL_DISTANCE, 10f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.ARMOR, 16f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.ATTACK_DAMAGE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.MAX_HEALTH, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.MOVEMENT_SPEED, 0.11f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.SCALE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.DRAGON, Attribute.SAFE_FALL_DISTANCE, 10f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.ATTACK_DAMAGE, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.MAX_HEALTH, 2.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.MOVEMENT_SPEED, 0.11f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.SCALE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.DRAGON, Attribute.SAFE_FALL_DISTANCE, 1024f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.ARMOR, 4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.ATTACK_DAMAGE, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.MAX_HEALTH, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.MOVEMENT_SPEED, 0.11f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.SCALE, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.DRAGON, Attribute.SAFE_FALL_DISTANCE, 10f));
        //endregion

        //region LoupGarou
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.ATTACK_DAMAGE, 30f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.ATTACK_SPEED, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.MAX_HEALTH, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.MOVEMENT_SPEED, 0.25f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.SCALE, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.LOUP_GAROU, Attribute.ENTITY_INTERACTION_RANGE, 3f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.ATTACK_DAMAGE, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.ATTACK_SPEED, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.SCALE, 1.11f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.LOUP_GAROU, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.ATTACK_DAMAGE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.ATTACK_SPEED, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.MAX_HEALTH, 0.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.SCALE, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.LOUP_GAROU, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.ATTACK_DAMAGE, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.ATTACK_SPEED, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.SCALE, 1.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.BLOCK_BREAK_SPEED, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.LOUP_GAROU, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));
        //endregion

        //region Fee
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.MAX_HEALTH, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.MOVEMENT_SPEED, 0.4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.SCALE, 0.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.BLOCK_BREAK_SPEED, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.GRAVITY, 0.03f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.OXYGEN_BONUS, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.WATER_MOVEMENT_EFFICIENCY, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.ATTACK_KNOCKBACK, 2.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.FEE, Attribute.ENTITY_INTERACTION_RANGE, 5f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.MAX_HEALTH, 0.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.MOVEMENT_SPEED, 0.25f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.SCALE, 0.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.GRAVITY, 0.03f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.SAFE_FALL_DISTANCE, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.OXYGEN_BONUS, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.WATER_MOVEMENT_EFFICIENCY, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.ATTACK_KNOCKBACK, 2.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.FEE, Attribute.ENTITY_INTERACTION_RANGE, 5f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.MAX_HEALTH, 0.25f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.MOVEMENT_SPEED, 0.25f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.SCALE, 0.75f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.GRAVITY, 0.03f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.SAFE_FALL_DISTANCE, 6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.OXYGEN_BONUS, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.WATER_MOVEMENT_EFFICIENCY, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.ATTACK_KNOCKBACK, 2.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.FEE, Attribute.ENTITY_INTERACTION_RANGE, 5f));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.ARMOR, 0f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.MAX_HEALTH, 0.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.MOVEMENT_SPEED, 0.4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.SCALE, 1.25f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.GRAVITY, 0.01f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.SAFE_FALL_DISTANCE, 1024f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.OXYGEN_BONUS, 5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.WATER_MOVEMENT_EFFICIENCY, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.ATTACK_KNOCKBACK, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.FEE, Attribute.ENTITY_INTERACTION_RANGE, 5f));
        //endregion

        //region Nain
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.ARMOR, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.SCALE, 0.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.BLOCK_BREAK_SPEED, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.NAIN, Attribute.BLOCK_INTERACTION_RANGE, 8));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.ARMOR, 30f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.MAX_HEALTH, 2.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.MOVEMENT_SPEED, 0.08f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.SCALE, 0.6f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.NAIN, Attribute.BLOCK_INTERACTION_RANGE, 7));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.MOVEMENT_SPEED, 0.09f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.SCALE, 0.7f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.BLOCK_BREAK_SPEED, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.SAFE_FALL_DISTANCE, 1024f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.NAIN, Attribute.BLOCK_INTERACTION_RANGE, 6));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN , Attribute.ARMOR, 4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.SCALE, 1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.NAIN, Attribute.BLOCK_INTERACTION_RANGE, 5));
        //endregion

        //region Geant
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.ARMOR, 20f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.MAX_HEALTH, 3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.MOVEMENT_SPEED, 0.15f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.SCALE, 1.111f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.BLOCK_BREAK_SPEED, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.VILLAGE, ERole.GEANT, Attribute.BLOCK_INTERACTION_RANGE, 8));

        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.ARMOR, 30f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.MAX_HEALTH, 2.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.MOVEMENT_SPEED, 0.08f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.SCALE, 1.2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.WILDERNESS, ERole.GEANT, Attribute.BLOCK_INTERACTION_RANGE, 7));

        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.ARMOR, 8f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.MOVEMENT_SPEED, 0.09f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.SCALE, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.BLOCK_BREAK_SPEED, 1.5f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.SAFE_FALL_DISTANCE, 1024f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.NETHER, ERole.GEANT, Attribute.BLOCK_INTERACTION_RANGE, 6));

        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT , Attribute.ARMOR, 4f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.MAX_HEALTH, 1.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.MOVEMENT_SPEED, 0.1f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.SCALE, 2f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.SAFE_FALL_DISTANCE, 10f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.ENTITY_INTERACTION_RANGE, 3.3f));
        this.attributeModifiers.add(new AttributeModifier(EWorld.END, ERole.GEANT, Attribute.BLOCK_INTERACTION_RANGE, 5));
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
