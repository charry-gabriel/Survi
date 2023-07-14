package fr.miuby.survi.player.life;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.attribute.Attribute;

import java.util.Objects;

import static java.lang.Math.max;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaLife {
    private final AlphaPlayer alphaPlayer;
    private int maxHealthEffectLife = 10;
    private int successLife = 0;
    private int deathLife = 0;

    public AlphaLife(AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public void actualize() {
        int deathWithDispel = max(0, deathLife - GameManager.getInstance().getDispel());
        double baseLife = maxHealthEffectLife + successLife - deathWithDispel;
        float modifier = GameManager.getInstance().getLifeFactory().getLifeModifier(alphaPlayer.getWorld(), alphaPlayer.getRole().getType());

        Objects.requireNonNull(alphaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(baseLife * modifier);
    }

    public void setSuccess(int success) {
        this.successLife = floor((double) success / 3f);
        actualize();
    }

    public void setDeath(int death) {
        this.deathLife = floor((double) death / 10f);
        actualize();
    }

    public void setMaxHealthBonus(int maxHealthBonus) {
        this.maxHealthEffectLife = maxHealthBonus;
        actualize();
    }
}
