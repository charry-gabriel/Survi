package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class MessageEffect extends BlessingEffect {
    private final String message;

    @Override
    public void applyEffect(AlphaPlayer player) {
        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage(Component.text(message));
    }

    @Override
    public boolean isOneShot() { return true; }
}
