package fr.miuby.survi.locked_item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LockedItemsFactory {
    private final List<LockedArmorItem> armorItems = new java.util.ArrayList<>();
    private final List<LockedToolItem> toolItems = new java.util.ArrayList<>();

    public LockedItemsFactory() {
        List<ItemStack> leatherItems = new ArrayList<>();
        leatherItems.add(new ItemStack(Material.LEATHER_HELMET));
        leatherItems.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        leatherItems.add(new ItemStack(Material.LEATHER_LEGGINGS));
        leatherItems.add(new ItemStack(Material.LEATHER_BOOTS));
        armorItems.add(new LockedArmorItem(leatherItems, LockedArmorType.LEATHER));

        List<ItemStack> goldItems = new ArrayList<>();
        goldItems.add(new ItemStack(Material.GOLDEN_HELMET));
        goldItems.add(new ItemStack(Material.GOLDEN_CHESTPLATE));
        goldItems.add(new ItemStack(Material.GOLDEN_LEGGINGS));
        goldItems.add(new ItemStack(Material.GOLDEN_BOOTS));
        armorItems.add(new LockedArmorItem(goldItems, LockedArmorType.GOLD));

        List<ItemStack> mailleItems = new ArrayList<>();
        mailleItems.add(new ItemStack(Material.CHAINMAIL_HELMET));
        mailleItems.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        mailleItems.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        mailleItems.add(new ItemStack(Material.CHAINMAIL_BOOTS));
        armorItems.add(new LockedArmorItem(mailleItems, LockedArmorType.CHAINMAIL));

        List<ItemStack> ironItems = new ArrayList<>();
        ironItems.add(new ItemStack(Material.IRON_HELMET));
        ironItems.add(new ItemStack(Material.IRON_CHESTPLATE));
        ironItems.add(new ItemStack(Material.IRON_LEGGINGS));
        ironItems.add(new ItemStack(Material.IRON_BOOTS));
        armorItems.add(new LockedArmorItem(ironItems, LockedArmorType.IRON));

        List<ItemStack> diamondItems = new ArrayList<>();
        diamondItems.add(new ItemStack(Material.DIAMOND_HELMET));
        diamondItems.add(new ItemStack(Material.DIAMOND_CHESTPLATE));
        diamondItems.add(new ItemStack(Material.DIAMOND_LEGGINGS));
        diamondItems.add(new ItemStack(Material.DIAMOND_BOOTS));
        armorItems.add(new LockedArmorItem(diamondItems, LockedArmorType.DIAMOND));

        List<ItemStack> woodToolItems = new ArrayList<>();
        woodToolItems.add(new ItemStack(Material.WOODEN_AXE));
        woodToolItems.add(new ItemStack(Material.WOODEN_HOE));
        woodToolItems.add(new ItemStack(Material.WOODEN_PICKAXE));
        woodToolItems.add(new ItemStack(Material.WOODEN_SHOVEL));
        woodToolItems.add(new ItemStack(Material.WOODEN_SWORD));
        toolItems.add(new LockedToolItem(woodToolItems, LockedToolType.WOOD));

        List<ItemStack> stoneToolItems = new ArrayList<>();
        stoneToolItems.add(new ItemStack(Material.STONE_AXE));
        stoneToolItems.add(new ItemStack(Material.STONE_HOE));
        stoneToolItems.add(new ItemStack(Material.STONE_PICKAXE));
        stoneToolItems.add(new ItemStack(Material.STONE_SHOVEL));
        stoneToolItems.add(new ItemStack(Material.STONE_SWORD));
        toolItems.add(new LockedToolItem(stoneToolItems, LockedToolType.STONE));

        List<ItemStack> ironToolItems = new ArrayList<>();
        ironToolItems.add(new ItemStack(Material.IRON_AXE));
        ironToolItems.add(new ItemStack(Material.IRON_HOE));
        ironToolItems.add(new ItemStack(Material.IRON_PICKAXE));
        ironToolItems.add(new ItemStack(Material.IRON_SHOVEL));
        ironToolItems.add(new ItemStack(Material.IRON_SWORD));
        toolItems.add(new LockedToolItem(ironToolItems, LockedToolType.IRON));

        List<ItemStack> goldToolItems = new ArrayList<>();
        goldToolItems.add(new ItemStack(Material.GOLDEN_AXE));
        goldToolItems.add(new ItemStack(Material.GOLDEN_HOE));
        goldToolItems.add(new ItemStack(Material.GOLDEN_PICKAXE));
        goldToolItems.add(new ItemStack(Material.GOLDEN_SHOVEL));
        goldToolItems.add(new ItemStack(Material.GOLDEN_SWORD));
        toolItems.add(new LockedToolItem(goldToolItems, LockedToolType.GOLD));

        List<ItemStack> diamondToolItems = new ArrayList<>();
        diamondToolItems.add(new ItemStack(Material.DIAMOND_AXE));
        diamondToolItems.add(new ItemStack(Material.DIAMOND_HOE));
        diamondToolItems.add(new ItemStack(Material.DIAMOND_PICKAXE));
        diamondToolItems.add(new ItemStack(Material.DIAMOND_SHOVEL));
        diamondToolItems.add(new ItemStack(Material.DIAMOND_SWORD));
        toolItems.add(new LockedToolItem(diamondToolItems, LockedToolType.DIAMOND));
    }

    public boolean isLocked(ItemStack item) {
        for (LockedItem lockedItem : armorItems) {
            for (ItemStack itemStack : lockedItem.getItems()) {
                if(itemStack.getType() == item.getType()) {
                    return lockedItem.isLocked();
                }
            }
        }
        for (LockedItem lockedItem : toolItems) {
            for (ItemStack itemStack : lockedItem.getItems()) {
                if(itemStack.getType() == item.getType()) {
                    return lockedItem.isLocked();
                }
            }
        }
        return false;
    }

    public void unlockArmorItem(LockedArmorType itemType) {
        for (LockedArmorItem lockedItem : armorItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.setLocked(false);
            }
        }
    }

    public void unlockToolItem(LockedToolType itemType) {
        for (LockedToolItem lockedItem : toolItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.setLocked(false);
            }
        }
    }
}
