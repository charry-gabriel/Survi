package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.text.Normalizer;

public class CustomItemBuilder {
    private final ItemStack itemStack;
    private final String itemBaseName;

    public CustomItemBuilder(ItemStack itemStack, String itemBaseName) {
        this.itemStack = itemStack;
        this.itemBaseName = itemBaseName;
    }

    public CustomItemBuilder name(String name, NamedTextColor color) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text(name, color));
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder addAttribute(Attribute attr, double value, AttributeModifier.Operation op, EquipmentSlotGroup slot) {
        String modifierName = sanitizeKey(itemBaseName)
                + attr.getKey().getKey().replace("_", "").substring(0, 1).toUpperCase()
                + attr.getKey().getKey().replace("_", "").substring(1);

        ItemMeta meta = itemStack.getItemMeta();
        meta.addAttributeModifier(attr, new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), modifierName), value, op, slot));
        itemStack.setItemMeta(meta);
        return this;
    }

    private static String sanitizeKey(String input) {
        String withoutDiacritics = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutDiacritics.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    public CustomItemBuilder addItemFlag(ItemFlag flag) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(flag);
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder unbreakable() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setUnbreakable(true);
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder leatherArmor(TrimMaterial material, TrimPattern pattern, Color color) {
        ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
        armorMeta.setTrim(new ArmorTrim(material, pattern));
        itemStack.setItemMeta(armorMeta);

        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        leatherArmorMeta.setColor(color);
        itemStack.setItemMeta(leatherArmorMeta);
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }
}