package fr.miuby.survi.item.locked_item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public class LockedItemsFactory {
    private final List<LockedArmorItem> armorItems = new java.util.ArrayList<>();
    private final List<LockedToolItem> toolItems = new java.util.ArrayList<>();

    public LockedItemsFactory() {
        List<NamespacedKey> leatherItems = new ArrayList<>();
        leatherItems.add(Material.LEATHER_HELMET.getKey());
        leatherItems.add(Material.LEATHER_CHESTPLATE.getKey());
        leatherItems.add(Material.LEATHER_LEGGINGS.getKey());
        leatherItems.add(Material.LEATHER_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(leatherItems, ELockedArmorType.LEATHER));

        List<NamespacedKey> copperItems = new ArrayList<>();
        copperItems.add(Material.COPPER_HELMET.getKey());
        copperItems.add(Material.COPPER_CHESTPLATE.getKey());
        copperItems.add(Material.COPPER_LEGGINGS.getKey());
        copperItems.add(Material.COPPER_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(copperItems, ELockedArmorType.COPPER));

        List<NamespacedKey> goldItems = new ArrayList<>();
        goldItems.add(Material.GOLDEN_HELMET.getKey());
        goldItems.add(Material.GOLDEN_CHESTPLATE.getKey());
        goldItems.add(Material.GOLDEN_LEGGINGS.getKey());
        goldItems.add(Material.GOLDEN_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(goldItems, ELockedArmorType.GOLD));

        List<NamespacedKey> mailleItems = new ArrayList<>();
        mailleItems.add(Material.CHAINMAIL_HELMET.getKey());
        mailleItems.add(Material.CHAINMAIL_CHESTPLATE.getKey());
        mailleItems.add(Material.CHAINMAIL_LEGGINGS.getKey());
        mailleItems.add(Material.CHAINMAIL_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(mailleItems, ELockedArmorType.CHAINMAIL));

        List<NamespacedKey> ironItems = new ArrayList<>();
        ironItems.add(Material.IRON_HELMET.getKey());
        ironItems.add(Material.IRON_CHESTPLATE.getKey());
        ironItems.add(Material.IRON_LEGGINGS.getKey());
        ironItems.add(Material.IRON_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(ironItems, ELockedArmorType.IRON));

        List<NamespacedKey> diamondItems = new ArrayList<>();
        diamondItems.add(Material.DIAMOND_HELMET.getKey());
        diamondItems.add(Material.DIAMOND_CHESTPLATE.getKey());
        diamondItems.add(Material.DIAMOND_LEGGINGS.getKey());
        diamondItems.add(Material.DIAMOND_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(diamondItems, ELockedArmorType.DIAMOND));

        List<NamespacedKey> netheriteItems = new ArrayList<>();
        netheriteItems.add(Material.NETHERITE_HELMET.getKey());
        netheriteItems.add(Material.NETHERITE_CHESTPLATE.getKey());
        netheriteItems.add(Material.NETHERITE_LEGGINGS.getKey());
        netheriteItems.add(Material.NETHERITE_BOOTS.getKey());
        armorItems.add(new LockedArmorItem(netheriteItems, ELockedArmorType.NETHERITE));

        List<NamespacedKey> woodToolItems = new ArrayList<>();
        woodToolItems.add(Material.WOODEN_AXE.getKey());
        woodToolItems.add(Material.WOODEN_HOE.getKey());
        woodToolItems.add(Material.WOODEN_PICKAXE.getKey());
        woodToolItems.add(Material.WOODEN_SHOVEL.getKey());
        woodToolItems.add(Material.WOODEN_SWORD.getKey());
        woodToolItems.add(Material.WOODEN_SPEAR.getKey());
        toolItems.add(new LockedToolItem(woodToolItems, ELockedToolType.WOOD));

        List<NamespacedKey> stoneToolItems = new ArrayList<>();
        stoneToolItems.add(Material.STONE_AXE.getKey());
        stoneToolItems.add(Material.STONE_HOE.getKey());
        stoneToolItems.add(Material.STONE_PICKAXE.getKey());
        stoneToolItems.add(Material.STONE_SHOVEL.getKey());
        stoneToolItems.add(Material.STONE_SWORD.getKey());
        stoneToolItems.add(Material.STONE_SPEAR.getKey());
        toolItems.add(new LockedToolItem(stoneToolItems, ELockedToolType.STONE));

        List<NamespacedKey> copperToolItems = new ArrayList<>();
        copperToolItems.add(Material.COPPER_AXE.getKey());
        copperToolItems.add(Material.COPPER_HOE.getKey());
        copperToolItems.add(Material.COPPER_PICKAXE.getKey());
        copperToolItems.add(Material.COPPER_SHOVEL.getKey());
        copperToolItems.add(Material.COPPER_SWORD.getKey());
        copperToolItems.add(Material.COPPER_SPEAR.getKey());
        toolItems.add(new LockedToolItem(copperToolItems, ELockedToolType.COPPER));

        List<NamespacedKey> ironToolItems = new ArrayList<>();
        ironToolItems.add(Material.IRON_AXE.getKey());
        ironToolItems.add(Material.IRON_HOE.getKey());
        ironToolItems.add(Material.IRON_PICKAXE.getKey());
        ironToolItems.add(Material.IRON_SHOVEL.getKey());
        ironToolItems.add(Material.IRON_SWORD.getKey());
        ironToolItems.add(Material.IRON_SPEAR.getKey());
        toolItems.add(new LockedToolItem(ironToolItems, ELockedToolType.IRON));

        List<NamespacedKey> goldToolItems = new ArrayList<>();
        goldToolItems.add(Material.GOLDEN_AXE.getKey());
        goldToolItems.add(Material.GOLDEN_HOE.getKey());
        goldToolItems.add(Material.GOLDEN_PICKAXE.getKey());
        goldToolItems.add(Material.GOLDEN_SHOVEL.getKey());
        goldToolItems.add(Material.GOLDEN_SWORD.getKey());
        goldToolItems.add(Material.GOLDEN_SPEAR.getKey());
        toolItems.add(new LockedToolItem(goldToolItems, ELockedToolType.GOLD));

        List<NamespacedKey> diamondToolItems = new ArrayList<>();
        diamondToolItems.add(Material.DIAMOND_AXE.getKey());
        diamondToolItems.add(Material.DIAMOND_HOE.getKey());
        diamondToolItems.add(Material.DIAMOND_PICKAXE.getKey());
        diamondToolItems.add(Material.DIAMOND_SHOVEL.getKey());
        diamondToolItems.add(Material.DIAMOND_SWORD.getKey());
        diamondToolItems.add(Material.DIAMOND_SPEAR.getKey());
        toolItems.add(new LockedToolItem(diamondToolItems, ELockedToolType.DIAMOND));

        List<NamespacedKey> netheriteToolItems = new ArrayList<>();
        netheriteToolItems.add(Material.NETHERITE_AXE.getKey());
        netheriteToolItems.add(Material.NETHERITE_HOE.getKey());
        netheriteToolItems.add(Material.NETHERITE_PICKAXE.getKey());
        netheriteToolItems.add(Material.NETHERITE_SHOVEL.getKey());
        netheriteToolItems.add(Material.NETHERITE_SWORD.getKey());
        netheriteToolItems.add(Material.NETHERITE_SPEAR.getKey());
        toolItems.add(new LockedToolItem(netheriteToolItems, ELockedToolType.NETHERITE));
    }

    public boolean isLocked(NamespacedKey item) {
        for (LockedItem lockedItem : armorItems) {
            for (NamespacedKey lockedNsKey : lockedItem.getItems()) {
                if (lockedNsKey.toString().equals(GameManager.getInstance().getCustomRecipeFactory().getOldNamespaceKeyOrDefault(item).toString()))
                    return lockedItem.isLocked();
            }
        }
        for (LockedItem lockedItem : toolItems) {
            for (NamespacedKey lockedNsKey : lockedItem.getItems()) {
                if (lockedNsKey.toString().equals(GameManager.getInstance().getCustomRecipeFactory().getOldNamespaceKeyOrDefault(item).toString()))
                    return lockedItem.isLocked();
            }
        }
        return false;
    }

    public void unlockArmorItem(AlphaPlayer player, ELockedArmorType itemType) {
        for (LockedArmorItem lockedItem : armorItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.unlock();

                player.getPlayer().discoverRecipes(lockedItem.getItems());
            }
        }

        player.getPlayer().discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());
    }

    public void unlockToolItem(AlphaPlayer player, ELockedToolType itemType) {
        for (LockedToolItem lockedItem : toolItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.unlock();

                player.getPlayer().discoverRecipes(lockedItem.getItems());
            }
        }

        player.getPlayer().discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());
    }

    public void lockArmorItem(ELockedArmorType itemType) {
        for (LockedArmorItem lockedItem : armorItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.lock();
            }
        }
    }

    public void lockToolItem(ELockedToolType itemType) {
        for (LockedToolItem lockedItem : toolItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.lock();
            }
        }
    }
}