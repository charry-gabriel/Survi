package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.*;

public class AlphaPlayer implements Serializable {
    private final UUID uuid;
    private Player player;
    private Role role;
    private final List<Role> subRoles = new ArrayList<>();
    private final AlphaLife alphaLife;
    private final AlphaScoreboard scoreboard;
    private Monde world;
    private int mort = 0;
    private int success = 0;
    private boolean isTakingNoDamage;
    private boolean hasArmorMalus;
    private String pseudo;

    //region Modifier
    private float resistanceModifier = 0.2f;
    private float damageModifier = 0.2f;
    private final float endResistanceModifier = 0.7f;
    private final float endDamageModifier = 0.7f;
    private List<RoleAttribute> worldRoleAttribute;
    //endregion

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;
        scoreboard = new AlphaScoreboard();
        alphaLife = new AlphaLife(this);
    }

    public static AlphaPlayer get(UUID uuid) {
        return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
    }

    public void joinServer() {
        if(player == null)
            player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);

        if(player != null) {
            player.setScoreboard(scoreboard.getScoreboard());
            switchWorld();
            GameManager.getInstance().getVillagerFactory().applyAllCurrentBlessing(this);
            GameManager.getInstance().getAlphaPlayerFactory().setPlayersToTeam(scoreboard);

            player.discoverRecipes(GameManager.getInstance().getCustomItemFactory().getNewRecipes().keySet());
            //retroaction, pas besoin sur un nouveau serveur
            player.undiscoverRecipes(GameManager.getInstance().getCustomItemFactory().getOldRecipes());
        }
    }

    public void gainOneSuccess(boolean challenge) {
        if(challenge) {
            success++;
            addSuccess(success);
        }
    }

    public void addMort(int mort) {
        this.mort += mort;
        this.alphaLife.setDeath(this.mort);

        GameManager.getInstance().getDatabase().updatePlayer(uuid, "mort", String.valueOf(this.mort));
    }

    public void addSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);
        this.actualizeAttribute();

        GameManager.getInstance().getDatabase().updatePlayer(uuid, "success", String.valueOf(this.success));
    }

    public void switchWorld() {
        this.world = Monde.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);
        this.setWorldRole();
    }

    public void teleport(Monde monde) {
        if (getPlayer() != null)
            getPlayer().teleport(monde.getSpawnPoint());
    }

    public void switchRole() {
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);
        this.setWorldRole();
    }

    public void setWorldRole() {
        List<RoleAttribute> foundAttributes = new ArrayList<>();
        for (RoleAttribute attribute : GameManager.getInstance().getRoleFactory().defaultAttributes()) {
            attribute.setRole("default");
            foundAttributes.add(attribute);
        }

        for (RoleAttribute attribute : this.getRole().attributes()) {
            if ((this.getWorld() == attribute.getWorld() || attribute.getWorld() == EWorld.ALL)) {
                attribute.setRole(this.getRole().roleId());
                foundAttributes.add(attribute);
            }
        }

        for (Role role : this.getSubRoles()) {
            for (RoleAttribute attribute : role.attributes()) {
                if ((this.getWorld() == attribute.getWorld() || attribute.getWorld() == EWorld.ALL)) {
                    attribute.setRole(role.roleId());
                    foundAttributes.add(attribute);
                }
            }
        }

        this.worldRoleAttribute = foundAttributes;
        actualizeAttribute();
    }

    public void actualizeAttribute() {
        for(RoleAttribute roleAttribute : worldRoleAttribute) {
            if(roleAttribute.getName() == null)
                continue;

            if (roleAttribute.getAttributeType() == Attribute.MAX_HEALTH) {
                this.alphaLife.actualize(roleAttribute.getValue());
            } else {
                AttributeInstance playerAttribute = this.getPlayer().getAttribute(roleAttribute.getAttributeType());
                if (playerAttribute == null)
                    continue;

                AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), roleAttribute.getName()));

                // Already exist
                if (attributeModifier != null)
                    playerAttribute.removeModifier(attributeModifier);

                //TODO: Remove the default after 1.21.4
                if (roleAttribute.getOperation() == RoleAttribute.Operation.REMOVE)
                    playerAttribute.setBaseValue(roleAttribute.getValue());
                else
                    playerAttribute.addTransientModifier(roleAttribute.createAttributeModifier());
            }
        }

        if (hasArmorMalus())
            Objects.requireNonNull(this.getPlayer().getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
    }

    //region Getters Setters
    public AlphaScoreboard getScoreboard() {
        return this.scoreboard;
    }

    public EWorld getWorld() {
        return this.world.getType();
    }

    public UUID getUUID(){
        return this.uuid;
    }

    public int getMort(){
        return this.mort;
    }

    public int getSuccess(){
        return this.success;
    }

    public Player getPlayer(){
        return this.player;
    }

    public void resetPlayer(){
        this.player = null;
    }

    public void setResistanceModifier(float modifier) {
        this.resistanceModifier = modifier;
    }

    public float getResistanceModifier() {
        return this.resistanceModifier;
    }

    public float getEndResistanceModifier() {
        return this.endResistanceModifier;
    }

    public float getDamageModifier() {
        return this.damageModifier;
    }

    public float getEndDamageModifier() {
        return this.endDamageModifier;
    }

    public void setDamageModifier(float modifier) {
        this.damageModifier = modifier;
    }

    public Role getRole() {
        return this.role;
    }

    public void setMort(int mort) {
        this.mort = mort;
        this.alphaLife.setDeath(mort);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AlphaLife getAlphaLife() {
        return this.alphaLife;
    }

    public boolean isTakingNoDamage() {
        return isTakingNoDamage;
    }

    public void setTakingNoDamage(boolean takeNoDamage) {
        this.isTakingNoDamage = takeNoDamage;
    }

    public boolean hasArmorMalus() {
        return hasArmorMalus;
    }

    public void setArmorMalus(boolean hasArmorMalus) {
        this.hasArmorMalus = hasArmorMalus;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public List<Role> getSubRoles() {
        return subRoles;
    }

    public void addSubRole(Role role) {
        Role removeRole = null;
        for (Role subRole : subRoles) {
            if (subRole.roleId().equals(role.roleId()))
                removeRole = subRole;
        }
        subRoles.remove(removeRole);
        subRoles.add(role);
    }

    public void removeSubRole(Role role) {
        subRoles.remove(role);
    }
    //endregion
}
