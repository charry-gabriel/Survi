package fr.miuby.survi.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;

import java.util.*;

public class RoleFactory {
    private final Map<ERole, Role> roles = new HashMap<>();

    public RoleFactory() {
        //region dragon
        List<RoleAttribute> roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 30f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_DAMAGE, 15f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 5f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.20f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 1.2f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SAFE_FALL_DISTANCE, 10f));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 16f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_DAMAGE, 8f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 1.5f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.11f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SAFE_FALL_DISTANCE, 10f));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 12f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_DAMAGE, 4f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 2.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.11f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SAFE_FALL_DISTANCE, 1024f));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 4f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_DAMAGE, 2f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 1.2f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.11f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 1.5f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SAFE_FALL_DISTANCE, 10f));

        roles.put(ERole.DRAGON, new Role(ERole.DRAGON, Component.text("[Dragon]", NamedTextColor.GOLD), roleAttributes, "dragon"));
        //endregion

        //region LoupGarou
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_DAMAGE, 30f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_SPEED, 7f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 2f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.25f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ENTITY_INTERACTION_RANGE, 3f));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_DAMAGE, 20f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_SPEED, 7f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.15f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 1.11f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_DAMAGE, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_SPEED, 7f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 0.8f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.2f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 1.2f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_DAMAGE, 6f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_SPEED, 7f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.KNOCKBACK_RESISTANCE, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.15f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 1.75f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ENTITY_INTERACTION_RANGE, 2.2f));
        
        roles.put(ERole.LOUP_GAROU, new Role(ERole.LOUP_GAROU, Component.text("[Loup Garou]", NamedTextColor.DARK_RED), roleAttributes, "loup_garou"));
        //endregion

        //region Fee
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.4f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 0.8f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 1.2f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.GRAVITY, 0.04f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SAFE_FALL_DISTANCE, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.OXYGEN_BONUS, 5f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.75f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_KNOCKBACK, 2.5f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ENTITY_INTERACTION_RANGE, 10f));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.25f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 0.8f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.GRAVITY, 0.04f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SAFE_FALL_DISTANCE, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.OXYGEN_BONUS, 5f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.75f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_KNOCKBACK, 2.5f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ENTITY_INTERACTION_RANGE, 10f));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 0.33f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.25f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 0.8f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.GRAVITY, 0.04f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SAFE_FALL_DISTANCE, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.OXYGEN_BONUS, 5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.75f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_KNOCKBACK, 2.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ENTITY_INTERACTION_RANGE, 10f));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.4f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 1.25f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.GRAVITY, 0.02f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SAFE_FALL_DISTANCE, 1024f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.OXYGEN_BONUS, 5f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.75f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_KNOCKBACK, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ENTITY_INTERACTION_RANGE, 10f));

        roles.put(ERole.FEE, new Role(ERole.FEE, Component.text("[Fée]", NamedTextColor.LIGHT_PURPLE), roleAttributes, "fee"));
        //endregion

        //region Nain
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 2f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.15f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 0.6f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 30f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 3f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.08f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 0.7f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 8f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.09f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 0.7f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 1.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 4f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roles.put(ERole.NAIN, new Role(ERole.NAIN, Component.text("[Nain]", NamedTextColor.GREEN), roleAttributes, "nain"));
        //endregion

        //region Geant
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 10f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 2f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.15f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 1.11f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 30f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 3f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.08f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 1.15f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1.8f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 8f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 1.1f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.09f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 1.15f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 1.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_INTERACTION_RANGE, 8));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 4f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.1f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 2f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 1.3f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.KNOCKBACK_RESISTANCE, 0.66f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_INTERACTION_RANGE, 8));
        
        roles.put(ERole.GEANT, new Role(ERole.GEANT, Component.text("[Géant]", NamedTextColor.DARK_GREEN), roleAttributes, "geant"));
        //endregion

        //region Metier
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 8));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 3.5f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 8));

        roles.put(ERole.COMBATANT, new Role(ERole.COMBATANT, Component.text("\uD83D\uDDE1", NamedTextColor.GRAY), roleAttributes, "Metier"));

        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 1.8f));

        roles.put(ERole.MINEUR, new Role(ERole.MINEUR, Component.text("⛏", NamedTextColor.GRAY), roleAttributes, "Metier"));
        //endregion
    }

    public Role getRole(ERole role) {
        return roles.get(role);
    }

    public Collection<Role> getRoles() {
        return roles.values();
    }

    public Role getDefaultRole() {
        return roles.get(ERole.NAIN);
    }

    public Role getRole(String roleType) {
        for (String role : GameManager.getInstance().getRoleFactory().getRoles().stream().map(role -> role.type().toString()).toList()) {
            if (roleType.equals(role))
                return GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(role));
        }
        return null;
    }

    public List<RoleAttribute> defaultAttributes() {
        List<RoleAttribute> roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_HEALTH, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR_TOUGHNESS, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_KNOCKBACK, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_SPEED, 4f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.LUCK, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MOVEMENT_SPEED, 0.1f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.STEP_HEIGHT, 0.6f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SCALE, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 1f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SAFE_FALL_DISTANCE, 3f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 3f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_INTERACTION_RANGE, 4.5f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.OXYGEN_BONUS, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.WATER_MOVEMENT_EFFICIENCY, 0f));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.GRAVITY, 0.08f));
        return roleAttributes;
    }
}
