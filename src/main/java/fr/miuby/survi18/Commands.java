package fr.miuby.survi18;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Commands {
    private static Survi18 plugin;

    Commands(Survi18 instance) {
        plugin = instance;
    }

    public boolean doCommand(Player sender, String commandName, String[] args) {
        if(sender.isOp()) {
            if (commandName.equals("alpha-addmort") && args.length == 2) {
                int mort = Integer.parseInt(args[0]);
                AlphaPlayer alphaP = GameManager.getInstance().getAlphaPlayer(args[1]);
                if (mort != 0 && alphaP != null) {
                    addMort(mort, alphaP, sender);
                    return true;
                }
            } else if(commandName.equals("alpha-success") && args.length == 1) {
                Player player = plugin.getServer().getPlayer(args[0]);
                if(player != null) {
                    GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId()).gainOldAdvancement(player);
                    return true;
                }
            }else if(commandName.equals("alpha-debug") && args.length == 0) {
                for(AlphaPlayer player : GameManager.getInstance().getAlphaPlayers().values()){
                    if(player.getPlayer() != null)
                        GameManager.getInstance().getLogger().info(String.valueOf(player.getPlayer().getScoreboard().getTeams().size()));
                }
                return true;
            }
        }
        return false;
    }

    public void addMort(int mort, AlphaPlayer alphaP, Player sender){
        GameManager.getInstance().getLogger().info("command addMort send");
        alphaP.addMort(mort);
        sender.sendMessage(alphaP.getPseudo() + " a reçu " + mort + " Mort ! Il a un total de " + alphaP.getMort() + " Mort !");
    }
}
