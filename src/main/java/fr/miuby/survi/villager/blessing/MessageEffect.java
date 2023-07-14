package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;

public class MessageEffect extends BlessingEffect {
    private final String message;

    public MessageEffect(String message) {
        this.message = message;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        //Bukkit.broadcast(Component.text(message));
    }
}
