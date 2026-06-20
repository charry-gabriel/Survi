package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.survi.world.zone.VillageZoneStageUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Réagit à {@link VillageZoneStageUpEvent} pour annoncer en chat, à tous les joueurs
 * en ligne, l'agrandissement de la zone village.
 */
public class VillageZoneStageUpListener implements Listener {

    @EventHandler
    public void onVillageZoneStageUp(VillageZoneStageUpEvent event) {
        LangService ls = GameManager.getInstance().getLangService();
        VillageZoneConfig.VillageZoneStage stage = event.getNewStage();

        ls.broadcast("world.zone.expanded.broadcast", stage.halfWidth(), stage.halfDepth());
    }
}