package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.CustomRecipe;
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
        if (event.getInventory().getResult() == null || event.getInventory().getResult().getType() == Material.AIR)
            return;

        //block par les pnj
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getInventory().getResult())) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }

        CustomRecipe customRecipe = CustomRecipe.getCustomRecipe(event.getInventory().getResult());

        if (customRecipe == null)
            return;

        boolean flag = true;
        for (int i = 0; i < 9 && flag; i++) {
            if (event.getInventory().getItem(i + 1) == null || event.getInventory().getItem(i + 1).getType() == Material.AIR)
                flag = customRecipe.getIngredients().get(i).getType() == Material.AIR;
            else
                flag = customRecipe.getIngredients().get(i).isSimilar(event.getInventory().getItem(i + 1));
        }

        if (!flag)
            event.getInventory().setResult(new ItemStack(Material.AIR));
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
