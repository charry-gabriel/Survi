package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResistanceEffect extends BlessingEffect {
    private final float resistance;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.setResistanceModifier(resistance);
    }

    @Override
    public void resetEffect(VillagerLevel villager, AlphaPlayer player) {
        player.setResistanceModifier(SurviConfig.getInstance().getNormalResistanceModifier());
    }
}