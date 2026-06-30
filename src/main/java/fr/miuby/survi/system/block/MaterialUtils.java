package fr.miuby.survi.system.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Constantes {@link Material} et méthodes utilitaires partagées entre les listeners de métier,
 * intégralement dérivées de {@link ELogFamily}, {@link EOreFamily} et {@link EPlantFamily}.
 *
 * <p>Pour ajouter un nouveau type de bois, minerai ou culture : modifier uniquement
 * l'enum correspondant — toutes les constantes et méthodes ci-dessous se mettent à jour automatiquement.</p>
 */
public final class MaterialUtils {
    private MaterialUtils() {}

    public static final BlockFace[] ORTHO_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    // ═══════════════════════════════════════════════════════════════════════════
    //  Logs — dérivés de ELogFamily
    // ═══════════════════════════════════════════════════════════════════════════

    /** Tous les logs susceptibles de passer dans le système bûcheron (naturels + stripped). */
    public static final Set<Material> LOG_BLOCKS;
    /** Logs strippables (non-stripped uniquement) — pour bloquer le stripping avant niv.4. */
    public static final Set<Material> STRIPPABLE_LOG_BLOCKS;
    /** Log ou stripped log → sapling correspondant pour l'auto-replant. */
    public static final Map<Material, Material> LOG_TO_SAPLING;
    /** Tous les saplings et propagules (un par famille {@link ELogFamily}) + {@link #AZALEA_SAPLINGS}. */
    public static final Set<Material> SAPLING_MATERIALS;

    /**
     * Azalée et azalée fleurie : se plantent et se font pousser à la farine d'os comme un sapling,
     * mais l'arbre obtenu utilise le chêne ({@code OAK_LOG}) et leurs propres feuilles
     * ({@code AZALEA_LEAVES} / {@code FLOWERING_AZALEA_LEAVES}, déjà dans {@link #APPLE_LEAF_BLOCKS}).
     * Pas de log dédié → pas de famille {@link ELogFamily} ; ajoutées directement à
     * {@link #SAPLING_MATERIALS} (donc {@link #CROP_BLOCKS} et {@link #SEED_ITEMS}) pour que la
     * plantation, la croissance et la farine d'os soient gérées comme un sapling (niveau Bûcheron).
     */
    public static final Set<Material> AZALEA_SAPLINGS = EnumSet.of(
            Material.AZALEA, Material.FLOWERING_AZALEA
    );

    static {
        EnumSet<Material> all      = EnumSet.noneOf(Material.class);
        EnumSet<Material> natural  = EnumSet.noneOf(Material.class);
        EnumSet<Material> saplings = EnumSet.noneOf(Material.class);
        Map<Material, Material> toSapling = new EnumMap<>(Material.class);
        for (ELogFamily f : ELogFamily.values()) {
            all.add(f.log);          all.add(f.wood);
            all.add(f.strippedLog);  all.add(f.strippedWood);
            natural.add(f.log);      natural.add(f.wood);
            saplings.add(f.sapling);
            toSapling.put(f.log,         f.sapling);
            toSapling.put(f.strippedLog, f.sapling);
        }
        saplings.addAll(AZALEA_SAPLINGS);
        LOG_BLOCKS            = Collections.unmodifiableSet(all);
        STRIPPABLE_LOG_BLOCKS = Collections.unmodifiableSet(natural);
        LOG_TO_SAPLING        = Collections.unmodifiableMap(toSapling);
        SAPLING_MATERIALS     = Collections.unmodifiableSet(saplings);
    }

    public static final Set<Material> APPLE_LEAF_BLOCKS = EnumSet.of(
            Material.OAK_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
    );

    public static final Set<Material> AXE_MATERIALS = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    public static final Set<Material> SOIL_BLOCKS = EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.FARMLAND, Material.ROOTED_DIRT,
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS,
            Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  Minerais — dérivés de EOreFamily
    // ═══════════════════════════════════════════════════════════════════════════

    /** Tous les blocs de minerai (pierre, deepslate, nether, ancient_debris). */
    public static final Set<Material> ORE_BLOCKS;
    /**
     * Bloc de minerai → {@link EOreFamily}.
     * Utilisé par le vein miner pour regrouper les variants pierre/deepslate d'un même filon.
     * Les ores nether-only ont leur propre famille (ex. {@code NETHER_GOLD} ≠ {@code GOLD}).
     */
    public static final Map<Material, EOreFamily> ORE_TO_FAMILY;
    /**
     * Item droppé normalement → {@link EOreFamily}.
     * Clé = {@link EOreFamily#drop} de chaque famille (ex. {@code DIAMOND}, {@code RAW_IRON}…).
     *
     * <p>Utilisé par {@code JobUtils.dropMineWithMultiplier} pour distinguer un drop légitime
     * (à multiplier) d'un drop Silk Touch (le bloc lui-même, absent de cette map) ou d'un drop
     * non-minerai (à laisser passer à 1×).</p>
     *
     * <p>Exception notable : {@code ANCIENT_DEBRIS} est à la fois le bloc et son propre drop —
     * il figure donc dans cette map et reçoit le multiplicateur Mineur lors d'un cassage naturel.
     * Les blocs posés par un joueur sont exclus en amont via {@code PlacedBlockTracker}.</p>
     */
    public static final Map<Material, EOreFamily> ORE_DROP_TO_FAMILY;

    static {
        EnumSet<Material> ores = EnumSet.noneOf(Material.class);
        Map<Material, EOreFamily> toFamily   = new EnumMap<>(Material.class);
        Map<Material, EOreFamily> dropToFamily = new EnumMap<>(Material.class);
        for (EOreFamily f : EOreFamily.values()) {
            ores.add(f.ore);
            toFamily.put(f.ore, f);
            if (f.deepslate != null) {
                ores.add(f.deepslate);
                toFamily.put(f.deepslate, f);
            }
            dropToFamily.put(f.drop, f);
        }
        ORE_BLOCKS        = Collections.unmodifiableSet(ores);
        ORE_TO_FAMILY     = Collections.unmodifiableMap(toFamily);
        ORE_DROP_TO_FAMILY = Collections.unmodifiableMap(dropToFamily);
    }

    public static final Set<Material> PICKAXE_MATERIALS = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  Cultures — dérivées de ECropFamily + ELogFamily
    // ═══════════════════════════════════════════════════════════════════════════

    /** Blocs de culture récoltables par FarmerListener (matures ou semi-matures). */
    public static final Set<Material> HARVEST_CROPS;
    /**
     * Sous-ensemble de {@link #HARVEST_CROPS} pour les plantes posées directement par le joueur
     * et qui poussent en colonne (canne à sucre, cactus, bambou).
     * La base est tracée par {@code PlacedBlockTracker} ; les blocs ayant poussé au-dessus ne le sont pas.
     * Ces blocs utilisent {@code isPlaced()} comme garde dans les listeners, à la place de {@code isFullyGrown()}.
     */
    public static final Set<Material> COLUMN_HARVEST_CROPS;
    /**
     * Culture récoltable → item minimum garanti lors d'une récolte.
     * Évite de perdre ce qu'on a planté quand le multiplicateur aboutit à 0 drop.
     */
    public static final Map<Material, Material> CROP_SEED;
    /**
     * Tous les blocs qui peuvent pousser ou être plantés dans le monde
     * (cultures, tiges, champignons, arbustes, plantes libres, saplings).
     * Utilisé par CropGrowthListener pour tracker la croissance.
     */
    public static final Set<Material> CROP_BLOCKS;
    /**
     * Tous les items qu'un joueur peut tenir pour planter quelque chose
     * (graines, tubercules, boutures, saplings, spores, etc.).
     * Inclut les saplings de {@link ELogFamily} et {@link #AZALEA_SAPLINGS}.
     */
    public static final Set<Material> SEED_ITEMS;
    /**
     * Bloc de culture → cible {@link Material} pour les quêtes {@code HARVEST_CROP}.
     * {@code CAVE_VINES_PLANT} est normalisé sur {@code CAVE_VINES} (encodé dans {@link EPlantFamily}).
     * N'inclut pas les blocs dont {@link EPlantFamily#questTarget} est {@code null}.
     */
    public static final Map<Material, Material> QUEST_CROP_TARGET;

    static {
        EnumSet<Material> harvestCrops       = EnumSet.noneOf(Material.class);
        EnumSet<Material> columnHarvestCrops = EnumSet.noneOf(Material.class);
        EnumSet<Material> cropBlocks         = EnumSet.noneOf(Material.class);
        EnumSet<Material> seedItems          = EnumSet.noneOf(Material.class);
        Map<Material, Material> cropSeed     = new EnumMap<>(Material.class);
        Map<Material, Material> questMap     = new EnumMap<>(Material.class);

        for (EPlantFamily f : EPlantFamily.values()) {
            if (f.crop != null) {
                cropBlocks.add(f.crop);
                if (f.seed != null) {
                    harvestCrops.add(f.crop);
                    cropSeed.put(f.crop, f.seed);
                    if (f.columnGrowth) columnHarvestCrops.add(f.crop);
                }
                if (f.questTarget != null) {
                    questMap.put(f.crop, f.questTarget);
                }
            }
            if (f.plant != null) seedItems.add(f.plant);
        }

        // Les saplings (ELogFamily + AZALEA_SAPLINGS) sont à la fois des blocs en croissance et des items plantables
        cropBlocks.addAll(SAPLING_MATERIALS);
        seedItems.addAll(SAPLING_MATERIALS);

        HARVEST_CROPS        = Collections.unmodifiableSet(harvestCrops);
        COLUMN_HARVEST_CROPS = Collections.unmodifiableSet(columnHarvestCrops);
        CROP_SEED            = Collections.unmodifiableMap(cropSeed);
        CROP_BLOCKS          = Collections.unmodifiableSet(cropBlocks);
        SEED_ITEMS           = Collections.unmodifiableSet(seedItems);
        QUEST_CROP_TARGET    = Collections.unmodifiableMap(questMap);
    }

    // ─── Méthodes utilitaires cultures (anciennement PlantedCropUtils) ───────

    /** {@code true} si {@code m} est un bloc qui pousse ou a été planté dans le monde. */
    public static boolean isCrop(Material m)      { return CROP_BLOCKS.contains(m); }

    /** {@code true} si {@code m} est un item qu'un joueur peut tenir pour planter. */
    public static boolean isSeed(Material m)      { return SEED_ITEMS.contains(m); }

    /** {@code true} si {@code m} est un sapling, propagule ou azalée (voir {@link #SAPLING_MATERIALS}). */
    public static boolean isSapling(Material m)   { return SAPLING_MATERIALS.contains(m); }

    /**
     * {@code true} si {@code m} peut être planté par un joueur.
     * Couvre graines, tubercules, saplings, boutures, champignons, plantes libres et algues.
     * Équivalent à {@link #isSeed} — toutes les choses plantables sont dans {@code SEED_ITEMS}.
     */
    public static boolean isPlantable(Material m) { return SEED_ITEMS.contains(m); }

    // ─── Maturité ─────────────────────────────────────────────────────────────

    /**
     * {@code true} si {@code block} est à maturité — une graine fraîchement plantée ne doit pas compter.
     *
     * <p>Les blocs non {@code Ageable} (ex. {@code MELON}, {@code PUMPKIN} — le fruit formé, pas la tige)
     * sont considérés mûrs par nature : il n'existe pas de version "graine" de ces blocs eux-mêmes.</p>
     */
    public static boolean isFullyGrown(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return true;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    /**
     * {@code true} si casser ce bloc compte comme un cassage de minerai légitime pour le Mineur.
     * Exclut les minerais posés manuellement par un joueur.
     *
     * @param block    le bloc cassé
     * @param isPlaced résultat de {@code PlacedBlockTracker.isPlaced(block)} pour ce bloc
     */
    public static boolean isLegitimateMineBreak(Block block, boolean isPlaced) {
        return ORE_BLOCKS.contains(block.getType()) && !isPlaced;
    }

    /**
     * {@code true} si casser ce bloc compte comme un cassage de bûche légitime pour le Bûcheron.
     * Exclut les bûches posées manuellement par un joueur.
     *
     * @param block    le bloc cassé
     * @param isPlaced résultat de {@code PlacedBlockTracker.isPlaced(block)} pour ce bloc
     */
    public static boolean isLegitimateLumberBreak(Block block, boolean isPlaced) {
        return LOG_BLOCKS.contains(block.getType()) && !isPlaced;
    }

    /**
     * {@code true} si casser ce bloc compte comme une récolte légitime du Fermier.
     *
     * <ul>
     *   <li>Cultures {@code Ageable} (blé, carotte…) : le bloc doit être à maturité complète.</li>
     *   <li>Plantes en colonne (canne à sucre, cactus, bambou) : le bloc ne doit pas avoir été
     *       posé par le joueur — la base placée est tracée par {@code PlacedBlockTracker},
     *       seuls les blocs ayant poussé au-dessus ne le sont pas.</li>
     * </ul>
     *
     * @param block    le bloc cassé
     * @param isPlaced résultat de {@code PlacedBlockTracker.isPlaced(block)} pour ce bloc
     */
    public static boolean isLegitimateHarvest(Block block, boolean isPlaced) {
        if (!HARVEST_CROPS.contains(block.getType())) return false;
        if (COLUMN_HARVEST_CROPS.contains(block.getType())) return !isPlaced;
        return isFullyGrown(block);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Mob passive
    // ═══════════════════════════════════════════════════════════════════════════

    public static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.BEE, EntityType.DONKEY, EntityType.STRIDER, EntityType.AXOLOTL,
            EntityType.MOOSHROOM, EntityType.CAT, EntityType.PIG, EntityType.HORSE,
            EntityType.GOAT, EntityType.CAMEL, EntityType.FROG, EntityType.HOGLIN,
            EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.RABBIT, EntityType.WOLF,
            EntityType.SHEEP, EntityType.MULE, EntityType.OCELOT, EntityType.PANDA,
            EntityType.CHICKEN, EntityType.FOX, EntityType.SNIFFER, EntityType.ARMADILLO,
            EntityType.TURTLE, EntityType.COW
    );
}