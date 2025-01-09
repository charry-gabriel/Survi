package fr.miuby.survi;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.villager.AVillager;
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

                Role roleFound = GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(args[1]));
                GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "role", roleFound.type().toString());
                alphaPlayer.setRole(roleFound);

                alphaPlayer.switchRole();
                return true;
            }
            else if (commandName.equals("subrole") && args.length == 3) {
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage(Component.text("Joueur introuvable !"));
                    return false;
                }

                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(player.getUniqueId());
                Role roleFound = GameManager.getInstance().getRoleFactory().getRole(ERole.valueOf(args[2]));

                if (args[0].equals("add")) {
                    alphaPlayer.addSubRole(roleFound);
                } else if (args[0].equals("remove")) {
                    alphaPlayer.removeSubRole(roleFound);
                }
                GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "subroles", String.join(",", alphaPlayer.getSubRoles().stream().map(role -> role.type().toString()).toList()));

                alphaPlayer.switchRole();
                return true;
            }
            else if (commandName.equals("villager") && args.length == 2) {
                if (args[0].equals("teleport")) {
                    AVillager villager = GameManager.getInstance().getVillagerFactory().getVillager(args[1]);

                    if (villager == null) {
                        sender.sendMessage(Component.text("Villager introuvable !"));
                        return false;
                    }

                    GameManager.getInstance().getDatabase().updateVillagerLocation(villager.getVillager().getUniqueId(), sender.getLocation());
                    villager.getVillager().teleport(sender.getLocation());
                    return true;
                }
            }
        }
        return false;
    }
}
