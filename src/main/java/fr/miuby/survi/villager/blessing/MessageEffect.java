package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageEffect extends BlessingEffect {
    private final String message;

    @Override
    public void applyEffect(AlphaPlayer player) {
        //Bukkit.broadcast(Component.text(message));
    }
}
