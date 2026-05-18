package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;

public class FlyEffect extends BlessingEffect {
    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.getPlayer().setFlying(true);
    }
}
