package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class AlphaTeam {
    private final Team team;
    private final String name;
    private final Scoreboard scoreboard;
    private final Monde world;
    private final Role role;

    public AlphaTeam(Scoreboard scoreboard, EWorld worldType, ERole roleType) {
        this.world = Monde.get(worldType);
        this.role = GameManager.getInstance().getRoleFactory().getRole(roleType);

        this.name = worldType.toString() + roleType.toString();
        this.scoreboard = scoreboard;
        this.team = this.scoreboard.registerNewTeam(name);

        this.team.color(this.world.getColor());
        this.team.prefix(Component.text(world.getName() + " - ").append(role.displayName()).append(Component.text(" ")));
    }

    public ERole getRole() {
        return role.type();
    }

    public EWorld getWorld() {
        return world.getType();
    }

    public void addPlayer(AlphaPlayer player) {
        this.team.addEntry(player.getPlayer().getName());
    }
}
