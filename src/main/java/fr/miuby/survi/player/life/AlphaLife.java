package fr.miuby.survi.player.life;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.attribute.Attribute;

import java.util.List;
import java.util.Objects;

import static java.lang.Math.max;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaLife {
    private final AlphaPlayer alphaPlayer;
    private int maxHealthEffectLife = 10;
    private int successLife = 0;
    private int deathLife = 0;
    private List<AttributeModifier> worldRoleModifiers;
    private int maxLife;

    public AlphaLife(AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public void actualize() {
        if (this.worldRoleModifiers.isEmpty())
            return;

        for(AttributeModifier attributeModifier : worldRoleModifiers) {
            if (attributeModifier.getAttributeType() == Attribute.MAX_HEALTH) {
                int deathWithDispel = max(0, this.deathLife - GameManager.getInstance().getDispel());
                double baseLife = this.maxHealthEffectLife + this.successLife - deathWithDispel;
                this.maxLife = (int) Math.round(baseLife * attributeModifier.getAttributeModifier());

                Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH)).setBaseValue(this.maxLife);
            } else {
                Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(attributeModifier.getAttributeType())).setBaseValue(attributeModifier.getAttributeModifier());
            }
        }
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

    public void setWorldRole() {
        double missingLife = this.maxLife - this.alphaPlayer.getPlayer().getHealth();

        this.worldRoleModifiers = GameManager.getInstance().getLifeFactory().getAttributeModifier(this.alphaPlayer.getWorld(), this.alphaPlayer.getRole().getType());
        actualize();

        if (missingLife > 0)
            this.alphaPlayer.getPlayer().setHealth(max(1, this.maxLife - missingLife));
    }
}
