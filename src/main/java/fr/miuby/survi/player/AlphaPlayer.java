package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.life.AlphaLife;
import fr.miuby.survi.role.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.UUID;

public class AlphaPlayer implements Serializable {
    private final UUID uuid;
    private Player player;
    private Role role;
    private final AlphaLife alphaLife;
    private final AlphaScoreboard scoreboard;
    private Monde world;
    private int mort = 0;
    private int success = 0;
    private boolean isTakingNoDamage;
    private boolean hasArmorMalus;

    //region Modifier
    private float resistanceModifier = 0.2f;
    private float damageModifier = 0.2f;
    private final float endResistanceModifier = 0.7f;
    private final float endDamageModifier = 0.7f;
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

        GameManager.getInstance().getDatabase().updatePlayer(uuid, "mort", this.mort);
    }

    public void addSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);

        GameManager.getInstance().getDatabase().updatePlayer(uuid, "success", this.success);
    }

    public void switchWorld() {
        this.world = Monde.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);
        alphaLife.setWorldRole();
    }

    public void teleport(Monde monde) {
        getPlayer().teleport(monde.getSpawnPoint());
    }

    public void switchRole() {
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);
        alphaLife.setWorldRole();
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
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public void setRole(String roleName) {
        this.role = Role.get(roleName);
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
    //endregion
}
