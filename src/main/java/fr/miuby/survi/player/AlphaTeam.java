package fr.miuby.survi.player;

import fr.miuby.survi.role.Role;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Random;

public class AlphaTeam {
    private final Team team;

    public AlphaTeam(Scoreboard scoreboard, AlphaPlayer alphaPlayer) {
        Random random = new Random();
        this.team = scoreboard.registerNewTeam(alphaPlayer.getPseudo() + random.nextInt());

        Monde world = Monde.get(alphaPlayer.getWorld());
        this.team.color(world.getColor());
        TextComponent prefix = Component.text(world.getName() + " - ");

        for (Role subRole : alphaPlayer.getSubRoles())
            prefix = prefix.append(subRole.displayName());

        this.team.prefix(prefix.append(alphaPlayer.getRole().displayName()).append(Component.text(" ")));
    }

    public void addPlayer(AlphaPlayer player) {
        this.team.addEntry(player.getPlayer().getName());
    }
}
