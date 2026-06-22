package fr.miuby.survi.item;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

/**
 * Or craftée à mi-chemin fer/diamant (armure, toughness et durabilité), au lieu des
 * valeurs vanilla (plus faibles que le fer). Utilisé à la fois par {@link CustomRecipeFactory}
 * (craft) et {@link ArmorTierService} (remplacement au break).
 */
public final class GoldenArmorBonus {

    private static final Map<Material, EquipmentSlotGroup> SLOTS = Map.of(
            Material.GOLDEN_HELMET, EquipmentSlotGroup.HEAD,
            Material.GOLDEN_CHESTPLATE, EquipmentSlotGroup.CHEST,
            Material.GOLDEN_LEGGINGS, EquipmentSlotGroup.LEGS,
            Material.GOLDEN_BOOTS, EquipmentSlotGroup.FEET
    );
    private static final Map<Material, Double> ARMOR_VALUES = Map.of(
            Material.GOLDEN_HELMET, 2.5,
            Material.GOLDEN_CHESTPLATE, 7.0,
            Material.GOLDEN_LEGGINGS, 5.5,
            Material.GOLDEN_BOOTS, 2.5
    );
    private static final double TOUGHNESS = 1.0;

    private static final Map<Material, Integer> DURABILITY = Map.of(
            Material.GOLDEN_HELMET, 264,
            Material.GOLDEN_CHESTPLATE, 384,
            Material.GOLDEN_LEGGINGS, 360,
            Material.GOLDEN_BOOTS, 312
    );

    private GoldenArmorBonus() {}

    public static ItemStack apply(ItemStack item) {
        EquipmentSlotGroup slot = SLOTS.get(item.getType());
        if (slot == null) return item;

        return new CustomItemBuilder(item, "GoldenArmorTierBuff")
                .addAttribute(Attribute.ARMOR, ARMOR_VALUES.get(item.getType()), ADD_NUMBER, slot)
                .addAttribute(Attribute.ARMOR_TOUGHNESS, TOUGHNESS, ADD_NUMBER, slot)
                .maxDurability(DURABILITY.get(item.getType()))
                .build();
    }
}
