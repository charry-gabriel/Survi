package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageEffect extends BlessingEffect {
    private final String message;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        //Bukkit.broadcast(Component.text(message));
    }
}
