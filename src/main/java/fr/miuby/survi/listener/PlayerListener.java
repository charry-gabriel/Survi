package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.world.EWorld;
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
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if ((Monde.isOutOfLimit(event.getPlayer(), EWorld.VILLAGE) || Monde.isOutOfLimit(event.getPlayer(), EWorld.WILDERNESS)) && !event.getPlayer().isOp()) {
            AlphaPlayer.get(event.getPlayer().getUniqueId()).teleport(Monde.get(EWorld.VILLAGE));
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
        if (advancementProgress.isDone() && advancementDisplay != null && !category.equals("recipes")) {
            if (advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                AlphaPlayer.get(player.getUniqueId()).gainOneSuccess(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getRightClicked();
            if (AVillager.contains(villager.getUniqueId()))
                player.openInventory(AVillager.get(villager.getUniqueId()).getInventory());

            event.setCancelled(true);
        } else if (event.getRightClicked().getType() == EntityType.WANDERING_TRADER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        /*if (AlphaPlayer.get(event.getPlayer().getUniqueId()).getRole().getType() == ERole.JUMP
                && GameManager.getInstance().isNight()) {

        }*/

        boolean malus = false;
        for (ItemStack item : event.getPlayer().getInventory().getArmorContents()) {
            if (item != null && GameManager.getInstance().getLockedItemsFactory().isLocked(item)) {
                malus = true;
            }
        }
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        player.setArmorMalus(malus);
        player.getAlphaLife().actualize();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();

        final ItemStack[] armor = player.getInventory().getArmorContents();
        GameManager.getInstance().getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {
                player.getInventory().setArmorContents(armor);
            }
        });

        for (ItemStack is : armor) {
            event.getDrops().remove(is);
        }
    }

    /*@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) {
            event.getPlayer().chat("zzz");
        }
    }*/
}