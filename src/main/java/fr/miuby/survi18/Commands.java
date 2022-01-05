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
            if (commandName.equals("alpha-addcoin") && args.length == 2) {
                int coin = Integer.parseInt(args[0]);
                AlphaPlayer alphaP = GameManager.getInstance().getAlphaPlayer(args[1]);
                if (coin != 0 && alphaP != null) {
                    addCoin(coin, alphaP, sender);
                    return true;
                }
            } else if (commandName.equals("alpha-addmort") && args.length == 2) {
                int mort = Integer.parseInt(args[0]);
                AlphaPlayer alphaP = GameManager.getInstance().getAlphaPlayer(args[1]);
                if (mort != 0 && alphaP != null) {
                    addMort(mort, alphaP, sender);
                    return true;
                }
            } else if(commandName.equals("alpha-impot") && args.length == 1) {
                int percent = Integer.parseInt(args[0]);
                for(AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                    if(!alphaPlayer.getUUID().toString().equals("de8530ae-0e92-4e83-a90e-bc04bcb4cf74")) {
                        int removeCoin = Math.round((alphaPlayer.getCoins() * percent) / 100f);
                        GameManager.getInstance().getLogger().info(removeCoin + "");
                        alphaPlayer.addCoins(-removeCoin);
                    }
                }
                sender.sendMessage(percent + "% retiré a tous les joueurs !");
                return true;
            } else if(commandName.equals("alpha-coins") && args.length == 0) {
                for(AlphaPlayer alphaPlayer : GameManager.getInstance().getAlphaPlayers().values()) {
                    sender.sendMessage(alphaPlayer.getPseudo() + " a " + alphaPlayer.getCoins() + " AlphaCoins");
                }
                return true;
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
        if (commandName.equals("givecoin") && args.length == 2) {
            GameManager.getInstance().getLogger().info("command givecoin send");
            AlphaPlayer alphaP = GameManager.getInstance().getAlphaPlayer(args[1]);
            AlphaPlayer alphaS = GameManager.getInstance().getAlphaPlayers().get(sender.getUniqueId());
            int sous = Math.abs(Integer.parseInt(args[0]));
            if(alphaS.getCoins() >= sous) {
                alphaP.addCoins(sous);
                alphaS.addCoins(-sous);
                sender.sendMessage("Vous avez envoyé " + sous + " AlphaCoins a " + alphaP.getPseudo() + " !");
                Player player = GameManager.getInstance().getPlugin().getServer().getPlayer(alphaP.getUUID());
                if(player != null)
                    player.sendMessage("Vous avez reçu " + sous + " AlphaCoins de la part de " + alphaS.getPseudo() + " !");
            } else {
                sender.sendMessage("Vous n'avez pas assez d'AlphaCoins !");
            }
            return true;
        }
        return false;
    }

    public void addCoin(int coin, AlphaPlayer alphaP, Player sender){
        GameManager.getInstance().getLogger().info("command addCoin send");
        alphaP.addCoins(coin);
        Player player = Bukkit.getPlayer(alphaP.getUUID());
        if(player != null) {
            if (coin < 0) {
                player.sendMessage("Vous avez payé une taxe de " + Math.abs(coin) + " AlphaCoin pour l'État !");
            } else {
                player.sendMessage("Vous avez reçu " + Math.abs(coin) + " AlphaCoin de la part de l'État !");
            }
        }
        sender.sendMessage(alphaP.getPseudo() + " a reçu " + coin + " AlphaCoin ! Il a un total de " + alphaP.getCoins() + " AlphaCoin !");
    }

    public void addMort(int mort, AlphaPlayer alphaP, Player sender){
        GameManager.getInstance().getLogger().info("command addMort send");
        alphaP.addMort(mort);
        sender.sendMessage(alphaP.getPseudo() + " a reçu " + mort + " Mort ! Il a un total de " + alphaP.getMort() + " Mort !");
    }
}
