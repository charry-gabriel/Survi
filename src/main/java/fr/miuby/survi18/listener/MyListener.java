package fr.miuby.survi18.listener;

import fr.miuby.survi18.*;
import fr.miuby.survi18.village.VillagerEtat;
import fr.miuby.survi18.village.VillagerLevel;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class MyListener implements Listener {
    static Survi18 plugin;

    public MyListener(Survi18 instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsManager().isLocked(event.getInventory().getResult())) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        //tape pas les pnj
        if(event.getEntity().getType() == EntityType.VILLAGER) {
            if(event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (player.getGameMode() != GameMode.CREATIVE && player.getWorld() == GameManager.getInstance().getVillage().getWorld()) {
                    event.setCancelled(true);
                }
            }
        }

        //si on tape
        if(event.getDamager() instanceof Player){
            event.setDamage(event.getDamage() * GameManager.getInstance().getAlphaPlayers().get(event.getDamager().getUniqueId()).getResistance());
        }
        //si on se fait taper
        if(event.getEntity() instanceof Player){
            event.setDamage(event.getDamage() * GameManager.getInstance().getAlphaPlayers().get(event.getDamager().getUniqueId()).getDamage());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if(item != null
            && item.getType() != Material.AIR
            && event.getClickedInventory() != null
            && (event.getClickedInventory().getHolder() instanceof Villager || event.getClickedInventory().getHolder() instanceof Player
            && event.getInventory().getHolder() instanceof Villager))
        {
            for (VillagerEtat villager : GameManager.getInstance().getVillage().getVillagersEtat().values()) {
                boolean villagerInventory = villager.getInventory() == event.getClickedInventory();
                if (villager.getInventory() == event.getInventory() || villagerInventory) {
                    villager.Trade(villagerInventory, item, player);
                    event.setCancelled(true);
                }
            }

            for (VillagerLevel villager : GameManager.getInstance().getVillage().getVillagersLevel().values()) {
                boolean villagerInventory = villager.getInventory() == event.getClickedInventory();
                if (villager.getInventory() == event.getInventory() || villagerInventory) {
                    villager.GiveItems(villagerInventory, item, player);
                    event.setCancelled(true);
                }
            }
        }
    }
}
