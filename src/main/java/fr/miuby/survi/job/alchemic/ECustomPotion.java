package fr.miuby.survi.job.alchemic;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Les 5 potions inédites du Pêcheur (Alchimiste), uniquement pêchables.
 *
 * <p>Chaque potion est identifiée par un PDC {@code survi:custom_potion_type} = nom de l'enum.
 * Les effets ne sont <b>pas</b> stockés en PotionMeta vanilla — ils sont déclenchés
 * programmatiquement par {@link AlchemicPotionListener}.</p>
 *
 * <ul>
 *   <li><b>BOUCLIER</b>   — absorbe les 3 prochains coups reçus, puis se dissipe.</li>
 *   <li><b>DEFLAGRATION</b> — (splash) dégâts AOE directs (8 dmg) sur tous les mobs dans un rayon de 6 blocs.</li>
 *   <li><b>MIASME</b>     — (splash) crée un nuage Poison III pendant 15 s.</li>
 *   <li><b>SYMBIOSE</b>   — Régénération I pendant 30 s, mais 0 dégâts en attaque pendant cette durée.</li>
 *   <li><b>FISSURE</b>    — Haste XV pendant 8 s, puis Fatigue IV pendant 2 minutes.</li>
 * </ul>
 */
public enum ECustomPotion {

    BOUCLIER(
            "Potion de Bouclier Éphémère",
            Color.fromRGB(0x4169E1),   // bleu royal
            Material.POTION
    ),
    DEFLAGRATION(
            "Potion de Déflagration",
            Color.fromRGB(0xFF4500),   // orange-rouge
            Material.POTION
    ),
    MIASME(
            "Potion de Miasme",
            Color.fromRGB(0x2E8B00),   // vert sombre
            Material.SPLASH_POTION
    ),
    SYMBIOSE(
            "Potion de Symbiose",
            Color.fromRGB(0xFF69B4),   // rose
            Material.POTION
    ),
    FISSURE(
            "Potion de Fissure",
            Color.fromRGB(0x8B4513),   // brun
            Material.POTION
    );

    // ─── PDC ─────────────────────────────────────────────────────────────────────

    /** Clé PDC posée sur chaque potion custom pour identifier son type. */
    public static NamespacedKey pdcKey() {
        return new NamespacedKey(GameManager.getInstance().getPlugin(), "custom_potion_type");
    }

    // ─── Champs ──────────────────────────────────────────────────────────────────

    private final String displayName;
    private final Color  color;
    private final Material material;

    ECustomPotion(String displayName, Color color, Material material) {
        this.displayName = displayName;
        this.color       = color;
        this.material    = material;
    }

    // ─── Fabrique ────────────────────────────────────────────────────────────────

    /** Crée un {@link ItemStack} de cette potion avec son nom et sa couleur. */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setColor(color);
        meta.customName(Component.text(displayName,
                Style.style().decoration(TextDecoration.ITALIC, false).build()));
        meta.lore(List.of(
                Component.text("§8Potion de l'Alchimiste")
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(pdcKey(), PersistentDataType.STRING, name());
        item.setItemMeta(meta);
        return item;
    }

    // ─── Détection ───────────────────────────────────────────────────────────────

    /**
     * Retourne l'enum correspondant à l'item, ou {@code null} si ce n'est pas une potion custom.
     * Compatible POTION et SPLASH_POTION.
     */
    public static ECustomPotion fromItem(ItemStack item) {
        if (item == null) return null;
        if (item.getType() != Material.POTION && item.getType() != Material.SPLASH_POTION) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(pdcKey(), PersistentDataType.STRING);
        if (id == null) return null;
        try { return valueOf(id); } catch (IllegalArgumentException e) { return null; }
    }
}
