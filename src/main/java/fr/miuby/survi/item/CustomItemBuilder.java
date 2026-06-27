package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class CustomItemBuilder {
    private final ItemStack itemStack;
    private final String itemKeyId;

    public CustomItemBuilder(ItemStack itemStack, String itemKeyId) {
        this.itemStack = itemStack;
        this.itemKeyId = itemKeyId;
    }

    public CustomItemBuilder name(String name, NamedTextColor color) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text(name, Style.style(color).decoration(TextDecoration.ITALIC, false)));
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder lore(List<Component> list) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.lore(list);
        itemStack.setItemMeta(meta);
        return this;
    }


    public CustomItemBuilder addAttribute(Attribute attr, double value, AttributeModifier.Operation op, EquipmentSlotGroup slot) {
        String modifierName = sanitizeKey(itemKeyId)
                + "_" + itemStack.getType().getKey().getKey()
                + "_" + attr.getKey().getKey().replace(".", "_");

        ItemMeta meta = itemStack.getItemMeta();
        meta.addAttributeModifier(attr, new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), modifierName), value, op, slot));
        itemStack.setItemMeta(meta);
        return this;
    }

    private static String sanitizeKey(String input) {
        String withoutDiacritics = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
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

    /** Modèle/texture custom (composant {@code minecraft:item_model}) — défini dans le resource pack côté client. */
    public CustomItemBuilder itemModel(NamespacedKey model) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setItemModel(model);
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder maxStackSize(int maxStackSize) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setMaxStackSize(maxStackSize);
        itemStack.setItemMeta(meta);
        return this;
    }

    public CustomItemBuilder maxDurability(int durability) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setMaxDamage(durability);
        }
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