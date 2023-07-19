package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.AVillager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getInventory().getResult())) {
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
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Player && event.getInventory().getHolder() instanceof Villager) {
            if (item != null && item.getType() != Material.AIR) {
                Villager v = (Villager) event.getInventory().getHolder();
                AVillager villager = AVillager.get(v.getUniqueId());

                villager.giveItems(event.getInventory(), item, player);
            }

            event.setCancelled(true);
        } else if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Villager) {
            event.setCancelled(true);
        }
    }
}
