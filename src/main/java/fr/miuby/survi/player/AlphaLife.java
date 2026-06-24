package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

import static java.lang.Math.min;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaLife {
    private final AlphaPlayer alphaPlayer;

    private int successLife = 0;
    private int deathLife = 0;
    private double blessingLife = 2;
    private boolean hasArmorMalus;
    private AttributeInstance attributeInstance;

    private final NamespacedKey deathKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "death_life");
    private final NamespacedKey successKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "success_life");
    private final NamespacedKey blessingKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "blessing_life");

    public AlphaLife(AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public void regenHealth(IHealthAttribute healthAttribute) {
        if (this.alphaPlayer.getPlayer() == null)
            return;

        this.attributeInstance = this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH);
        if (attributeInstance == null)
            return;

        double oldMaxHealth = attributeInstance.getValue();

        healthAttribute.changeHealthAttribute();

        if (!this.alphaPlayer.getPlayer().isDead())
            this.alphaPlayer.getPlayer().setHealth(Math.clamp((this.alphaPlayer.getPlayer().getHealth() * this.attributeInstance.getValue()) / oldMaxHealth, 1, this.attributeInstance.getValue()));
    }

    public void actualizeDeath() {
        this.regenHealth(() -> {
            int deathWithDispel = min(0, GameManager.getInstance().getDispel() - this.deathLife);
            if (attributeInstance.getModifier(deathKey) != null)
                attributeInstance.removeModifier(deathKey);
            AttributeModifier deathModifier = new AttributeModifier(deathKey, deathWithDispel, AttributeModifier.Operation.ADD_NUMBER);
            attributeInstance.addTransientModifier(deathModifier);
        });

        if (hasArmorMalus) {
            if (attributeInstance.getModifier(deathKey) != null)
                attributeInstance.removeModifier(deathKey);
            AttributeModifier deathModifier = new AttributeModifier(deathKey, -999f, AttributeModifier.Operation.ADD_NUMBER);
            attributeInstance.addTransientModifier(deathModifier);

            if (!this.alphaPlayer.getPlayer().isDead())
                this.alphaPlayer.getPlayer().setHealth(0.01f);
        }
    }

    public void actualizeSuccess() {
        this.regenHealth(() -> {
            if (attributeInstance.getModifier(successKey) != null)
                attributeInstance.removeModifier(successKey);
            AttributeModifier successModifier = new AttributeModifier(successKey, this.successLife, AttributeModifier.Operation.ADD_NUMBER);
            attributeInstance.addTransientModifier(successModifier);
        });
    }

    public void setSuccess(int success) {
        this.successLife = success;

        this.actualizeSuccess();
    }

    public void setDeath(int death) {
        this.deathLife = floor((double) death / SurviConfig.getInstance().getDeathLifeDivisor());
        actualizeDeath();
    }

    public void setBlessing(int blessing) {
        this.blessingLife = blessing;
        if (this.alphaPlayer.getPlayer() == null) return;
        // Rafraîchit l'instance en cas de référence périmée (reconnexion avant le 1er regenHealth).
        this.attributeInstance = this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH);
        if (this.attributeInstance == null) return;
        if (attributeInstance.getModifier(blessingKey) != null)
            attributeInstance.removeModifier(blessingKey);
        AttributeModifier blessingModifier = new AttributeModifier(blessingKey, this.blessingLife, AttributeModifier.Operation.ADD_NUMBER);
        attributeInstance.addTransientModifier(blessingModifier);
    }

    public boolean isArmorMalus() {
        return this.hasArmorMalus;
    }

    public void setArmorMalus(boolean hasArmorMalus) {
        this.hasArmorMalus = hasArmorMalus;
        this.actualizeDeath();
    }
}