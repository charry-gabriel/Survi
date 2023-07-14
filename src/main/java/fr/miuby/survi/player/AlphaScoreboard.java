package fr.miuby.survi.player;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;

public class AlphaScoreboard {
    private final Scoreboard scoreboard;
    private final List<AlphaTeam> teams = new ArrayList<>();
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

    public AlphaTeam getTeam(EWorld worldType, ERole roleType) {
        for (AlphaTeam team : teams) {
            if (team.getRole() == roleType && team.getWorld() == worldType) {
                return team;
            }
        }

        AlphaTeam team = new AlphaTeam(scoreboard, worldType, roleType);
        teams.add(team);
        return team;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}

