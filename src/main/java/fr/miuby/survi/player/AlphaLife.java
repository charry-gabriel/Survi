package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.world.EWorld;
import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaLife {
    private final AlphaPlayer alphaPlayer;
    private int maxHealthEffectLife = 2;
    private int successLife = 0;
    private int deathLife = 0;
    private List<RoleAttribute> worldRoleModifiers;
    private int maxLife;

    public AlphaLife(AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public void actualize() {
        for(RoleAttribute roleAttribute : worldRoleModifiers) {
            if (roleAttribute.attributeType() == Attribute.MAX_HEALTH) {
                double oldHealth = this.alphaPlayer.getPlayer().getHealth();
                double oldMaxHealth = this.maxLife;

                int deathWithDispel = max(0, this.deathLife - GameManager.getInstance().getDispel());
                double baseLife = this.maxHealthEffectLife + this.successLife - deathWithDispel;
                this.maxLife = (int) Math.round(baseLife * roleAttribute.attribute());

                Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH)).setBaseValue(this.maxLife);
                this.alphaPlayer.getPlayer().setHealth(min(max(1, (oldHealth * this.maxLife) / oldMaxHealth), this.maxLife));
            } else {
                Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(roleAttribute.attributeType())).setBaseValue(roleAttribute.attribute());
            }
        }

        if (alphaPlayer.hasArmorMalus())
            Objects.requireNonNull(this.alphaPlayer.getPlayer().getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
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
        List<RoleAttribute> foundAttributes = new ArrayList<>();
        for (RoleAttribute attribute : this.alphaPlayer.getRole().attributes()) {
            if ((this.alphaPlayer.getWorld() == attribute.world() || attribute.world() == EWorld.ALL))
                foundAttributes.add(attribute);
        }
        this.worldRoleModifiers = foundAttributes;
        actualize();
    }
}
