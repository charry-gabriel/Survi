package fr.miuby.survi18.listener;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.Survi18;
import fr.miuby.survi18.village.VillagerEtat;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

public class PlayerListener implements Listener {
    static Survi18 plugin;

    public PlayerListener(Survi18 instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if(!GameManager.getInstance().getAlphaPlayers().containsKey(uuid)) {
            GameManager.getInstance().getAlphaPlayers().put(uuid, new AlphaPlayer(uuid));
        }else{
            GameManager.getInstance().getAlphaPlayers().get(uuid).actualize();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameManager.getInstance().getAlphaPlayers().get(uuid).resetPlayer();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.getPlayer().getLocation().getWorld() == GameManager.getInstance().getVillage().getWorld()) {
            Block block = event.getPlayer().getLocation().getBlock();
            if(block.getX() > 12199 || block.getX() < 11963 || block.getZ() > 1590 || block.getZ() < 1375 || block.getY() < 0) {
                event.getPlayer().teleport(new Location(GameManager.getInstance().getVillage().getWorld(), 12073, 64, 1463));
                event.getPlayer().sendMessage(ChatColor.RED + "Ne sort pas des limite du village, c'est dangereux !!");
            }
        }
        if(event.getPlayer().getLocation().getWorld() == GameManager.getInstance().GetWorld("Wilderness")) {
            Block block = event.getPlayer().getLocation().getBlock();
            if(block.getX() > 2000 || block.getX() < -2000 || block.getZ() > 2000 || block.getZ() < -2000) {
                event.getPlayer().teleport(new Location(GameManager.getInstance().getVillage().getWorld(), 12073, 64, 1463));
                event.getPlayer().sendMessage(ChatColor.RED + "Ne sort pas des limite du wilderness, c'est dangereux !! (Parle avec Thomas Pesquet)");
            }
        }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
        AdvancementDisplay advancementDisplay = advancement.getDisplay();

        /*String categorie = advancement.getKey().getKey().split("/")[0];
        if(advancementProgress.isDone() && advancementDisplay != null && !categorie.equals("recipes")) {
            if(advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId()).gainOneSuccess(true);
            } else if(advancementDisplay.frame() == AdvancementDisplay.Frame.GOAL || advancementDisplay.frame() == AdvancementDisplay.Frame.TASK) {
                GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId()).gainOneSuccess(false);
            }
        }*/
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        if(event.getRightClicked() instanceof Villager) {
            Villager v = (Villager) event.getRightClicked();
            VillagerEtat villager = GameManager.getInstance().getVillage().getVillagersEtat().get(v.customName());
            if(v.customName() != null && villager != null) {
                p.openInventory(villager.getInventory());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        //GameManager.getInstance().getAlphaPlayers().get(event.getPlayer().getUniqueId()).addMort(1);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        GameManager.getInstance().switchWorld(event.getPlayer().getWorld().getName(), event.getPlayer().getName());
    }
}
