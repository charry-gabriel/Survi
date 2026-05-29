package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setResistanceModifier(resistance);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        player.setResistanceModifier(SurviConfig.getInstance().getNormalResistanceModifier());
    }
}