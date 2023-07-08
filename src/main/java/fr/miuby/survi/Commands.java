package fr.miuby.survi;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class Commands {
    private static Survi plugin;

    Commands(Survi instance) {
        plugin = instance;
    }

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
        }
        return false;
    }
}
