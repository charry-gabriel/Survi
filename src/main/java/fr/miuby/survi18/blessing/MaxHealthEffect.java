package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import org.bukkit.attribute.Attribute;

import java.util.Objects;

public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    public MaxHealthEffect(int health) {
        maxHealth = health;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        Objects.requireNonNull(player.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
    }
}
