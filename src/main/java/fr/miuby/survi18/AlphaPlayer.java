package fr.miuby.survi18;

import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.blessing.BlessingEffect;
import fr.miuby.survi18.database.DbConnection;
import fr.miuby.survi18.village.VillagerLevel;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
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

public class AlphaPlayer implements Serializable {
    private UUID uuid;
    private String pseudo;
    private int coins = 0;
    private int mort = 0;
    private int success = 0;
    private int progres = 0;

    private Player player;

    private Score mortScore;
    private Score coinScore;
    private Score progresScore;
    private Score successScore;
    private Scoreboard scoreboard;

    public AlphaPlayer(UUID uuid) {
        this.uuid = uuid;

        Bukkit.getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
                    final DbConnection dbConnection = GameManager.getInstance().getDatabaseManager().getDbConnection();
                    try {
                        final Connection connection = dbConnection.getConnection();
                        final PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, coins, mort, success, progres, pseudo FROM player WHERE uuid = ?");
                        preparedStatement.setString(1, uuid.toString());
                        final ResultSet resultSet = preparedStatement.executeQuery();

                        if (resultSet.next()) {
                            coins = resultSet.getInt("coins");
                            mort = resultSet.getInt("mort");
                            success = resultSet.getInt("success");
                            progres = resultSet.getInt("progres");
                            pseudo = resultSet.getString("pseudo");
                            Bukkit.getScheduler().runTask(GameManager.getInstance().getPlugin(), this::actualize);
                        } else {
                            player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);
                            //noinspection ConstantConditions
                            pseudo = player.getName();
                            CreatePlayer(connection);
                            Bukkit.getScheduler().runTask(GameManager.getInstance().getPlugin(), this::actualize);
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                });
    }

    public void CreatePlayer(Connection connection) {
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

        Objective objectifInfo = scoreboard.registerNewObjective("Info", "dummy");
        objectifInfo.setDisplaySlot(DisplaySlot.SIDEBAR);

        coinScore = objectifInfo.getScore("AlphaCoins :");
        coinScore.setScore(coins);

        progresScore = objectifInfo.getScore("Progrès :");
        progresScore.setScore(progres);

        successScore = objectifInfo.getScore("Succès :");
        successScore.setScore(success);

        mortScore = objectifInfo.getScore("Mort :");
        mortScore.setScore(mort);


        Objective life = scoreboard.registerNewObjective("Vie", Criterias.HEALTH);
        life.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        life.setRenderType(RenderType.HEARTS);

        createTeam();
    }

    public void createTeam(){
        Team village = scoreboard.registerNewTeam("Village");
        village.setColor(ChatColor.GREEN);
        village.setPrefix("Village - ");
        Team wilderness = scoreboard.registerNewTeam("Wilderness");
        wilderness.setColor(ChatColor.GOLD);
        wilderness.setPrefix("Wilderness - ");
        Team nether = scoreboard.registerNewTeam("Nether");
        nether.setColor(ChatColor.RED);
        nether.setPrefix("Nether - ");
        Team end = scoreboard.registerNewTeam("End");
        end.setColor(ChatColor.AQUA);
        end.setPrefix("End - ");
    }

    public void actualize() {
        if(player == null)
            player = GameManager.getInstance().getPlugin().getServer().getPlayer(uuid);

        if(player != null) {
            if(scoreboard == null) {
                newScoreboard();
            }

            mort = player.getStatistic(Statistic.DEATHS);
            mortScore.setScore(mort);
            player.setScoreboard(scoreboard);
            GameManager.getInstance().switchWorld(player.getWorld().getName(), pseudo);

            for(AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                if(alphaPlayer.getPlayer() != null) {
                    switch (alphaPlayer.getPlayer().getWorld().getName()) {
                        case "village":
                            this.getScoreboard().getTeam("Village").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "wilderness":
                            this.getScoreboard().getTeam("Wilderness").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "wilderness_nether":
                            this.getScoreboard().getTeam("Nether").addEntry(alphaPlayer.getPseudo());
                            break;
                        case "wilderness_the_end":
                            this.getScoreboard().getTeam("End").addEntry(alphaPlayer.getPseudo());
                            break;
                    }
                }
            }

            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(10);

            for (VillagerLevel villager : GameManager.getInstance().getVillage().getVillagers().values()) {
                for (Blessing blessing : villager.getCurrentBlessings()) {
                    for (BlessingEffect effect : blessing.getBlessingEffects()) {
                        effect.ApplyEffect(this);
                    }
                }
            }
        }
    }

    public void gainOneSuccess(boolean challenge) {
        int result;
        if(challenge) {
            success++;
            result = success * 1500000;
            setSuccess(success);
        } else {
            progres++;
            if(progres <= 20)
                result = progres * progres * 10;
            else if(progres <= 40)
                result = progres * progres * 100;
            else if(progres <= 70)
                result = progres * progres * 1000;
            else
                result = progres * progres * 10000;
            setProgres(progres);
        }
        addCoins(result);
    }

    public void gainOldAdvancement(Player player) {
        int result = 0;
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();
        while(advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
            AdvancementDisplay advancementDisplay = advancement.getDisplay();
            String categorie = advancement.getKey().getKey().split("/")[0];
            if(advancementProgress.isDone() && advancementDisplay != null && !categorie.equals("recipes")) {
                if(advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                    success++;
                    result += success * 1500000;
                } else if(advancementDisplay.frame() == AdvancementDisplay.Frame.GOAL || advancementDisplay.frame() == AdvancementDisplay.Frame.TASK) {
                    progres++;
                    if(progres <= 20)
                        result += progres * progres * 10;
                    else if(progres <= 40)
                        result += progres * progres * 100;
                    else if(progres <= 70)
                        result += progres * progres * 1000;
                    else
                        result += progres * progres * 10000;
                }
            }
        }
        addCoins(result);
        setProgres(progres);
        setSuccess(success);
    }

    public void addCoins(int coin) {
        coins += coin;
        if(coinScore != null)
            coinScore.setScore(coins);

        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "coins", this.coins);
    }

    public void addMort(int mort) {
        this.mort += mort;
        if(mortScore != null)
            mortScore.setScore(this.mort);

        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "mort", this.mort);
    }

    public void setSuccess(int success) {
        this.success = success;
        if(successScore != null)
            successScore.setScore(this.success);

        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "success", this.success);
    }

    public void setProgres(int progres) {
        this.progres = progres;
        if(progresScore != null)
            progresScore.setScore(this.progres);

        GameManager.getInstance().getDatabaseManager().updatePlayer(uuid, "progres", this.progres);
    }

    public UUID getUUID(){
        return uuid;
    }

    public int getMort(){
        return mort;
    }

    public int getCoins(){
        return coins;
    }

    public int getSuccess(){
        return success;
    }

    public int getProgres(){
        return progres;
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
}
