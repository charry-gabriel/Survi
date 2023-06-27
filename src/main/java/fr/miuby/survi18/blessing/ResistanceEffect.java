package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;

public class ResistanceEffect extends BlessingEffect {
    private final int resistance;

    public ResistanceEffect(int reduction) {
        this.resistance = reduction;
    }

    @Override
    public void ApplyEffect(AlphaPlayer player) {
        player.setResistance(resistance);
    }
}
