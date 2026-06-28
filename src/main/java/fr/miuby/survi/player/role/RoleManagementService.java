package fr.miuby.survi.player.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.player.service.PlayerPersistenceService;
import fr.miuby.survi.system.lang.LangService;

public class RoleManagementService {
    private final PlayerPersistenceService persistenceService;
    private final AlphaPlayerFactory playerFactory;

    public RoleManagementService(PlayerPersistenceService persistence, AlphaPlayerFactory factory) {
        this.persistenceService = persistence;
        this.playerFactory = factory;
    }

    /**
     * Change le rôle principal du joueur.
     * Gère l'event, la persistance et le refresh.
     */
    public boolean changeMainRole(AlphaPlayer player, Role newRole) {
        Role oldRole = player.getRole();

        // Event
        AlphaPlayerRoleChangeEvent event = new AlphaPlayerRoleChangeEvent(player, oldRole, newRole);
        GameManager.getInstance().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Apply
        player.setRole(newRole);
        persistenceService.updateRole(player);

        // Refresh
        if (player.getPlayer() != null && player.getPlayer().isOnline()) {
            LangService ls = GameManager.getInstance().getLangService();
            player.getPlayer().sendMessage(
                    ls.text(player.getPlayer(), "player.role.applied",
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("role", newRole.displayName())));
        }

        return true;
    }

    /**
     * Ajoute un sous-rôle.
     */
    public boolean addSubRole(AlphaPlayer player, Role subRole) {
        // Event
        AlphaPlayerRoleChangeEvent event = new AlphaPlayerRoleChangeEvent(player, null, subRole);
        GameManager.getInstance().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Apply
        if (player.addSubRole(subRole)) {
            persistenceService.updateSubRoles(player);
            return true;
        }

        return false;
    }

    /**
     * Retire un sous-rôle.
     */
    public boolean removeSubRole(AlphaPlayer player, Role subRole) {
        // Event
        AlphaPlayerRoleChangeEvent event = new AlphaPlayerRoleChangeEvent(player, subRole, null);
        GameManager.getInstance().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Apply
        if (player.removeSubRole(subRole)) {
            persistenceService.updateSubRoles(player);
            return true;
        }

        return false;
    }
}