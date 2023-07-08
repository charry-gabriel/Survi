package fr.miuby.survi;

import fr.miuby.survi.role.*;
import fr.miuby.survi.village.VillagerLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

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

    private Score mortScore;
    private Score successScore;
    private Scoreboard scoreboard;

    private float resistance;
    private float damage;
    private final float endResistance = 0.7f;
    private final float endDamage = 0.7f;
    private int vieBonus = 10;
    private int vieBonusSuccess = 0;

    private Role role;

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public void newScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        /*Objective objectifInfo = scoreboard.registerNewObjective("Info", Criteria.DUMMY, Component.text("Info"));
        objectifInfo.setDisplaySlot(DisplaySlot.SIDEBAR);
        successScore = objectifInfo.getScore("Succès :");
        successScore.setScore(success);*/

        Objective life = scoreboard.registerNewObjective("Vie", Criteria.HEALTH, Component.text("Vie"));
        life.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        life.setRenderType(RenderType.HEARTS);

        createTeam();
    }

    public void createTeam(){
        Team village = scoreboard.registerNewTeam("Village");
        village.color(NamedTextColor.AQUA);
        village.prefix(Component.text("Village - ").append(Component.text("[Simplet] ").color(NamedTextColor.GRAY)));
        Team wilderness = scoreboard.registerNewTeam("Wilderness");
        wilderness.color(NamedTextColor.GOLD);
        wilderness.prefix(Component.text("Wilderness - ").append(Component.text("[Simplet] ").color(NamedTextColor.GRAY)));
        Team nether = scoreboard.registerNewTeam("Nether");
        nether.color(NamedTextColor.RED);
        nether.prefix(Component.text("Nether - ").append(Component.text("[Simplet] ").color(NamedTextColor.GRAY)));
        Team end = scoreboard.registerNewTeam("End");
        end.color(NamedTextColor.YELLOW);
        end.prefix(Component.text("End - ").append(Component.text("[Simplet] ").color(NamedTextColor.GRAY)));
    }

    public void actualize() {
        if(player == null)
            player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);

        if(player != null) {
            if(scoreboard == null) {
                newScoreboard();
            }

            player.setScoreboard(scoreboard);
            GameManager.getInstance().switchWorld(player.getWorld().getName(), pseudo);

            for(AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                if(alphaPlayer.getPlayer() != null) {
                    switch (alphaPlayer.getPlayer().getWorld().getName()) {
                        case "Village":
                            Objects.requireNonNull(this.getScoreboard().getTeam("Village")).addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness":
                            Objects.requireNonNull(this.getScoreboard().getTeam("Wilderness")).addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness_nether":
                            Objects.requireNonNull(this.getScoreboard().getTeam("Nether")).addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness_the_end":
                            Objects.requireNonNull(this.getScoreboard().getTeam("End")).addEntry(alphaPlayer.getPseudo());
                            break;
                    }
                }
            }

            resistance = 0.2f;
            damage = 0.2f;
            vieBonusSuccess = floor((double) success / 3f);

            for (VillagerLevel villager : GameManager.getInstance().getVillage().getVillagersLevel().values()) {
                villager.ApplyAllCurrentBlessing(this);
            }

            updateLife();
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

    public Scoreboard getScoreboard(){
        return scoreboard;
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

    public void updateLife() {
        malus = floor((double) mort / 10f);
        int vieEnMoins = max(0, malus - GameManager.getInstance().getDispel());

        if (role.getName().equals("MaireRole")) {
            if (getPlayer().getWorld().getName().equals("Village")) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 2f);
            } else if(getPlayer().getWorld().getName().equals("Wilderness_the_end") || getPlayer().getWorld().getName().equals("Wilderness_the_end2")) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.25f);
            } else {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.5f);
            }
        } else {
            if(getPlayer().getWorld().getName().equals("Wilderness_the_end") || getPlayer().getWorld().getName().equals("Wilderness_the_end2")) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue((vieBonus + vieBonusSuccess - vieEnMoins) * 0.5f);
            } else {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(vieBonus + vieBonusSuccess - vieEnMoins);
            }
        }
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

    public void setRole(String role) {
        switch (role) {
            case "Simplet":
                this.role = new SimpletRole();
                break;
            case "Maire":
                this.role = new MaireRole();
                break;
            case "Couple":
                this.role = new CoupleRole();
                break;
            case "Doctor":
                this.role = new DoctorRole();
                break;
            case "Riche":
                this.role = new RicheRole();
                break;
        }
    }
}
