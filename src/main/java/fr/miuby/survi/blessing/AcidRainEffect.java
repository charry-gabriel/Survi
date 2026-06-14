package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;

/**
 * Effet global : active ou désactive la pluie acide sur le serveur.
 *
 * <p>Quand actif, tout joueur exposé à la pluie ({@code player.isInRain()}) sans
 * niveau Pêcheur suffisant subit des dégâts périodiques (voir {@code rain.acid.*} dans config.yml).
 * L'état persiste jusqu'à un appel explicite de {@link #resetEffect}.
 */
public class AcidRainEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getRainManager().setAcidRainActive(true);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getRainManager().setAcidRainActive(false);
    }
}
