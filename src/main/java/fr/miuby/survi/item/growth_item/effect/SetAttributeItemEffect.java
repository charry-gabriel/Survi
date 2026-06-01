package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

/**
 * Remplace (ou initialise) un attribut sur l'item à chaque montée de palier.
 *
 * <p>Supprime d'abord TOUS les modificateurs existants pour l'attribut cible,
 * puis ajoute le nouveau modificateur avec la valeur du palier courant.
 * Cela garantit que la valeur totale reflète toujours exactement le palier,
 * quelle que soit la valeur initiale posée dans {@code ECustomItem}.
 *
 * <p>Exemple d'usage — casque de mineur :
 * <pre>
 *     new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 8.0, ADD_NUMBER, EquipmentSlotGroup.HEAD)
 *     new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 14.0, ADD_NUMBER, EquipmentSlotGroup.HEAD)
 *     new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 22.0, ADD_NUMBER, EquipmentSlotGroup.HEAD)
 * </pre>
 */
public class SetAttributeItemEffect implements ItemEffect {

    private final Attribute attribute;
    private final double value;
    private final AttributeModifier.Operation operation;
    private final EquipmentSlotGroup slotGroup;

    public SetAttributeItemEffect(Attribute attribute, double value, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup) {
        this.attribute = attribute;
        this.value = value;
        this.operation = operation;
        this.slotGroup = slotGroup;
    }

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();

        // Retire tous les modificateurs existants pour cet attribut (y compris la valeur initiale d'ECustomItem)
        Collection<AttributeModifier> existing = meta.getAttributeModifiers(attribute);
        if (existing != null) {
            for (AttributeModifier mod : existing) {
                meta.removeAttributeModifier(attribute, mod);
            }
        }

        // Clé stable et unique par attribut dans l'espace du plugin
        NamespacedKey key = new NamespacedKey(
                GameManager.getInstance().getPlugin(),
                "growth_" + attribute.getKey().getKey());

        meta.addAttributeModifier(attribute, new AttributeModifier(key, value, operation, slotGroup));
        item.setItemMeta(meta);
    }
}