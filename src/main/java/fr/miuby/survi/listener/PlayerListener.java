package fr.miuby.survi.listener;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.Monde;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

public class PlayerListener implements Listener {
    static fr.miuby.survi.Survi plugin;

    public PlayerListener(fr.miuby.survi.Survi instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if(!GameManager.getInstance().getAlphaPlayers().containsKey(uuid)) {
            AlphaPlayer alphaPlayer = new AlphaPlayer(uuid);
            GameManager.getInstance().getAlphaPlayers().put(uuid, alphaPlayer);
            GameManager.getInstance().getDatabase().getAlphaPlayers(alphaPlayer, uuid);
        }else{
            AlphaPlayer.get(uuid).actualize();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        AlphaPlayer.get(event.getPlayer().getUniqueId()).resetPlayer();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(Monde.isOutOfLimit(event.getPlayer(), EWorld.VILLAGE) || Monde.isOutOfLimit(event.getPlayer(), EWorld.WILDERNESS)) {
            event.getPlayer().teleport(GameManager.getInstance().getMonde(EWorld.VILLAGE).getSpawnPoint());
            event.getPlayer().sendMessage(Component.text("Ne sort pas des limite du village, c'est dangereux !!").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
        AdvancementDisplay advancementDisplay = advancement.getDisplay();

        String category = advancement.getKey().getKey().split("/")[0];
        if(advancementProgress.isDone() && advancementDisplay != null && !category.equals("recipes")) {
            if(advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                AlphaPlayer.get(player.getUniqueId()).gainOneSuccess(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if(event.getRightClicked().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getRightClicked();
            if (AVillager.contains(villager.getUniqueId()))
                player.openInventory(AVillager.get(villager.getUniqueId()).getInventory());

            event.setCancelled(true);
        } else if(event.getRightClicked().getType() == EntityType.WANDERING_TRADER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        AlphaPlayer.get(event.getPlayer().getUniqueId()).addMort(1);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        GameManager.getInstance().switchWorld(event.getPlayer().getWorld().getName(), event.getPlayer().getName());
        AlphaPlayer.get(event.getPlayer().getUniqueId()).updateLife();
    }
}
