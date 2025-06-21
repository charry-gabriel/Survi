package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.Trader;
import fr.miuby.survi.villager.VillagerLevel;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        if (event.getInventory().getResult() == null || event.getInventory().getResult().getType() == Material.AIR)
            return;

        //lock par les pnj
        if(event.getInventory().getResult() != null) {
            if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getInventory().getResult().getType().getKey())) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }

        //lock par role
        CustomRecipe customRecipe = CustomRecipe.getCustomRecipe(event.getInventory().getResult());
        if (customRecipe != null && customRecipe.getResult().isSimilar(ECustomItem.GROWTH_PICKAXE.getItemStack(1))) {
            if (event.getViewers().isEmpty() || !(event.getViewers().getFirst() instanceof Player viewer)) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                return;
            }

            AlphaPlayer alpha = AlphaPlayer.get(viewer.getUniqueId());
            boolean isMiner = alpha != null && alpha.getSubRoles().stream().anyMatch(role -> role.type() == ERole.MINEUR);

            if (!isMiner) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        //block ancien craft
        if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getResult().getType().getKey())
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
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.MERCHANT || event.getInventory().getType() == InventoryType.MERCHANT)
            return;

        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory().getHolder() instanceof Player && event.getInventory().getHolder() instanceof Villager) {
            if (item != null && item.getType() != Material.AIR) {
                Villager v = (Villager) event.getInventory().getHolder();
                VillagerLevel villager = (VillagerLevel) AVillager.get(v.getUniqueId());

                if (villager == null)
                    return;

                villager.giveItems(event.getInventory(), item, player);
            }

            event.setCancelled(true);
        } else if (event.getClickedInventory().getHolder() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerTrade(PlayerTradeEvent event) {
        Trader villager = (Trader) AVillager.get(event.getVillager().getUniqueId());
        if (villager == null)
            return;

        event.getPlayer().sendMessage(Component.text("<", NamedTextColor.AQUA).append(villager.getDisplayName()).color(NamedTextColor.AQUA).append(Component.text("> ", NamedTextColor.AQUA)
                .append(villager.getMessage(event.getTrade().getResult())).color(NamedTextColor.AQUA)));
    }
}
