package fr.miuby.survi.player;

import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.role.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Random;

public class AlphaTeam {
    private final Team team;

    public AlphaTeam(Scoreboard scoreboard, AlphaPlayer alphaPlayer) {
        Random random = new Random();
        this.team = scoreboard.registerNewTeam(alphaPlayer.getPseudo() + random.nextInt());

        MLWorld world = alphaPlayer.getWorld();
        if (world != null && world.getColor() != null) {
            this.team.color(world.getColor());
        } else {
            this.team.color(NamedTextColor.WHITE);
        }

        TextComponent prefix = (world != null)
                ? Component.text(world.getName() + " - ")
                : Component.empty();

        for (Role subRole : alphaPlayer.getSubRoles()) {
            prefix = prefix.append(subRole.displayName());
        }

        Role role = alphaPlayer.getRole();
        if (role != null) {
            this.team.prefix(prefix.append(role.displayName()).append(Component.text(" ")));
        } else {
            this.team.prefix(prefix);
        }
    }

    public void addPlayer(AlphaPlayer player) {
        if (player.getPlayer() != null) {
            this.team.addEntry(player.getPlayer().getName());
        }
    }
}