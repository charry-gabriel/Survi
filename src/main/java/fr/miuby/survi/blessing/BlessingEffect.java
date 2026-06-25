package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;

public abstract class BlessingEffect {
    public abstract void applyEffect(AlphaPlayer player);

    public void resetEffect(AlphaPlayer player) {
        // no-op par défaut
    }

    public boolean requiresOnlinePlayer() { return false; }

    /**
     * Retourne {@code true} si l'effet est one-shot (ex : incrément global du monde).
     * Ces effets ne sont PAS rejoués dans {@code applyAllCurrentBlessing} à la reconnexion —
     * leur résultat est déjà persisté en base et ne doit pas être réappliqué.
     */
    public boolean isOneShot() { return false; }
}