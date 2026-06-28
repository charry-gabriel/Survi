package fr.miuby.survi.player.service;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.role.RoleAttribute;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.player.role.Role;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Level;

/**
 * Single place responsible for applying and removing role-based attributes on a player.
 * Used on join (via AlphaPlayer.addRoleAttribute), role change, and world change.
 * Listeners delegate here so all attribute logic stays in one place.
 */
public final class PlayerAttributeService {

    public void applyAllRoleAttributes(AlphaPlayer alphaPlayer) {
        applyAttributesForRole(alphaPlayer, alphaPlayer.getRole());
        for (Role subRole : alphaPlayer.getSubRoles()) {
            applyAttributesForRole(alphaPlayer, subRole);
        }
    }

    public void clearAllRoleAttributes(AlphaPlayer alphaPlayer) {
        removeAttributesForRole(alphaPlayer, alphaPlayer.getRole());
        for (Role subRole : alphaPlayer.getSubRoles()) {
            removeAttributesForRole(alphaPlayer, subRole);
        }
    }

    public void applyAttributesForRole(AlphaPlayer alphaPlayer, Role role) {
        if (alphaPlayer.getPlayer() == null || role == null) return;
        for (RoleAttribute attribute : role.attributes()) {
            if (alphaPlayer.getWorld().getType() != attribute.getWorld() && attribute.getWorld() != EWorld.ALL)
                continue;
            attribute.setRole(role.roleId());
            if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                alphaPlayer.getAlphaLife().regenHealth(() -> applyAttribute(alphaPlayer, attribute));
            else
                applyAttribute(alphaPlayer, attribute);
        }
    }

    public void removeAttributesForRole(AlphaPlayer alphaPlayer, Role role) {
        if (alphaPlayer.getPlayer() == null || role == null) return;
        for (RoleAttribute attribute : role.attributes()) {
            AttributeInstance playerAttribute = alphaPlayer.getPlayer().getAttribute(attribute.getAttributeType());
            if (playerAttribute == null) continue;
            String name = RoleAttribute.createName(attribute.getWorld().toString(), role.roleId(), attribute.getAttributeType().getKey().getKey());
            AttributeModifier mod = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), name));
            if (mod != null) {
                if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                    alphaPlayer.getAlphaLife().regenHealth(() -> playerAttribute.removeModifier(mod));
                else
                    playerAttribute.removeModifier(mod);
            }
        }
    }

    public void applyAttribute(AlphaPlayer alphaPlayer, RoleAttribute roleAttribute) {
        if (roleAttribute.getName() == null)
            return;

        AttributeInstance playerAttribute = alphaPlayer.getPlayer().getAttribute(roleAttribute.getAttributeType());
        if (playerAttribute == null)
            return;

        AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), roleAttribute.getName()));

        if (attributeModifier != null)
            playerAttribute.removeModifier(attributeModifier);

        if (roleAttribute.getOperation() == RoleAttribute.EOperation.REMOVE) {
            // Convertit la valeur absolue cible en delta sur la base par defaut -> modifier transitoire
            // uniquement. setBaseValue() persistait en playerdata et causait des desync au reconnect.
            double delta = roleAttribute.getValue() - playerAttribute.getDefaultValue();
            playerAttribute.addTransientModifier(new AttributeModifier(
                    new NamespacedKey(GameManager.getInstance().getPlugin(), roleAttribute.getName()),
                    delta, AttributeModifier.Operation.ADD_NUMBER));
        } else {
            playerAttribute.addTransientModifier(roleAttribute.createAttributeModifier());
        }

        if (roleAttribute.getAttributeType() == Attribute.MAX_ABSORPTION) {
            alphaPlayer.getPlayer().removePotionEffect(PotionEffectType.ABSORPTION);
            alphaPlayer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 0, (int) roleAttribute.getValue()));
        }
    }

    /**
     * Variante de {@link #applyAllRoleAttributes} destinee exclusivement a la reconnexion.
     *
     * <p>N'utilise PAS {@code regenHealth} pour MAX_HEALTH : evite le rescaling proportionnel
     * intermediaire. Tous les modifiers sont poses a plat ; le clamp final est fait dans
     * {@code AlphaPlayerFactory.onPlayerJoin()} avec la vie sauvegardee par Minecraft.</p>
     */
    public void applyAllRoleAttributesOnJoin(AlphaPlayer alphaPlayer) {
        applyAttributesForRoleOnJoin(alphaPlayer, alphaPlayer.getRole());
        for (Role subRole : alphaPlayer.getSubRoles()) {
            applyAttributesForRoleOnJoin(alphaPlayer, subRole);
        }
        if (alphaPlayer.getPlayer() != null) {
            AttributeInstance maxHealth = alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[applyAllRoleAttributesOnJoin] " + alphaPlayer.getPseudo()
                            + " maxHealth=" + (maxHealth != null ? maxHealth.getValue() : "null"));
        }
    }

    private void applyAttributesForRoleOnJoin(AlphaPlayer alphaPlayer, Role role) {
        if (alphaPlayer.getPlayer() == null || role == null) return;
        for (RoleAttribute attribute : role.attributes()) {
            if (alphaPlayer.getWorld().getType() != attribute.getWorld() && attribute.getWorld() != EWorld.ALL)
                continue;
            attribute.setRole(role.roleId());
            // Direct : pas de regenHealth, le clamp se fait apres tous les attributs dans Factory.
            applyAttribute(alphaPlayer, attribute);
        }
    }

    /**
     * Recalculates role attributes for the current world: clears all role modifiers then reapplies main + sub-roles.
     * Use after world change.
     */
    public void reapplyAllRoleAttributes(AlphaPlayer alphaPlayer) {
        clearAllRoleAttributes(alphaPlayer);
        applyAllRoleAttributes(alphaPlayer);
    }

    /**
     * Updates attributes when a role is added or removed (main role change or sub-role add/remove).
     * Removes oldRole modifiers if non-null, adds newRole modifiers if non-null.
     */
    public void onRoleAttributesChange(AlphaPlayer alphaPlayer, Role oldRole, Role newRole) {
        if (oldRole != null)
            removeAttributesForRole(alphaPlayer, oldRole);
        if (newRole != null)
            applyAttributesForRole(alphaPlayer, newRole);
    }
}