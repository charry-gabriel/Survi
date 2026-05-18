package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class LockVillagerEffect extends BlessingEffect {
    private final Duration duration;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        villager.lock(duration);
    }
}
