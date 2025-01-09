package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import org.bukkit.attribute.Attribute;

import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaLife {
    private final AlphaPlayer alphaPlayer;
    private int maxHealthEffectLife = 2;
    private int successLife = 0;
    private int deathLife = 0;
    private int maxLife;

    public AlphaLife(AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public void actualize(float attributeValue) {
        double oldHealth = this.alphaPlayer.getPlayer().getHealth();
        double oldMaxHealth = this.maxLife;

        int deathWithDispel = max(0, this.deathLife - GameManager.getInstance().getDispel());
        double baseLife = this.maxHealthEffectLife + this.successLife - deathWithDispel;
        this.maxLife = (int) Math.round(baseLife * attributeValue);

        Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH)).setBaseValue(this.maxLife);

        if (!alphaPlayer.getPlayer().isDead())
            this.alphaPlayer.getPlayer().setHealth(min(max(1, (oldHealth * this.maxLife) / oldMaxHealth), this.maxLife));
    }

    public void setSuccess(int success) {
        this.successLife = floor((double) success);
    }

    public void setDeath(int death) {
        this.deathLife = floor((double) death / 10f);
    }

    public void setMaxHealthBonus(int maxHealthBonus) {
        this.maxHealthEffectLife = maxHealthBonus;
    }
}
