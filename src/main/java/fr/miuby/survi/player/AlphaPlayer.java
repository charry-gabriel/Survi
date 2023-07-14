package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Math.max;
import static org.bukkit.util.NumberConversions.floor;

public class AlphaPlayer implements Serializable {
    private final UUID uuid;
    private String pseudo;
    private int mort = 0;
    private int malus = 0;
    private int success = 0;

    private Player player;

    private float resistance = 0.2f;
    private float damage = 0.2f;
    private final float endResistance = 0.7f;
    private final float endDamage = 0.7f;
    private int vieBonus = 10;
    private int vieBonusSuccess = 0;

    private Role role;

    private final AlphaScoreboard scoreboard;
    private Monde world;

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;
        scoreboard = new AlphaScoreboard();
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

        updateLife();
        GameManager.getInstance().getDatabase().updatePlayer(uuid, "mort", this.mort);
    }

    public void addSuccess(int success) {
        this.success = success;
        vieBonusSuccess = floor((double) success / 3f);

        GameManager.getInstance().getDatabase().updatePlayer(uuid, "success", this.success);
    }

    public void updateLife() {
        vieBonusSuccess = floor((double) success / 3f);
        malus = floor((double) mort / 10f);
        int vieEnMoins = max(0, malus - GameManager.getInstance().getDispel());

        if (role.getType() == ERole.MAIRE) {
            if (getWorld() == EWorld.VILLAGE) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 1.5f);
            } else if(getWorld() == EWorld.END || getWorld() == EWorld.END2) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.5f);
            } else {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.75f);
            }
        } else {
            if(getWorld() == EWorld.END || getWorld() == EWorld.END2) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.5f);
            } else {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(vieBonus + vieBonusSuccess - vieEnMoins);
            }
        }
    }

    public void switchWorld() {
        this.world = Monde.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);
        updateLife();
    }

    public void teleport(Monde monde) {
        getPlayer().teleport(monde.getSpawnPoint());
    }

    //region Getters Setters
    public AlphaScoreboard getScoreboard() {
        return scoreboard;
    }

    public EWorld getWorld() {
        return this.world.getType();
    }

    public UUID getUUID(){
        return uuid;
    }

    public int getMort(){
        return mort;
    }

    public int getSuccess(){
        return success;
    }

    public String getPseudo(){
        return pseudo;
    }

    public Player getPlayer(){
        return player;
    }

    public void resetPlayer(){
        player = null;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    public float getResistance() {
        return resistance;
    }

    public float getEndResistance() {
        return endResistance;
    }

    public float getDamage() {
        return damage;
    }

    public float getEndDamage() {
        return endDamage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setVieBonus(int vieBonus) {
        this.vieBonus = vieBonus;
    }

    public Role getRole() {
        return role;
    }

    public void setMort(int mort) {
        this.mort = mort;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public void setRole(String roleName) {
        role = Role.get(roleName);
    }
    //endregion
}
