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
 * Constantes {@link Material} partagées entre les listeners de métier,
 * intégralement dérivées de {@link ELogFamily}, {@link EOreFamily} et {@link ECropFamily}.
 *
 * <p>Pour ajouter un nouveau type de bois, minerai ou culture : modifier uniquement
 * l'enum correspondant — toutes les constantes ci-dessous se mettent à jour automatiquement.</p>
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

    static {
        EnumSet<Material> all     = EnumSet.noneOf(Material.class);
        EnumSet<Material> natural = EnumSet.noneOf(Material.class);
        Map<Material, Material> toSapling = new EnumMap<>(Material.class);
        for (ELogFamily f : ELogFamily.values()) {
            all.add(f.log);        all.add(f.wood);
            all.add(f.strippedLog); all.add(f.strippedWood);
            natural.add(f.log);    natural.add(f.wood);
            toSapling.put(f.log,        f.sapling);
            toSapling.put(f.strippedLog, f.sapling);
        }
        LOG_BLOCKS            = Collections.unmodifiableSet(all);
        STRIPPABLE_LOG_BLOCKS = Collections.unmodifiableSet(natural);
        LOG_TO_SAPLING        = Collections.unmodifiableMap(toSapling);
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
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS
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

    static {
        EnumSet<Material> ores = EnumSet.noneOf(Material.class);
        Map<Material, EOreFamily> toFamily = new EnumMap<>(Material.class);
        for (EOreFamily f : EOreFamily.values()) {
            ores.add(f.ore);
            toFamily.put(f.ore, f);
            if (f.deepslate != null) {
                ores.add(f.deepslate);
                toFamily.put(f.deepslate, f);
            }
        }
        ORE_BLOCKS    = Collections.unmodifiableSet(ores);
        ORE_TO_FAMILY = Collections.unmodifiableMap(toFamily);
    }

    public static final Set<Material> PICKAXE_MATERIALS = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  Cultures — dérivées de ECropFamily
    // ═══════════════════════════════════════════════════════════════════════════

    /** Blocs de culture récoltables (matures ou semi-matures). */
    public static final Set<Material> HARVEST_CROPS;
    /**
     * Culture → item minimum garanti lors d'une récolte.
     * Évite de perdre ce qu'on a planté quand le multiplicateur aboutit à 0 drop.
     */
    public static final Map<Material, Material> CROP_SEED;

    static {
        EnumSet<Material> crops = EnumSet.noneOf(Material.class);
        Map<Material, Material> toSeed = new EnumMap<>(Material.class);
        for (ECropFamily f : ECropFamily.values()) {
            crops.add(f.crop);
            toSeed.put(f.crop, f.seed);
        }
        HARVEST_CROPS = Collections.unmodifiableSet(crops);
        CROP_SEED     = Collections.unmodifiableMap(toSeed);
    }

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