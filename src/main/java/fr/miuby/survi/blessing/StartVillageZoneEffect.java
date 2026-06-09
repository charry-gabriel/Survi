package fr.miuby.survi.blessing;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;

import java.util.logging.Level;

/**
 * Démarre le timer de zone du village ({@link fr.miuby.survi.world.VillageZoneManager#start()}).
 *
 * <p>One-shot : si le timer est déjà en cours, l'appel est sans effet (idempotent).
 * Le {@code resetEffect} est volontairement no-op — arrêter la zone à la réinitialisation
 * d'un villageois n'aurait pas de sens ; utiliser {@code /survi zone reset} si besoin.</p>
 */
public class StartVillageZoneEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        boolean started = GameManager.getInstance().getVillageZoneManager().start();
        if (started) {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[StartVillageZoneEffect] Timer de zone village démarré suite au levelup d'un villageois.");
        }
    }
}