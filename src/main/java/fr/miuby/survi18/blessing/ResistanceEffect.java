package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;

public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    public ResistanceEffect(float reduction) {
        this.resistance = reduction;
    }

    @Override
    public void ApplyEffect(AlphaPlayer player) {
        player.setResistance(resistance);
    }
}
