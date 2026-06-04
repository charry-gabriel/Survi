package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.player.service.OfflineNotificationService;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.event.WorldLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Écoute les events nécessaires à {@link OfflineNotificationService} et lui délègue entièrement.
 *
 * <ul>
 *   <li>{@link AlphaPlayerJobLevelUpEvent}  → accumulation si joueur absent</li>
 *   <li>{@link WorldLevelUpEvent}           → accumulation pour tous les joueurs absents</li>
 *   <li>{@link VillagerLevelUpEvent}        → accumulation pour tous les joueurs absents</li>
 *   <li>{@link PlayerJoinEvent} (HIGH)      → livraison des notifications en attente</li>
 * </ul>
 */
public class OfflineNotificationListener implements Listener {

    @EventHandler
    public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        service().recordJobLevelUp(event);
    }

    @EventHandler
    public void onWorldLevelUp(WorldLevelUpEvent event) {
        service().recordWorldLevelUp(event);
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        service().recordVillagerLevelUp(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        service().deliverPending(event.getPlayer().getUniqueId(), event.getPlayer());
    }

    private OfflineNotificationService service() {
        return GameManager.getInstance().getOfflineNotificationService();
    }
}