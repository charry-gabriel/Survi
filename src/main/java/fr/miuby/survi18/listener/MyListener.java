package fr.miuby.survi18.listener;

import fr.miuby.survi18.*;
import fr.miuby.survi18.village.ItemEtat;
import fr.miuby.survi18.village.VillagerEtat;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class MyListener implements Listener {
    static Survi18 plugin;

    public MyListener(Survi18 instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        if(event.getInventory().getResult() != null) {
            ItemStack ironHelmet = new ItemStack(Material.IRON_HELMET, 1);
            ItemStack ironChestplate = new ItemStack(Material.IRON_CHESTPLATE, 1);
            ItemStack ironLeggings = new ItemStack(Material.IRON_LEGGINGS, 1);
            ItemStack ironBoots = new ItemStack(Material.IRON_BOOTS, 1);
            ItemStack goldenHelmet = new ItemStack(Material.GOLDEN_HELMET, 1);
            ItemStack goldenChestplate = new ItemStack(Material.GOLDEN_CHESTPLATE, 1);
            ItemStack goldlenLeggings = new ItemStack(Material.GOLDEN_LEGGINGS, 1);
            ItemStack goldenBoots = new ItemStack(Material.GOLDEN_BOOTS, 1);
            ItemStack diamondHelmet = new ItemStack(Material.DIAMOND_HELMET, 1);
            ItemStack diamondChestplate = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
            ItemStack diamondLeggings = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
            ItemStack diamondBoots = new ItemStack(Material.DIAMOND_BOOTS, 1);
            ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT, 1);
            ItemStack shulkerBox = new ItemStack(Material.SHULKER_BOX, 1);
            ItemStack beacon = new ItemStack(Material.BEACON, 1);
            if (event.getRecipe().getResult().equals(ironHelmet) ||
                    event.getRecipe().getResult().equals(ironChestplate) ||
                    event.getRecipe().getResult().equals(ironLeggings) ||
                    event.getRecipe().getResult().equals(ironBoots) ||
                    event.getRecipe().getResult().equals(goldenHelmet) ||
                    event.getRecipe().getResult().equals(goldenChestplate) ||
                    event.getRecipe().getResult().equals(goldlenLeggings) ||
                    event.getRecipe().getResult().equals(goldenBoots) ||
                    event.getRecipe().getResult().equals(diamondHelmet) ||
                    event.getRecipe().getResult().equals(diamondChestplate) ||
                    event.getRecipe().getResult().equals(diamondLeggings) ||
                    event.getRecipe().getResult().equals(netheriteIngot) ||
                    event.getRecipe().getResult().equals(beacon) ||
                    event.getRecipe().getResult().equals(shulkerBox) ||
                    event.getRecipe().getResult().equals(diamondBoots)) {
                ItemStack air = new ItemStack(Material.AIR);
                event.getInventory().setResult(air);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(event.getDamager().getType() == EntityType.ZOMBIE) {
            if(event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if(player.getHealth() > 10) {
                    player.setHealth(10);
                }
            }
        } else if(event.getEntity().getType() == EntityType.VILLAGER) {
            if(event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (player.getGameMode() != GameMode.CREATIVE && player.getWorld() == GameManager.getInstance().getVillage().getWorld()) {
                    GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId()).addCoins(-50);
                    player.sendMessage("Une amende de 50 AlphaCoins pour avoir frappé un agent de l'État !");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        if (event.getEntity() instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon) event.getEntity();
            try {
                if (dragon.getMaxHealth() >= 2000) {
                    dragon.setHealth(2000);
                }
                Objects.requireNonNull(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2000);
                dragon.setHealth(2000);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player)event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if(item != null && item.getType() != Material.AIR && event.getClickedInventory() != null &&
                (event.getClickedInventory().getHolder() instanceof Villager || event.getClickedInventory().getHolder() instanceof Player && event.getInventory().getHolder() instanceof Villager)) {
            for (VillagerEtat villager : GameManager.getInstance().getVillage().getVillagers().values()) {
                boolean villagerInventory = villager.getInventory() == event.getClickedInventory();
                if (villager.getInventory() == event.getInventory() || villagerInventory) {
                    villager.Trade(villagerInventory, item, player);
                    event.setCancelled(true);
                }
            }
        }
    }
}
