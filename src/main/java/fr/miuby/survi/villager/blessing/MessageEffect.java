package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageEffect extends BlessingEffect {
    private final String message;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        //Bukkit.broadcast(Component.text(message));
    }
}
