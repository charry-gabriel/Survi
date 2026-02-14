package fr.miuby.survi.player;

import fr.miuby.survi.role.Role;

/**
 * Single place responsible for applying and removing role-based attributes on a player.
 * Used on join (via AlphaPlayer.addRoleAttribute), role change, and world change.
 * Listeners delegate here so all attribute logic stays in one place.
 */
public final class PlayerAttributeService {

    /**
     * Recalculates role attributes for the current world: clears all role modifiers then reapplies main + sub-roles.
     * Use after world change.
     */
    public void reapplyAllRoleAttributes(AlphaPlayer alphaPlayer) {
        alphaPlayer.clearAllRoleAttributes();
        alphaPlayer.addRoleAttribute();
    }

    /**
     * Updates attributes when a role is added or removed (main role change or sub-role add/remove).
     * Removes oldRole modifiers if non-null, adds newRole modifiers if non-null.
     */
    public void onRoleAttributesChange(AlphaPlayer alphaPlayer, Role oldRole, Role newRole) {
        if (oldRole != null)
            alphaPlayer.removeAttributesForRole(oldRole);
        if (newRole != null)
            alphaPlayer.addAttributesForRole(newRole);
    }
}
