package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;

public class FlyEffect extends BlessingEffect {
    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.getPlayer().setFlying(true);
    }
}
