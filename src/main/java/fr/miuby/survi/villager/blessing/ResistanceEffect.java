package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setResistanceModifier(resistance);
    }
}
