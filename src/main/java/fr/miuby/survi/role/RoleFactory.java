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
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR, 0.5f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_HEALTH, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.1f, RoleAttribute.Operation.ADD_SCALAR));


        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_DAMAGE, 0.5f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SAFE_FALL_DISTANCE, 1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_DAMAGE, 0.25f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.01f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SAFE_FALL_DISTANCE, 1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_DAMAGE, 0.4f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.05f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SAFE_FALL_DISTANCE, 10f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_DAMAGE, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.01f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 0.5f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SAFE_FALL_DISTANCE, 1f, RoleAttribute.Operation.ADD_SCALAR));

        roles.put(ERole.DRAGON, new Role(ERole.DRAGON, Component.text("[Dragon \uD83D\uDC09]", NamedTextColor.GOLD), roleAttributes, "dragon"));
        //endregion

        //region LoupGarou
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR, -0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 6f , RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0.35f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_SPEED, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_HEALTH, -0.05f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.STEP_HEIGHT, 0.4f , RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, -0.5f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SNEAKING_SPEED, 0.7f, RoleAttribute.Operation.ADD_NUMBER));


        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 0.1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 0.1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 0.6f, RoleAttribute.Operation.ADD_SCALAR));
        
        roles.put(ERole.LOUP_GAROU, new Role(ERole.LOUP_GAROU, Component.text("[Loup Garou \uD83D\uDC3A]", NamedTextColor.DARK_RED), roleAttributes, "loup_garou"));
        //endregion

        //region Fee
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR, -0.3f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_HEALTH, -0.3f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SAFE_FALL_DISTANCE, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.FALL_DAMAGE_MULTIPLIER, -1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.OXYGEN_BONUS, 3f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.25f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_ABSORPTION, 6f, RoleAttribute.Operation.ADD_NUMBER));


        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, -0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.GRAVITY, -0.05f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ATTACK_KNOCKBACK, 2f, RoleAttribute.Operation.ADD_NUMBER));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, -0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.GRAVITY, -0.05f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ATTACK_KNOCKBACK, 2f, RoleAttribute.Operation.ADD_NUMBER));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, -0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.GRAVITY, -0.05f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ATTACK_KNOCKBACK, 3f, RoleAttribute.Operation.ADD_NUMBER));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 0.4f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.GRAVITY, -0.075f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ATTACK_KNOCKBACK, 5f, RoleAttribute.Operation.ADD_NUMBER));

        roles.put(ERole.FEE, new Role(ERole.FEE, Component.text("[Fée \uD83E\uDDDA]", NamedTextColor.LIGHT_PURPLE), roleAttributes, "fee"));
        //endregion

        //region Nain
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0.70f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_INTERACTION_RANGE, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 0.3f, RoleAttribute.Operation.ADD_NUMBER));


        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.08f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, -0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, -0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, -0.02f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, -0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, -0.02f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 0.35f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));

        roles.put(ERole.NAIN, new Role(ERole.NAIN, Component.text("[Nain \uD83C\uDF44]", NamedTextColor.GREEN), roleAttributes, "nain"));
        //endregion

        //region Geant
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0.70f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_INTERACTION_RANGE, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 0.3f, RoleAttribute.Operation.ADD_NUMBER));


        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.08f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.ARMOR, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.MAX_HEALTH, 1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.SCALE, 0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.WILDERNESS, Attribute.BLOCK_BREAK_SPEED, 1f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.ARMOR, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MAX_HEALTH, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.MOVEMENT_SPEED, -0.02f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.SCALE, 0.15f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));

        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.ARMOR, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MAX_HEALTH, 0.1f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.MOVEMENT_SPEED, -0.02f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.SCALE, 0.65f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 0.5f, RoleAttribute.Operation.ADD_SCALAR));
        
        roles.put(ERole.GEANT, new Role(ERole.GEANT, Component.text("[Géant \uD83C\uDF44]", NamedTextColor.DARK_GREEN), roleAttributes, "geant"));
        //endregion

        //region Metier
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE,  1f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_SPEED, 0.2f, RoleAttribute.Operation.ADD_SCALAR));

        roles.put(ERole.COMBATANT, new Role(ERole.COMBATANT, Component.text("\uD83D\uDDE1", NamedTextColor.GRAY), roleAttributes, "Metier"));

        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.2f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_INTERACTION_RANGE, 0.2f, RoleAttribute.Operation.ADD_SCALAR));

        roles.put(ERole.MINEUR, new Role(ERole.MINEUR, Component.text("⛏", NamedTextColor.GRAY), roleAttributes, "Metier"));
        //endregion

        //region Novice
        roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.ARMOR, 20f, RoleAttribute.Operation.ADD_SCALAR));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.MAX_HEALTH, 20f, RoleAttribute.Operation.ADD_NUMBER));
        roleAttributes.add(new RoleAttribute(EWorld.VILLAGE, Attribute.SCALE, 0f, RoleAttribute.Operation.ADD_SCALAR));

        roles.put(ERole.NOVICE, new Role(ERole.NOVICE, Component.text("❤", NamedTextColor.YELLOW), roleAttributes, "novice"));
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

    //TODO: Remove the default after 1.21.4
    public List<RoleAttribute> defaultAttributes() {
        List<RoleAttribute> roleAttributes = new ArrayList<>();
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MAX_HEALTH, 20f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ARMOR_TOUGHNESS, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_DAMAGE, 1f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_KNOCKBACK, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ATTACK_SPEED, 4f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.LUCK, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.STEP_HEIGHT, 0.6f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SCALE, 1f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 1f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.SAFE_FALL_DISTANCE, 3f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 3f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.BLOCK_INTERACTION_RANGE, 4.5f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.OXYGEN_BONUS, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.WATER_MOVEMENT_EFFICIENCY, 0f, RoleAttribute.Operation.REMOVE));
        roleAttributes.add(new RoleAttribute(EWorld.ALL, Attribute.GRAVITY, 0.08f, RoleAttribute.Operation.REMOVE));
        return roleAttributes;
    }
}
