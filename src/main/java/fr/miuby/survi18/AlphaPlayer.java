package fr.miuby.survi18;

import fr.miuby.survi18.database.DbConnection;
import fr.miuby.survi18.role.Role;
import fr.miuby.survi18.village.VillagerLevel;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Math.min;
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
    private int vieBonus = 0;
    private int vieBonusSuccess = 0;

    private Role role;

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;

        Bukkit.getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
                    final DbConnection dbConnection = GameManager.getInstance().getDatabaseManager().getDbConnection();
                    try {
                        final Connection connection = dbConnection.getConnection();
                        final PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, mort, success, progres, pseudo FROM player WHERE uuid = ?");
                        preparedStatement.setString(1, uuid.toString());
                        final ResultSet resultSet = preparedStatement.executeQuery();

                        if (resultSet.next()) {
                            mort = resultSet.getInt("mort");
                            success = resultSet.getInt("success");
                            pseudo = resultSet.getString("pseudo");
                            Bukkit.getScheduler().runTask(GameManager.getInstance().getPlugin(), this::actualize);
                        } else {
                            player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);
                            //noinspection ConstantConditions
                            pseudo = player.getName();
                            CreateDBPlayer(connection);
                            Bukkit.getScheduler().runTask(GameManager.getInstance().getPlugin(), this::actualize);
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                });
    }

    public void CreateDBPlayer(Connection connection) {
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO player VALUES (?, 0, 0, 0, 0, ?)");
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, pseudo);
            preparedStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
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

            /*mort = player.getStatistic(Statistic.DEATHS);
            mortScore.setScore(mort);*/
            player.setScoreboard(scoreboard);
            GameManager.getInstance().switchWorld(player.getWorld().getName(), pseudo);

            for(AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                if(alphaPlayer.getPlayer() != null) {
                    switch (alphaPlayer.getPlayer().getWorld().getName()) {
                        case "Village":
                            this.getScoreboard().getTeam("Village").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness":
                            this.getScoreboard().getTeam("Wilderness").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness_nether":
                            this.getScoreboard().getTeam("Nether").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "Wilderness_the_end":
                            this.getScoreboard().getTeam("End").addEntry(alphaPlayer.getPseudo());
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
            setSuccess(success);
        }
    }

    public void gainOldAdvancement(Player player) {
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();
        while(advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
            AdvancementDisplay advancementDisplay = advancement.getDisplay();
            String categorie = advancement.getKey().getKey().split("/")[0];
            if(advancementProgress.isDone() && advancementDisplay != null && !categorie.equals("recipes")) {
                if(advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                    success++;
                }
            }
        }
        setSuccess(success);
    }

    public void addMort(int mort) {
        this.mort += mort;

        updateLife();
        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "mort", this.mort);
    }

    public void setSuccess(int success) {
        this.success = success;
        /*if(successScore != null)
            successScore.setScore(this.success);*/
        vieBonusSuccess = floor((double) success / 3f);

        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "success", this.success);
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

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setVieBonus(int vieBonus) {
        this.vieBonus = vieBonus;
    }

    public void updateLife() {
        malus = floor((double) mort / 10f);
        int vieEnMoins = min(0, malus - GameManager.getInstance().getDispel());
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(10 + vieBonus + vieBonusSuccess - vieEnMoins);
    }
}
