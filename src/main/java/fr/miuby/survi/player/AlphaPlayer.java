package fr.miuby.survi.player;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.database.PlayerColumn;
import fr.miuby.survi.role.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.Serializable;
import java.util.*;

public class AlphaPlayer implements Serializable {
    @Getter
    private final UUID uuid;
    @Getter
    private final AlphaScoreboard scoreboard;
    @Getter
    private final AlphaLife alphaLife;

    @Getter
    private final List<Role> subRoles = new ArrayList<>();

    @Setter
    private MLWorld world;
    @Getter
    private int mort = 0;
    @Getter
    private int success = 0;

    @Setter
    @Getter
    private boolean isTakingNoDamage;
    @Setter
    @Getter
    private String pseudo;
    @Setter
    @Getter
    private Player player;
    @Setter
    @Getter
    private Role role;

    //region Modifier
    @Getter
    @Setter
    private float resistanceModifier = 0.2f;
    @Setter
    @Getter
    private float damageModifier = 0.2f;
    @Getter
    private final float endResistanceModifier = 0.7f;
    @Getter
    private final float endDamageModifier = 0.7f;
    //endregion

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;
        this.scoreboard = new AlphaScoreboard();
        this.alphaLife = new AlphaLife(this);
    }

    public static AlphaPlayer get(UUID uuid) {
        return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
    }

    public void joinServer() {
        if(this.player == null)
            this.player = GameManager.getInstance().getPlugin().getServer().getPlayer(this.uuid);

        if(this.player != null) {
            this.player.setScoreboard(this.scoreboard.getScoreboard());

            this.world = WorldRegistry.get(getPlayer().getWorld().getUID());
            GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);

            GameManager.getInstance().getVillagerFactory().applyAllCurrentBlessing(this);
            GameManager.getInstance().getAlphaPlayerFactory().setPlayersToTeam(this.scoreboard);

            this.addRoleAttribute();
            this.getAlphaLife().actualizeDeath();
            this.getAlphaLife().actualizeSuccess();

            this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());
        }
    }

    public void addRoleAttribute() {
        for (RoleAttribute attribute : this.getRole().attributes()) {
            if ((this.getWorld() == attribute.getWorld() || attribute.getWorld() == EWorld.ALL)) {
                attribute.setRole(this.getRole().roleId());

                if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                    this.getAlphaLife().regenHealth(() -> this.addAttribute(attribute));
                else
                    this.addAttribute(attribute);
            }
        }

        for (Role role : this.getSubRoles()) {
            for (RoleAttribute attribute : role.attributes()) {
                if ((this.getWorld() == attribute.getWorld() || attribute.getWorld() == EWorld.ALL)) {
                    attribute.setRole(role.roleId());

                    if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                        this.getAlphaLife().regenHealth(() -> this.addAttribute(attribute));
                    else
                        this.addAttribute(attribute);
                }
            }
        }
    }

    public void gainOneSuccess(boolean challenge) {
        if(challenge) {
            this.success++;
            this.addSuccess(this.success);
        }
    }

    public void addMort(int mort) {
        this.mort += mort;
        this.alphaLife.setDeath(this.mort);

        GameManager.getInstance().getDatabase().updatePlayer(this.uuid, PlayerColumn.MORT, String.valueOf(this.mort));
    }

    public void addSuccess(int success) {
        this.success = success;
        this.getAlphaLife().regenHealth(() -> this.getAlphaLife().setSuccess(success));

        GameManager.getInstance().getDatabase().updatePlayer(this.uuid, PlayerColumn.SUCCESS, String.valueOf(this.success));
    }

    public void teleport(MLWorld monde) {
        if (getPlayer() != null)
            getPlayer().teleport(monde.getSpawnPoint());
    }

    public void addAttribute(RoleAttribute roleAttribute) {
        if(roleAttribute.getName() == null)
            return;

        AttributeInstance playerAttribute = this.getPlayer().getAttribute(roleAttribute.getAttributeType());
        if (playerAttribute == null)
            return;

        AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), roleAttribute.getName()));

        // If already exist
        if (attributeModifier != null)
            playerAttribute.removeModifier(attributeModifier);

        //TODO: Remove the default after 1.21.4
        if (roleAttribute.getOperation() == RoleAttribute.Operation.REMOVE)
            playerAttribute.setBaseValue(roleAttribute.getValue());
        else
            playerAttribute.addTransientModifier(roleAttribute.createAttributeModifier());

        if (roleAttribute.getAttributeType() == Attribute.MAX_ABSORPTION) {
            this.getPlayer().removePotionEffect(PotionEffectType.ABSORPTION);
            this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 0, (int) roleAttribute.getValue()));
        }
    }

    //region Getters Setters
    public EWorld getWorld() {
        return (EWorld) this.world.getType();
    }

    public void resetPlayer(){
        this.player = null;
    }

    public void setMort(int mort) {
        this.mort = mort;
        this.alphaLife.setDeath(mort);
    }

    public void setSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);
    }

    public void addSubRole(Role role) {
        Role removeRole = null;
        for (Role subRole : this.subRoles) {
            if (subRole.roleId().equals(role.roleId()))
                removeRole = subRole;
        }
        this.subRoles.remove(removeRole);
        this.subRoles.add(role);
    }

    public void removeSubRole(Role role) {
        this.subRoles.remove(role);
    }
    //endregion
}
