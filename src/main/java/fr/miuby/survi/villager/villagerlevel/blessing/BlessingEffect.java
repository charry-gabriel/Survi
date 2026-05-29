package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;

public abstract class BlessingEffect {
    public abstract void applyEffect(VillagerLevel villager, AlphaPlayer player);

    /**
     * Annule l'effet de ce blessing.
     * Appelé lors du reset d'un villager pour remettre l'état du serveur
     * tel qu'il était avant que ce blessing n'ait été appliqué.
     *
     * La plupart des effets n'ont pas besoin d'être annulés (items one-shot,
     * messages, etc.) — la no-op par défaut suffit dans ces cas.
     */
    public void resetEffect(VillagerLevel villager, AlphaPlayer player) {
        // no-op par défaut
    }
}