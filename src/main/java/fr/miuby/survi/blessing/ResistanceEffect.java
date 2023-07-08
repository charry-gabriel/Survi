package fr.miuby.survi.blessing;

import fr.miuby.survi.AlphaPlayer;

public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    public ResistanceEffect(float reduction) {
        this.resistance = reduction;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setResistance(resistance);
    }
}
