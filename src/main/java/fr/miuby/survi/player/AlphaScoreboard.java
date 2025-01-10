package fr.miuby.survi.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.*;

public class AlphaScoreboard {
    private final Scoreboard scoreboard;
    private Score mortScore;
    private Score successScore;

    public AlphaScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        /*Objective objectifInfo = scoreboard.registerNewObjective("Info", Criteria.DUMMY, Component.text("Info"));
        objectifInfo.setDisplaySlot(DisplaySlot.SIDEBAR);
        successScore = objectifInfo.getScore("Succès :");
        successScore.setScore(success);*/

        Objective life = scoreboard.registerNewObjective("Vie", Criteria.HEALTH, Component.text("Vie"));
        life.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        life.setRenderType(RenderType.HEARTS);
    }

    public AlphaTeam getTeam(AlphaPlayer alphaPlayer) {
        return new AlphaTeam(scoreboard, alphaPlayer);
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}

