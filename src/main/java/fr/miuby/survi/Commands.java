package fr.miuby.survi;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Commands {
    public boolean doCommand(Player sender, String commandName, String[] args) {
        if(sender.isOp()) {
            if (commandName.equals("sql") && args.length > 1) {

                StringBuilder sql = new StringBuilder();
                for (String arg : args)
                    sql.append(arg).append(" ");

                String result = GameManager.getInstance().getDatabase().Request(sql.toString());
                sender.sendMessage(Component.text(result));
                return true;
            }
            else if (commandName.equals("role") && args.length == 2) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendMessage(Component.text("Joueur introuvable !"));
                    return false;
                }

                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(player.getUniqueId());

                Role roleFound = Role.get(args[1]);
                GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "role", roleFound.getType().toString());
                alphaPlayer.setRole(roleFound);

                alphaPlayer.switchRole();
                return true;
            }
            else if (commandName.equals("villager") && args.length == 2) {
                //if (args[0].equals("teleport"))
            }
        }
        return false;
    }
}
