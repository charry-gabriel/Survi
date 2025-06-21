package fr.miuby.survi.role;

import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;

import java.util.*;

public class RoleFactory {
    private final Map<ERole, Role> roles = new EnumMap<>(ERole.class);

    public RoleFactory() {
        List<RoleDefinition> roleDefinitions = Arrays.asList(
            // Dragon
            new RoleDefinition(ERole.DRAGON, "Dragon \uD83D\uDC09", NamedTextColor.GOLD, "dragon", b -> b
                    .add(EWorld.ALL, Attribute.ARMOR, 0.5f)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, 1f)
                    .add(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.1f)

                    .add(EWorld.VILLAGE, Attribute.ATTACK_DAMAGE, 0.5f)
                    .add(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.VILLAGE, Attribute.SAFE_FALL_DISTANCE, 1f)

                    .add(EWorld.WILDERNESS, Attribute.ATTACK_DAMAGE, 0.25f)
                    .add(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.01f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.WILDERNESS, Attribute.SAFE_FALL_DISTANCE, 1f)

                    .add(EWorld.NETHER, Attribute.ATTACK_DAMAGE, 0.4f)
                    .add(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.05f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.NETHER, Attribute.SAFE_FALL_DISTANCE, 10f)

                    .add(EWorld.END, Attribute.ATTACK_DAMAGE, 0.1f)
                    .add(EWorld.END, Attribute.MOVEMENT_SPEED, 0.01f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.END, Attribute.SCALE, 0.556f)
                    .add(EWorld.END, Attribute.SAFE_FALL_DISTANCE, 1f)
            ),

            // Loup Garou
            new RoleDefinition(ERole.LOUP_GAROU, "Loup Garou \uD83D\uDC3A", NamedTextColor.DARK_RED, "loup_garou", b -> b
                    .add(EWorld.ALL, Attribute.ARMOR, -0.1f)
                    .add(EWorld.ALL, Attribute.ATTACK_DAMAGE, 6f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.KNOCKBACK_RESISTANCE, 0.35f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.ATTACK_SPEED, 1f)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, -0.05f)
                    .add(EWorld.ALL, Attribute.STEP_HEIGHT, 0.4f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, -0.5f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.SNEAKING_SPEED, 0.7f, RoleAttribute.Operation.ADD_NUMBER)

                    .add(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.VILLAGE, Attribute.SCALE, 0.056f)
                    .add(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.1f)

                    .add(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.WILDERNESS, Attribute.SCALE, 0.056f)

                    .add(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.NETHER, Attribute.SCALE, 0.056f)
                    .add(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 0.1f)

                    .add(EWorld.END, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.END, Attribute.SCALE, 0.611f)
            ),

            // Fée
            new RoleDefinition(ERole.FEE, "Fée \uD83E\uDDDA", NamedTextColor.LIGHT_PURPLE, "fee", b -> b
                    .add(EWorld.ALL, Attribute.ARMOR, -0.3f)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, -0.3f)
                    .add(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.1f)
                    .add(EWorld.ALL, Attribute.SAFE_FALL_DISTANCE, 1f)
                    .add(EWorld.ALL, Attribute.FALL_DAMAGE_MULTIPLIER, -1f)
                    .add(EWorld.ALL, Attribute.OXYGEN_BONUS, 3f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.25f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.ENTITY_INTERACTION_RANGE, 2f)
                    .add(EWorld.ALL, Attribute.MAX_ABSORPTION, 6f, RoleAttribute.Operation.ADD_NUMBER)

                    .add(EWorld.VILLAGE, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.VILLAGE, Attribute.SCALE, -0.056f)
                    .add(EWorld.VILLAGE, Attribute.GRAVITY, -0.5f)
                    .add(EWorld.VILLAGE, Attribute.ATTACK_KNOCKBACK, 2f, RoleAttribute.Operation.ADD_NUMBER)

                    .add(EWorld.WILDERNESS, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.WILDERNESS, Attribute.SCALE, -0.056f)
                    .add(EWorld.WILDERNESS, Attribute.GRAVITY, -0.5f)
                    .add(EWorld.WILDERNESS, Attribute.ATTACK_KNOCKBACK, 2f, RoleAttribute.Operation.ADD_NUMBER)

                    .add(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.NETHER, Attribute.SCALE, -0.056f)
                    .add(EWorld.NETHER, Attribute.GRAVITY, -0.5f)
                    .add(EWorld.NETHER, Attribute.ATTACK_KNOCKBACK, 3f, RoleAttribute.Operation.ADD_NUMBER)

                    .add(EWorld.END, Attribute.MOVEMENT_SPEED, 0.04f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.END, Attribute.SCALE, 0.5f)
            ),

            // Nain
            new RoleDefinition(ERole.NAIN, "Nain \uD83C\uDF44", NamedTextColor.DARK_GRAY, "nain", b -> b
                    .add(EWorld.ALL, Attribute.ARMOR, 0.2f)
                    .add(EWorld.ALL, Attribute.ATTACK_DAMAGE, 0.1f)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, 0.2f)
                    .add(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.3f)
                    .add(EWorld.ALL, Attribute.MOVEMENT_SPEED, -0.1f, RoleAttribute.Operation.ADD_NUMBER)
            ),

            // Géant
            new RoleDefinition(ERole.GEANT, "Géant \uD83C\uDF44", NamedTextColor.RED, "geant", b -> b
                    .add(EWorld.ALL, Attribute.ARMOR, 0.3f)
                    .add(EWorld.ALL, Attribute.ATTACK_DAMAGE, 0.3f)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, 0.5f)
                    .add(EWorld.ALL, Attribute.MOVEMENT_SPEED, -0.2f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.SCALE, 0.3f)
            ),

            // Combattant
            new RoleDefinition(ERole.COMBATANT, "\uD83D\uDDE1", NamedTextColor.DARK_RED, "combatant", b -> b
                    .add(EWorld.ALL, Attribute.ATTACK_DAMAGE, 0.4f)
                    .add(EWorld.ALL, Attribute.ATTACK_SPEED, 0.2f)
                    .add(EWorld.ALL, Attribute.ARMOR, 0.2f)
            ),

            // Mineur
            new RoleDefinition(ERole.MINEUR, "⛏", NamedTextColor.GRAY, "mineur", b -> b
                    .add(EWorld.ALL, Attribute.BLOCK_BREAK_SPEED, 0.4f)
                    .add(EWorld.ALL, Attribute.LUCK, 1.0f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.ARMOR, 0.1f)
            ),

            // Novice (rôle par défaut)
            new RoleDefinition(ERole.NOVICE, "❤", NamedTextColor.GRAY, "novice", b ->
                    // Pas de bonus particulier pour les novices
                    b.add(EWorld.ALL, Attribute.LUCK, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
            ),

            // Alchimiste
            new RoleDefinition(ERole.ALCHIMISTE, "⚗", NamedTextColor.DARK_PURPLE, "alchimiste", b -> b
                    .add(EWorld.ALL, Attribute.LUCK, 1.0f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.MAX_HEALTH, -0.1f)
                    .add(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.1f)
                    .add(EWorld.NETHER, Attribute.BLOCK_BREAK_SPEED, 0.2f)
                    .add(EWorld.NETHER, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
            ),

            // Enchanteur
            new RoleDefinition(ERole.ENCHANTEUR, "\uD83E\uDDD9", NamedTextColor.AQUA, "enchanteur", b -> b
                    .add(EWorld.ALL, Attribute.LUCK, 2.0f, RoleAttribute.Operation.ADD_NUMBER)
                    .add(EWorld.ALL, Attribute.ARMOR, -0.1f)
                    .add(EWorld.VILLAGE, Attribute.BLOCK_BREAK_SPEED, 0.15f)
                    .add(EWorld.END, Attribute.BLOCK_BREAK_SPEED, 0.25f)
                    .add(EWorld.END, Attribute.MOVEMENT_SPEED, 0.1f, RoleAttribute.Operation.ADD_NUMBER)
            )
        );
        
        // Construction de tous les rôles
        roleDefinitions.forEach(def -> roles.put(def.getType(), def.toRole()));
    }

    public Role getRole(ERole role) {
        return roles.get(role);
    }
    
    public Collection<Role> getAllRoles() {
        return Collections.unmodifiableCollection(roles.values());
    }

    public Collection<Role> getRoles() {
        return roles.values();
    }

    public Role getDefaultRole() {
        return roles.get(ERole.NAIN);
    }

    public Role getRole(String roleType) {
        try {
            return getRole(ERole.valueOf(roleType));
        } catch (IllegalArgumentException e) {
            return null;
        }
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
