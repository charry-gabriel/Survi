package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.setResistanceModifier(resistance);
    }
}
