package fr.miuby.survi.item.locked_item;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;

/**
 * Tiers d'armure, du plus faible au plus fort (ordre déclaratif = ordre de progression,
 * aligné sur la chaîne d'amélioration de {@code recipes.yml}).
 */
public enum ELockedArmorType {
    LEATHER(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
    COPPER(Material.COPPER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS),
    CHAINMAIL(Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
    IRON(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS),
    GOLD(Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS),
    DIAMOND(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
    NETHERITE(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);

    private final Map<EquipmentSlot, Material> pieces;

    ELockedArmorType(Material helmet, Material chestplate, Material leggings, Material boots) {
        this.pieces = Map.of(
                EquipmentSlot.HEAD, helmet,
                EquipmentSlot.CHEST, chestplate,
                EquipmentSlot.LEGS, leggings,
                EquipmentSlot.FEET, boots
        );
    }

    public Material forSlot(EquipmentSlot slot) {
        return pieces.get(slot);
    }

    public static ELockedArmorType ofMaterial(Material material) {
        for (ELockedArmorType type : values()) {
            if (type.pieces.containsValue(material)) return type;
        }
        return null;
    }

    public static EquipmentSlot slotOf(Material material) {
        for (ELockedArmorType type : values()) {
            for (Map.Entry<EquipmentSlot, Material> entry : type.pieces.entrySet()) {
                if (entry.getValue() == material) return entry.getKey();
            }
        }
        return null;
    }
}
