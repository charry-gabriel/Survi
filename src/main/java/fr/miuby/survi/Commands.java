package fr.miuby.survi;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.villager.AVillager;
import net.kyori.adventure.text.Component;
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
                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(args[0]);

                Role role = GameManager.getInstance().getRoleFactory().getRole(args[1]);
                if (role == null) {
                    sender.sendMessage(Component.text("Role introuvable !"));
                    return false;
                }

                // Call event
                AlphaPlayerRoleChangeEvent alphaPlayerRoleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, alphaPlayer.getRole(), role);
                GameManager.getInstance().callEvent(alphaPlayerRoleChangeEvent);
                if (alphaPlayerRoleChangeEvent.isCancelled())
                    return true;

                // Swap role
                GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "role", role.type().toString());
                alphaPlayer.setRole(role);

                if (alphaPlayer.getPlayer().isOnline())
                    GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
                return true;
            }
            else if (commandName.equals("subrole") && args.length == 3) {
                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(args[1]);

                Role role = GameManager.getInstance().getRoleFactory().getRole(args[2]);
                if (role == null) {
                    sender.sendMessage(Component.text("Role introuvable !"));
                    return false;
                }

                // Call event
                AlphaPlayerRoleChangeEvent alphaPlayerRoleChangeEvent;
                if (args[0].equals("add"))
                    alphaPlayerRoleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, null, role);
                else if (args[0].equals("remove"))
                    alphaPlayerRoleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, role, null);
                else
                    return false;

                GameManager.getInstance().callEvent(alphaPlayerRoleChangeEvent);
                if (alphaPlayerRoleChangeEvent.isCancelled())
                    return true;

                // Add or Remove subrole
                if (args[0].equals("add"))
                    alphaPlayer.addSubRole(role);
                else if (args[0].equals("remove"))
                    alphaPlayer.removeSubRole(role);

                GameManager.getInstance().getDatabase().updatePlayer(alphaPlayer.getUUID(), "subroles", String.join(",", alphaPlayer.getSubRoles().stream().map(subrole -> subrole.type().toString()).toList()));

                if (alphaPlayer.getPlayer().isOnline())
                    GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
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
