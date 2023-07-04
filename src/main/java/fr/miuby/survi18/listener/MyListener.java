package fr.miuby.survi18.listener;

import fr.miuby.survi18.*;
import fr.miuby.survi18.village.VillagerLevel;
import fr.miuby.survi18.village.VillagerVendor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.PortalCreateEvent;
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

        ItemStack[] matrix = event.getInventory().getMatrix();
        if((matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_BOOTS));

        if((matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[4] != null && matrix[4].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[1] != null && matrix[1].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[4] != null && matrix[4].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[7] != null && matrix[7].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[1] == null || matrix[1].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_CHESTPLATE));

        if(matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[1] != null && matrix[1].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[4] == null || matrix[4].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_LEGGINGS));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        //si on tape
        if(event.getDamager() instanceof Player){
            event.setDamage(event.getDamage() * GameManager.getInstance().getAlphaPlayers().get(event.getDamager().getUniqueId()).getDamage());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.VILLAGER) {
            event.setCancelled(true);
        }

        //si on prends des degats
        if(event.getEntityType() == EntityType.PLAYER) {
            event.setDamage(event.getDamage() / GameManager.getInstance().getAlphaPlayers().get(event.getEntity().getUniqueId()).getResistance());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if(item != null && item.getType() != Material.AIR) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Player) {
                for (VillagerLevel villager : GameManager.getInstance().getVillage().getVillagersLevel().values()) {
                    if (villager.getInventory() == event.getInventory()) {
                        GameManager.getInstance().getLogger().info(player.getName() + " a cliqué sur " + villager.getName().toString());
                        villager.GiveItems(villager.getInventory(), item, player);
                        event.setCancelled(true);
                    }
                }
                for (VillagerVendor villager : GameManager.getInstance().getVillage().getVillagersVendor().values()) {
                    if (villager.getInventory() == event.getInventory()) {
                        villager.GiveItems(villager.getInventory(), item, player);
                        event.setCancelled(true);
                    }
                }
            } else if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Villager) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (!GameManager.getInstance().hasNetherAccess())
                event.setCancelled(true);
        } else {
            if (!GameManager.getInstance().hasEndAccess())
                event.setCancelled(true);
        }
    }
}
