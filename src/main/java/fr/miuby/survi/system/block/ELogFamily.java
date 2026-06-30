package fr.miuby.survi.system.block;

import org.bukkit.Material;

/**
 * Regroupe tous les variants d'un même type de bois :
 * log naturel, planche ronde (wood), stripped log, stripped wood et sapling/propagule.
 *
 * <p>Toutes les constantes de {@link MaterialUtils} liées aux logs sont dérivées de cet enum
 * — modifier une entrée ici suffit à propager le changement partout.</p>
 */
public enum ELogFamily {
    OAK     (Material.OAK_LOG,      Material.OAK_WOOD,       Material.STRIPPED_OAK_LOG,      Material.STRIPPED_OAK_WOOD,       Material.OAK_SAPLING),
    SPRUCE  (Material.SPRUCE_LOG,   Material.SPRUCE_WOOD,    Material.STRIPPED_SPRUCE_LOG,   Material.STRIPPED_SPRUCE_WOOD,    Material.SPRUCE_SAPLING),
    BIRCH   (Material.BIRCH_LOG,    Material.BIRCH_WOOD,     Material.STRIPPED_BIRCH_LOG,    Material.STRIPPED_BIRCH_WOOD,     Material.BIRCH_SAPLING),
    JUNGLE  (Material.JUNGLE_LOG,   Material.JUNGLE_WOOD,    Material.STRIPPED_JUNGLE_LOG,   Material.STRIPPED_JUNGLE_WOOD,    Material.JUNGLE_SAPLING),
    ACACIA  (Material.ACACIA_LOG,   Material.ACACIA_WOOD,    Material.STRIPPED_ACACIA_LOG,   Material.STRIPPED_ACACIA_WOOD,    Material.ACACIA_SAPLING),
    DARK_OAK(Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD,  Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD,  Material.DARK_OAK_SAPLING),
    CHERRY  (Material.CHERRY_LOG,   Material.CHERRY_WOOD,    Material.STRIPPED_CHERRY_LOG,   Material.STRIPPED_CHERRY_WOOD,    Material.CHERRY_SAPLING),
    MANGROVE(Material.MANGROVE_LOG, Material.MANGROVE_WOOD,  Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_MANGROVE_WOOD,  Material.MANGROVE_PROPAGULE),
    PALE_OAK(Material.PALE_OAK_LOG, Material.PALE_OAK_WOOD,  Material.STRIPPED_PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_WOOD,  Material.PALE_OAK_SAPLING),
    CRIMSON (Material.CRIMSON_STEM, Material.CRIMSON_HYPHAE, Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_CRIMSON_HYPHAE, Material.CRIMSON_FUNGUS),
    WARPED  (Material.WARPED_STEM,  Material.WARPED_HYPHAE,  Material.STRIPPED_WARPED_STEM,  Material.STRIPPED_WARPED_HYPHAE,  Material.WARPED_FUNGUS);

    public final Material log;
    public final Material wood;
    public final Material strippedLog;
    public final Material strippedWood;
    /** Sapling, propagule ou fungus planté lors de l'auto-replant. */
    public final Material sapling;

    ELogFamily(Material log, Material wood, Material strippedLog, Material strippedWood, Material sapling) {
        this.log = log;
        this.wood = wood;
        this.strippedLog = strippedLog;
        this.strippedWood = strippedWood;
        this.sapling = sapling;
    }
}