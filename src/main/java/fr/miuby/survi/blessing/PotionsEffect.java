package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.potion.PotionEffect;

public class PotionsEffect extends BlessingEffect {
    private final PotionEffect potion;

    public PotionsEffect(PotionEffect potion) {
        this.potion = potion;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().addPotionEffect(potion);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        player.getPlayer().removePotionEffect(potion.getType());
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}