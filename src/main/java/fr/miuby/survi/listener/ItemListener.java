package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.AVillager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        ItemStack[] matrix = event.getInventory().getMatrix();

        if (matrix.length < 9)
            return;

        //block ancien craft
        if (event.getInventory().getResult() != null
                && (event.getInventory().getResult().getType() == Material.IRON_BOOTS
                || event.getInventory().getResult().getType() == Material.IRON_HELMET
                || event.getInventory().getResult().getType() == Material.IRON_CHESTPLATE
                || event.getInventory().getResult().getType() == Material.IRON_LEGGINGS
                || event.getInventory().getResult().getType() == Material.GOLDEN_BOOTS
                || event.getInventory().getResult().getType() == Material.GOLDEN_HELMET
                || event.getInventory().getResult().getType() == Material.GOLDEN_CHESTPLATE
                || event.getInventory().getResult().getType() == Material.GOLDEN_LEGGINGS
                || event.getInventory().getResult().getType() == Material.DIAMOND_BOOTS
                || event.getInventory().getResult().getType() == Material.DIAMOND_CHESTPLATE
                || event.getInventory().getResult().getType() == Material.DIAMOND_HELMET
                || event.getInventory().getResult().getType() == Material.DIAMOND_LEGGINGS)) {
            ItemStack air = new ItemStack(Material.AIR);
            event.getInventory().setResult(air);
        }

        //region chainmail_armor
        if((matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET
                && matrix[6] != null && matrix[6].getType() == Material.LAVA_BUCKET
                && matrix[8] != null && matrix[8].getType() == Material.LAVA_BUCKET
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && matrix[4] != null && matrix[4].getType() == Material.LEATHER_BOOTS
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && matrix[4] != null && matrix[4].getType() == Material.LEATHER_BOOTS
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_BOOTS));

        if((matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[1] != null && matrix[1].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[5] != null && matrix[5].getType() == Material.LAVA_BUCKET)
                && matrix[4] != null && matrix[4].getType() == Material.LEATHER_HELMET
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.LAVA_BUCKET
                && matrix[2] != null && matrix[2].getType() == Material.LAVA_BUCKET
                && matrix[3] != null && matrix[3].getType() == Material.LAVA_BUCKET
                && matrix[4] != null && matrix[4].getType() == Material.LEATHER_CHESTPLATE
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
                && matrix[4] != null && matrix[4].getType() == Material.LEATHER_LEGGINGS
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        //endregion

        //region iron_armor
        if((matrix[3] != null && matrix[3].getType() == Material.IRON_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.IRON_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.IRON_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.IRON_BLOCK
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.CHAINMAIL_BOOTS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.IRON_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.IRON_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.IRON_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.IRON_BLOCK)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.CHAINMAIL_BOOTS)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.IRON_BOOTS));

        if((matrix[0] != null && matrix[0].getType() == Material.IRON_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.IRON_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.IRON_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.IRON_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.IRON_BLOCK)
                && (matrix[4] != null && matrix[4].getType() == Material.CHAINMAIL_HELMET)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.IRON_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.IRON_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.IRON_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.IRON_BLOCK
                && matrix[4] != null && matrix[4].getType() == Material.CHAINMAIL_CHESTPLATE
                && matrix[5] != null && matrix[5].getType() == Material.IRON_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.IRON_BLOCK
                && matrix[7] != null && matrix[7].getType() == Material.IRON_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.IRON_BLOCK
                && (matrix[1] == null || matrix[1].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.IRON_CHESTPLATE));

        if(matrix[0] != null && matrix[0].getType() == Material.IRON_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.IRON_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.IRON_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.IRON_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.IRON_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.IRON_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.IRON_BLOCK
                && (matrix[4] != null && matrix[4].getType() == Material.CHAINMAIL_LEGGINGS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.IRON_LEGGINGS));
        //endregion

        //region gold_armor
        if((matrix[3] != null && matrix[3].getType() == Material.GOLD_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.GOLD_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.GOLD_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.GOLD_BLOCK
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.IRON_BOOTS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.GOLD_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.GOLD_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.GOLD_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.GOLD_BLOCK)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.IRON_BOOTS)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.GOLDEN_BOOTS));

        if((matrix[0] != null && matrix[0].getType() == Material.GOLD_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.GOLD_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.GOLD_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.GOLD_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.GOLD_BLOCK)
                && (matrix[4] != null && matrix[4].getType() == Material.IRON_HELMET)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.GOLDEN_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.GOLD_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.GOLD_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.GOLD_BLOCK
                && matrix[4] != null && matrix[4].getType() == Material.IRON_CHESTPLATE
                && matrix[5] != null && matrix[5].getType() == Material.GOLD_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.GOLD_BLOCK
                && matrix[7] != null && matrix[7].getType() == Material.GOLD_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.GOLD_BLOCK
                && (matrix[1] == null || matrix[1].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.GOLDEN_CHESTPLATE));

        if(matrix[0] != null && matrix[0].getType() == Material.GOLD_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.GOLD_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.GOLD_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.GOLD_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.GOLD_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.GOLD_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.GOLD_BLOCK
                && (matrix[4] != null && matrix[4].getType() == Material.IRON_LEGGINGS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.GOLDEN_LEGGINGS));
        //endregion

        //region diamond_armor
        if((matrix[3] != null && matrix[3].getType() == Material.DIAMOND_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.DIAMOND_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.DIAMOND_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.DIAMOND_BLOCK
                && (matrix[0] == null || matrix[0].getType() == Material.AIR)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[2] == null || matrix[2].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.GOLDEN_BOOTS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
                || (matrix[0] != null && matrix[0].getType() == Material.DIAMOND_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.DIAMOND_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.DIAMOND_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.DIAMOND_BLOCK)
                && (matrix[1] == null || matrix[1].getType() == Material.AIR)
                && (matrix[4] != null && matrix[4].getType() == Material.GOLDEN_BOOTS)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.DIAMOND_BOOTS));

        if((matrix[0] != null && matrix[0].getType() == Material.DIAMOND_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.DIAMOND_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.DIAMOND_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.DIAMOND_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.DIAMOND_BLOCK)
                && (matrix[4] != null && matrix[4].getType() == Material.GOLDEN_HELMET)
                && (matrix[6] == null || matrix[6].getType() == Material.AIR)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR)
                && (matrix[8] == null || matrix[8].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.DIAMOND_HELMET));

        if(matrix[0] != null && matrix[0].getType() == Material.DIAMOND_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.DIAMOND_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.DIAMOND_BLOCK
                && matrix[4] != null && matrix[4].getType() == Material.GOLDEN_CHESTPLATE
                && matrix[5] != null && matrix[5].getType() == Material.DIAMOND_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.DIAMOND_BLOCK
                && matrix[7] != null && matrix[7].getType() == Material.DIAMOND_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.DIAMOND_BLOCK
                && (matrix[1] == null || matrix[1].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.DIAMOND_CHESTPLATE));

        if(matrix[0] != null && matrix[0].getType() == Material.DIAMOND_BLOCK
                && matrix[1] != null && matrix[1].getType() == Material.DIAMOND_BLOCK
                && matrix[2] != null && matrix[2].getType() == Material.DIAMOND_BLOCK
                && matrix[3] != null && matrix[3].getType() == Material.DIAMOND_BLOCK
                && matrix[5] != null && matrix[5].getType() == Material.DIAMOND_BLOCK
                && matrix[6] != null && matrix[6].getType() == Material.DIAMOND_BLOCK
                && matrix[8] != null && matrix[8].getType() == Material.DIAMOND_BLOCK
                && (matrix[4] != null && matrix[4].getType() == Material.GOLDEN_LEGGINGS)
                && (matrix[7] == null || matrix[7].getType() == Material.AIR))
            event.getInventory().setResult(new ItemStack(Material.DIAMOND_LEGGINGS));
        //endregion

        //block par les pnj
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getInventory().getResult())) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        //block ancien craft
        if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getResult())
                || event.getResult().getType() == Material.IRON_BOOTS
                || event.getResult().getType() == Material.IRON_CHESTPLATE
                || event.getResult().getType() == Material.IRON_LEGGINGS
                || event.getResult().getType() == Material.GOLDEN_BOOTS
                || event.getResult().getType() == Material.GOLDEN_HELMET
                || event.getResult().getType() == Material.IRON_HELMET
                || event.getResult().getType() == Material.GOLDEN_CHESTPLATE
                || event.getResult().getType() == Material.GOLDEN_LEGGINGS
                || event.getResult().getType() == Material.DIAMOND_BOOTS
                || event.getResult().getType() == Material.DIAMOND_CHESTPLATE
                || event.getResult().getType() == Material.DIAMOND_HELMET
                || event.getResult().getType() == Material.DIAMOND_LEGGINGS) {
            ItemStack air = new ItemStack(Material.AIR);
            event.setResult(air);
        }
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
