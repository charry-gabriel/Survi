package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class MessageEffect extends BlessingEffect {
    private String message;

    public MessageEffect(String message) {
        this.message = message;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        //Bukkit.broadcast(Component.text(message));
    }
}
