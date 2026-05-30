package fr.miuby.survi.listener;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemListener implements Listener {
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() == Material.AIR)
            return;

        // Vérification du verrouillage par PNJ
        if (GameManager.getInstance().getLockedItemsFactory().isLocked(result.getType().getKey())) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        CustomRecipe customRecipe = CustomRecipe.getCustomRecipe(result);
        if (customRecipe == null)
            return;

        if (event.getViewers().isEmpty() || !(event.getViewers().getFirst() instanceof Player viewer)) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        AlphaPlayer alpha = AlphaPlayer.get(viewer.getUniqueId());
        if (alpha == null) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        // Vérification des permissions par tags
        List<String> roles = customRecipe.getRoles();
        if (roles != null && !roles.isEmpty()) {
            boolean hasPermission = false;
            for (Role r : alpha.getSubRoles()) {
                if (roles.contains(r.type().name())) {
                    hasPermission = true;
                    break;
                }
            }

            if (!hasPermission)
                event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.MERCHANT || event.getInventory().getType() == InventoryType.MERCHANT)
            return;

        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory().getHolder() instanceof Player && event.getInventory().getHolder() instanceof Villager v) {
            if (item != null && item.getType() != Material.AIR) {
                VillagerLevel villager = (VillagerLevel) VillagerRegistry.get(v.getUniqueId());

                if (villager == null)
                    return;

                villager.giveItems(event.getInventory(), item, player);
            }

            event.setCancelled(true);
        } else if (event.getClickedInventory().getHolder() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTrade(PlayerTradeEvent event) {
        Trader villager = (Trader) VillagerRegistry.get(event.getVillager().getUniqueId());
        if (villager == null)
            return;

        event.getPlayer().sendMessage(Component.text("<", NamedTextColor.AQUA).append(villager.getDisplayName()).color(NamedTextColor.AQUA).append(Component.text("> ", NamedTextColor.AQUA)
                .append(villager.getMessage(event.getTrade().getResult())).color(NamedTextColor.AQUA)));
    }
}