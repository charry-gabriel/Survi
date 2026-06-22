package fr.miuby.survi.item;

import fr.miuby.survi.item.locked_item.ELockedArmorType;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Détermine le tier d'armure inférieur à donner en remplacement quand une pièce casse.
 * L'ordre des tiers (cuir → cuivre → maille → fer → or → diamant → netherite) et les
 * matériaux associés vivent dans {@link ELockedArmorType}, seule source de vérité.
 */
public final class ArmorTierService {

    private ArmorTierService() {}

    public static EquipmentSlot slotFor(Material armorMaterial) {
        return ELockedArmorType.slotOf(armorMaterial);
    }

    /**
     * @return l'item du tier inférieur (avec le bonus or si applicable), ou {@code null} si
     * {@code brokenMaterial} n'est pas une armure de la chaîne ou est déjà le tier le plus bas (cuir).
     */
    public static ItemStack getLowerTierReplacement(Material brokenMaterial) {
        ELockedArmorType tier = ELockedArmorType.ofMaterial(brokenMaterial);
        if (tier == null || tier.ordinal() == 0) return null;

        ELockedArmorType lowerTier = ELockedArmorType.values()[tier.ordinal() - 1];
        EquipmentSlot slot = ELockedArmorType.slotOf(brokenMaterial);
        Material lowerMaterial = lowerTier.forSlot(slot);

        return lowerTier == ELockedArmorType.GOLD
                ? GoldenArmorBonus.apply(new ItemStack(lowerMaterial))
                : new ItemStack(lowerMaterial);
    }
}
