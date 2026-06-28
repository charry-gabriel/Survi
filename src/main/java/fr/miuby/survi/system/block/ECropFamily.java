package fr.miuby.survi.system.block;

import org.bukkit.Material;

/**
 * Associe chaque bloc de culture récoltable à l'item minimum garanti en drop
 * (graine, bulbe, ou fragment permettant de replanter).
 *
 * <p>Toutes les constantes de {@link MaterialUtils} liées aux cultures sont dérivées de cet enum.</p>
 */
public enum ECropFamily {
    WHEAT            (Material.WHEAT,            Material.WHEAT_SEEDS),
    CARROTS          (Material.CARROTS,          Material.CARROT),
    POTATOES         (Material.POTATOES,         Material.POTATO),
    BEETROOTS        (Material.BEETROOTS,        Material.BEETROOT_SEEDS),
    NETHER_WART      (Material.NETHER_WART,      Material.NETHER_WART),
    MELON            (Material.MELON,            Material.MELON_SLICE),
    PUMPKIN          (Material.PUMPKIN,          Material.PUMPKIN_SEEDS),
    COCOA            (Material.COCOA,            Material.COCOA_BEANS),
    CAVE_VINES       (Material.CAVE_VINES,       Material.GLOW_BERRIES),
    CAVE_VINES_PLANT (Material.CAVE_VINES_PLANT, Material.GLOW_BERRIES),
    TORCHFLOWER      (Material.TORCHFLOWER_CROP, Material.TORCHFLOWER_SEEDS),
    PITCHER_CROP     (Material.PITCHER_CROP,     Material.PITCHER_POD),
    PITCHER_PLANT    (Material.PITCHER_PLANT,    Material.PITCHER_POD);

    /** Bloc de culture récolté (cassé par le joueur). */
    public final Material crop;
    /** Item minimum garanti — évite de perdre ce qu'on a planté si le multiplicateur donne 0 drop. */
    public final Material seed;

    ECropFamily(Material crop, Material seed) {
        this.crop = crop;
        this.seed = seed;
    }
}