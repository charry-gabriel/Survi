package fr.miuby.survi.system.block;

import org.bukkit.Material;

/**
 * Regroupe tous les matériaux de culture : bloc en jeu, drop minimum garanti,
 * item pour planter et cible normalisée pour les quêtes.
 *
 * <p>Toutes les constantes de {@link MaterialUtils} liées aux cultures sont dérivées de cet enum.
 * Pour ajouter une culture : une entrée ici suffit — aucune modification ailleurs.</p>
 *
 * <ul>
 *   <li>{@code crop}        — bloc présent dans le monde.</li>
 *   <li>{@code seed}        — drop minimum garanti à la récolte ; {@code null} = non géré par FarmerListener.</li>
 *   <li>{@code plant}       — item tenu pour planter ; {@code null} = non plantable directement.</li>
 *   <li>{@code questTarget} — {@link Material} transmis à {@code progressQuest} pour les quêtes
 *                             {@code HARVEST_CROP} ; {@code null} = non tracké. La normalisation
 *                             {@code CAVE_VINES_PLANT → CAVE_VINES} est encodée ici.</li>
 * </ul>
 *
 * <p>Les saplings sont gérés par {@link ELogFamily#sapling} et non par cet enum.</p>
 */
public enum EPlantFamily {

    // ─── Cultures récoltables (FarmerListener) ──────────────────────────────────────
    //  Constructeur 3-arg → questTarget = crop (auto).
    //  MELON : plant ≠ seed (on plante MELON_SEEDS, le drop minimum est MELON_SLICE).
    WHEAT            (Material.WHEAT,             Material.WHEAT_SEEDS,       Material.WHEAT_SEEDS),
    CARROTS          (Material.CARROTS,           Material.CARROT,            Material.CARROT),
    POTATOES         (Material.POTATOES,          Material.POTATO,            Material.POTATO),
    BEETROOTS        (Material.BEETROOTS,         Material.BEETROOT_SEEDS,    Material.BEETROOT_SEEDS),
    NETHER_WART      (Material.NETHER_WART,       Material.NETHER_WART,       Material.NETHER_WART),
    MELON            (Material.MELON,             Material.MELON_SLICE,       Material.MELON_SEEDS),
    PUMPKIN          (Material.PUMPKIN,           Material.PUMPKIN_SEEDS,     Material.PUMPKIN_SEEDS),
    COCOA            (Material.COCOA,             Material.COCOA_BEANS,       Material.COCOA_BEANS),
    CAVE_VINES       (Material.CAVE_VINES,        Material.GLOW_BERRIES,      Material.GLOW_BERRIES),
    TORCHFLOWER      (Material.TORCHFLOWER_CROP,  Material.TORCHFLOWER_SEEDS, Material.TORCHFLOWER_SEEDS),
    PITCHER_CROP     (Material.PITCHER_CROP,      Material.PITCHER_POD,       Material.PITCHER_POD),

    // 4-arg : CAVE_VINES_PLANT → normalisation sur CAVE_VINES pour les quêtes.
    CAVE_VINES_PLANT (Material.CAVE_VINES_PLANT,  Material.GLOW_BERRIES, null, Material.CAVE_VINES),
    // 4-arg : PITCHER_PLANT est récolté par FarmerListener mais non tracké en quête.
    PITCHER_PLANT    (Material.PITCHER_PLANT,     Material.PITCHER_POD,  null, null),

    // ─── Tiges (en croissance, non récoltées par FarmerListener) ─────────────────────
    //  Contribuent à CROP_BLOCKS uniquement. L'item planté est déjà couvert par MELON/PUMPKIN.
    MELON_STEM            (Material.MELON_STEM,            null, null, null),
    MELON_STEM_ATTACHED   (Material.ATTACHED_MELON_STEM,   null, null, null),
    PUMPKIN_STEM          (Material.PUMPKIN_STEM,          null, null, null),
    PUMPKIN_STEM_ATTACHED (Material.ATTACHED_PUMPKIN_STEM, null, null, null),

    // ─── Arbustes ────────────────────────────────────────────────────────────────────
    SWEET_BERRY      (Material.SWEET_BERRY_BUSH, null, Material.SWEET_BERRIES, Material.SWEET_BERRY_BUSH),

    // ─── Champignons / fungi ─────────────────────────────────────────────────────────
    //  Overworld : trackés en quête. Nether : non trackés.
    BROWN_MUSHROOM   (Material.BROWN_MUSHROOM, null, Material.BROWN_MUSHROOM, Material.BROWN_MUSHROOM),
    RED_MUSHROOM     (Material.RED_MUSHROOM,   null, Material.RED_MUSHROOM,   Material.RED_MUSHROOM),
    CRIMSON_FUNGUS   (Material.CRIMSON_FUNGUS, null, Material.CRIMSON_FUNGUS, null),
    WARPED_FUNGUS    (Material.WARPED_FUNGUS,  null, Material.WARPED_FUNGUS,  null),

    // ─── Plantes en croissance libre ─────────────────────────────────────────────────
    // columnGrowth = true : la base est posée par le joueur (BlockPlaceEvent → tracée),
    // seuls les blocs qui ont poussé au-dessus ne le sont pas → isPlaced() suffit comme garde.
    SUGAR_CANE       (Material.SUGAR_CANE,      Material.SUGAR_CANE, Material.SUGAR_CANE, Material.SUGAR_CANE, true),
    CACTUS           (Material.CACTUS,          Material.CACTUS,     Material.CACTUS,     Material.CACTUS,     true),
    BAMBOO           (Material.BAMBOO,          Material.BAMBOO,     Material.BAMBOO,     null,                true),
    KELP             (Material.KELP,            null, Material.KELP,            null),
    KELP_PLANT       (Material.KELP_PLANT,      null, null,               null),
    TWISTING_VINES   (Material.TWISTING_VINES,  null, Material.TWISTING_VINES,  null),
    WEEPING_VINES    (Material.WEEPING_VINES,   null, Material.WEEPING_VINES,   null),
    GLOW_LICHEN      (Material.GLOW_LICHEN,     null, Material.GLOW_LICHEN,     null),
    SEAGRASS         (Material.SEAGRASS,        null, Material.SEAGRASS,        null);

    /** Bloc présent dans le monde. */
    public final Material crop;
    /** Drop minimum garanti à la récolte ; {@code null} = non géré par FarmerListener. */
    public final Material seed;
    /** Item tenu pour planter ; {@code null} = non plantable directement. */
    public final Material plant;
    /**
     * Cible transmise à {@code progressQuest} pour les quêtes {@code HARVEST_CROP}.
     * {@code null} = ce bloc n'est pas tracké. Peut différer de {@code crop} pour les
     * normalisations (ex. {@code CAVE_VINES_PLANT → CAVE_VINES}).
     */
    public final Material questTarget;
    /**
     * {@code true} pour les plantes posées directement par le joueur et qui poussent en colonne
     * (canne à sucre, cactus, bambou). La base est tracée par {@code PlacedBlockTracker} ;
     * seuls les blocs ayant poussé au-dessus ne le sont pas.
     * Ces plantes utilisent {@code isPlaced()} comme garde anti-exploit à la place de {@code isFullyGrown()}.
     */
    public final boolean columnGrowth;

    /** Constructeur standard : {@code questTarget = crop}, {@code columnGrowth = false}. */
    EPlantFamily(Material crop, Material seed, Material plant) {
        this(crop, seed, plant, crop, false);
    }

    /** Constructeur avec questTarget explicite, {@code columnGrowth = false}. */
    EPlantFamily(Material crop, Material seed, Material plant, Material questTarget) {
        this(crop, seed, plant, questTarget, false);
    }

    /** Constructeur complet. */
    EPlantFamily(Material crop, Material seed, Material plant, Material questTarget, boolean columnGrowth) {
        this.crop        = crop;
        this.seed        = seed;
        this.plant       = plant;
        this.questTarget = questTarget;
        this.columnGrowth = columnGrowth;
    }
}