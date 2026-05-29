package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DispelEffect extends BlessingEffect{
    private final int dispel;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        GameManager.getInstance().setDispel(dispel);
        player.getAlphaLife().actualizeDeath();
    }

    @Override
    public void resetEffect(VillagerLevel villager, AlphaPlayer player) {
        GameManager.getInstance().setDispel(0);
        player.getAlphaLife().actualizeDeath();
    }
}