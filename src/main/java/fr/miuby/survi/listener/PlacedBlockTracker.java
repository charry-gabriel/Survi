package fr.miuby.survi.listener;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Traque les blocs posés par les joueurs pour éviter qu'un bloc placé puis cassé
 * (ou déplacé par piston) ne compte dans les quêtes MINE et les growth items.
 *
 * <p>Fonctionnement :</p>
 * <ul>
 *   <li>{@link BlockPlaceEvent}          → la position est enregistrée</li>
 *   <li>{@link BlockBreakEvent}          → la position est retirée (nettoyage mémoire)</li>
 *   <li>{@link BlockPistonExtendEvent}   → les positions suivies sont déplacées avec les blocs poussés</li>
 *   <li>{@link BlockPistonRetractEvent}  → les positions suivies sont déplacées avec les blocs tirés</li>
 * </ul>
 *
 * <p>Ce tracker est purement en mémoire et ne persiste pas entre les redémarrages.
 * Les blocs posés avant un restart ne sont plus tracés, mais ils sont alors
 * indiscernables de la génération naturelle.</p>
 *
 * <p><strong>Ordre d'enregistrement obligatoire :</strong> ce listener doit être enregistré
 * <em>en dernier</em> dans {@link fr.miuby.survi.Survi} afin que son handler
 * {@code onBlockBreak} (MONITOR) s'exécute après ceux de {@link QuestListener}
 * et {@link GrowthItemListener} (Bukkit respecte l'ordre d'enregistrement pour
 * les handlers de même priorité — FIFO).</p>
 */
public class PlacedBlockTracker implements Listener {

    /** clé = UUID du monde, valeur = positions de blocs posés par des joueurs (encodées en long). */
    private final Map<UUID, Set<Long>> placedBlocks = new HashMap<>();

    /**
     * Mapping log naturel → log strippé correspondant.
     * Quand Paper fire un {@link BlockPlaceEvent} suite à un stripping (clic droit hache),
     * le bloc "remplacé" est un log naturel et le bloc "posé" est son équivalent strippé.
     * Ces événements sont exclus du tracking : le stripped log résulte d'un log naturel,
     * pas d'un item posé depuis l'inventaire du joueur.
     */
    private static final Map<Material, Material> STRIP_RESULTS;
    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);
        m.put(Material.OAK_LOG,      Material.STRIPPED_OAK_LOG);
        m.put(Material.OAK_WOOD,     Material.STRIPPED_OAK_WOOD);
        m.put(Material.SPRUCE_LOG,   Material.STRIPPED_SPRUCE_LOG);
        m.put(Material.SPRUCE_WOOD,  Material.STRIPPED_SPRUCE_WOOD);
        m.put(Material.BIRCH_LOG,    Material.STRIPPED_BIRCH_LOG);
        m.put(Material.BIRCH_WOOD,   Material.STRIPPED_BIRCH_WOOD);
        m.put(Material.JUNGLE_LOG,   Material.STRIPPED_JUNGLE_LOG);
        m.put(Material.JUNGLE_WOOD,  Material.STRIPPED_JUNGLE_WOOD);
        m.put(Material.ACACIA_LOG,   Material.STRIPPED_ACACIA_LOG);
        m.put(Material.ACACIA_WOOD,  Material.STRIPPED_ACACIA_WOOD);
        m.put(Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG);
        m.put(Material.DARK_OAK_WOOD,Material.STRIPPED_DARK_OAK_WOOD);
        m.put(Material.CHERRY_LOG,   Material.STRIPPED_CHERRY_LOG);
        m.put(Material.CHERRY_WOOD,  Material.STRIPPED_CHERRY_WOOD);
        m.put(Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG);
        m.put(Material.MANGROVE_WOOD,Material.STRIPPED_MANGROVE_WOOD);
        m.put(Material.PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_LOG);
        m.put(Material.PALE_OAK_WOOD,Material.STRIPPED_PALE_OAK_WOOD);
        STRIP_RESULTS = Collections.unmodifiableMap(m);
    }

    /**
     * Quand {@code false}, {@link #isPlaced} retourne toujours {@code false} —
     * les blocs posés sont traités comme naturels (mode test quête).
     * Activé par défaut.
     */
    @Getter
    @Setter
    private static volatile boolean enabled = true;

    // ─────────────────────────────────────────────────────────────────────────────
    //  API publique — appelée par QuestListener et GrowthItemListener
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Retourne {@code true} si ce bloc a été posé par un joueur (et non généré nativement).
     * Valable pendant tout le cycle d'un {@code BlockBreakEvent}, jusqu'au handler de
     * nettoyage de ce tracker (MONITOR, enregistré en dernier).
     */
    public boolean isPlaced(Block block) {
        if (!enabled) return false;
        Set<Long> set = placedBlocks.get(block.getWorld().getUID());
        return set != null && set.contains(pack(block.getX(), block.getY(), block.getZ()));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Listeners
    // ─────────────────────────────────────────────────────────────────────────────

    /** Enregistre la position du bloc une fois le placement confirmé (non annulé).
     * Les actions de stripping (log → stripped log par clic droit hache) sont exclues :
     * Paper fire un {@link BlockPlaceEvent} pour ces transformations, mais le bloc résultant
     * provient d'un log naturel et ne doit pas être considéré comme "posé par le joueur". */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material placedType = block.getType();
        Material replacedType = event.getBlockReplacedState().getType();
        if (STRIP_RESULTS.get(replacedType) == placedType) return;
        placedBlocks
                .computeIfAbsent(block.getWorld().getUID(), k -> new HashSet<>())
                .add(pack(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * Retire la position après la casse (nettoyage mémoire).
     * S'exécute en dernier parmi les handlers MONITOR grâce à l'ordre d'enregistrement,
     * ce qui garantit que {@link QuestListener} et {@link GrowthItemListener} ont déjà
     * effectué leur vérification {@code isPlaced()} avant la suppression.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Set<Long> set = placedBlocks.get(block.getWorld().getUID());
        if (set != null) {
            set.remove(pack(block.getX(), block.getY(), block.getZ()));
        }
    }

    /**
     * Déplace les positions suivies lorsqu'un piston pousse une colonne de blocs.
     * Les blocs sont traités du plus éloigné au plus proche pour éviter qu'un bloc
     * déplacé n'écrase l'entrée du bloc suivant avant que celui-ci soit traité.
     * ({@code getBlocks()} retourne les blocs du plus proche au plus éloigné.)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        if (blocks.isEmpty()) return;
        Set<Long> set = placedBlocks.get(event.getBlock().getWorld().getUID());
        if (set == null) return;

        BlockFace direction = event.getDirection();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block block = blocks.get(i);
            long oldKey = pack(block.getX(), block.getY(), block.getZ());
            if (set.remove(oldKey)) {
                Block dest = block.getRelative(direction);
                set.add(pack(dest.getX(), dest.getY(), dest.getZ()));
            }
        }
    }

    /**
     * Déplace les positions suivies lorsqu'un piston collant tire un bloc.
     * {@code getDirection()} retourne la direction de la face du piston (= direction
     * d'extension) ; les blocs tirés se déplacent dans la direction <em>opposée</em>,
     * c'est-à-dire vers le corps du piston.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();
        if (blocks.isEmpty()) return;
        Set<Long> set = placedBlocks.get(event.getBlock().getWorld().getUID());
        if (set == null) return;

        BlockFace pullDirection = event.getDirection().getOppositeFace();
        for (Block block : blocks) {
            long oldKey = pack(block.getX(), block.getY(), block.getZ());
            if (set.remove(oldKey)) {
                Block dest = block.getRelative(pullDirection);
                set.add(pack(dest.getX(), dest.getY(), dest.getZ()));
            }
        }
    }

    /** Libère les entrées en mémoire lors du déchargement d'un monde (ex : reset de la Wilderness). */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        placedBlocks.remove(event.getWorld().getUID());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Encodage de position
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Encode (x, y, z) en un {@code long} sans collision dans les limites du monde Minecraft.
     * <ul>
     *   <li>z ∈ [−30 M, 30 M] + 30 M → [0, 60 M] → 26 bits — bits 0–25</li>
     *   <li>y ∈ [−64, 320]     + 64   → [0, 384]  →  9 bits — bits 26–34</li>
     *   <li>x ∈ [−30 M, 30 M] + 30 M → [0, 60 M] → 26 bits — bits 35–60</li>
     * </ul>
     * Total : 61 bits — le bit de signe (63) n'est jamais atteint, aucune collision possible.
     */
    private static long pack(int x, int y, int z) {
        return ((long)(x + 30_000_000) << 35) | ((long)(y + 64) << 26) | (long)(z + 30_000_000);
    }
}