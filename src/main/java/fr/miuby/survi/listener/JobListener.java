package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
/**
 * Gère les effets passifs liés au niveau des métiers :
 *
 * <ul>
 *   <li>{@link EJob#MINEUR}     — multiplicateur de drops sur les minerais</li>
 *   <li>{@link EJob#BUCHERON}   — multiplicateur drops bûches, charbon bonus, pomme bonus,
 *                                  auto-replant, tree feller, résistance au feu</li>
 *   <li>{@link EJob#ENCHANTEUR} — plafond de la table d'enchantement et de l'enclume</li>
 * </ul>
 */
public class JobListener implements Listener {

    private static final Random RANDOM = new Random();

    // ─── Blocs minerais ──────────────────────────────────────────────────────────
    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,            Material.DEEPSLATE_GOLD_ORE,            Material.NETHER_GOLD_ORE,
            Material.DIAMOND_ORE,         Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,         Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,           Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,        Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,          Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    // ─── Blocs bois (logs + écorces) ─────────────────────────────────────────────
    private static final Set<Material> LOG_BLOCKS = EnumSet.of(
            Material.OAK_LOG,       Material.OAK_WOOD,
            Material.SPRUCE_LOG,    Material.SPRUCE_WOOD,
            Material.BIRCH_LOG,     Material.BIRCH_WOOD,
            Material.JUNGLE_LOG,    Material.JUNGLE_WOOD,
            Material.ACACIA_LOG,    Material.ACACIA_WOOD,
            Material.DARK_OAK_LOG,  Material.DARK_OAK_WOOD,
            Material.CHERRY_LOG,    Material.CHERRY_WOOD,
            Material.MANGROVE_LOG,  Material.MANGROVE_WOOD,
            Material.PALE_OAK_LOG,  Material.PALE_OAK_WOOD
    );

    // ─── Feuilles pouvant dropper des pommes ─────────────────────────────────────
    private static final Set<Material> APPLE_LEAF_BLOCKS = EnumSet.of(
            Material.OAK_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
    );

    // ─── Haches (requis pour le tree feller) ─────────────────────────────────────
    private static final Set<Material> AXE_MATERIALS = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    // ─── Blocs de sol valides pour l'auto-replant ────────────────────────────────
    private static final Set<Material> SOIL_BLOCKS = EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.FARMLAND, Material.ROOTED_DIRT,
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS
    );

    // ─── Correspondance log → sapling pour l'auto-replant ────────────────────────
    private static final Map<Material, Material> LOG_TO_SAPLING;
    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);
        m.put(Material.OAK_LOG,       Material.OAK_SAPLING);
        m.put(Material.SPRUCE_LOG,    Material.SPRUCE_SAPLING);
        m.put(Material.BIRCH_LOG,     Material.BIRCH_SAPLING);
        m.put(Material.JUNGLE_LOG,    Material.JUNGLE_SAPLING);
        m.put(Material.ACACIA_LOG,    Material.ACACIA_SAPLING);
        m.put(Material.DARK_OAK_LOG,  Material.DARK_OAK_SAPLING);
        m.put(Material.CHERRY_LOG,    Material.CHERRY_SAPLING);
        m.put(Material.MANGROVE_LOG,  Material.MANGROVE_PROPAGULE);
        m.put(Material.PALE_OAK_LOG,  Material.PALE_OAK_SAPLING);
        LOG_TO_SAPLING = Collections.unmodifiableMap(m);
    }

    // ─── Faces orthogonales pour le BFS tree feller ──────────────────────────────
    private static final BlockFace[] ORTHO_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    // ─── Tables de valeurs par niveau (index = niveau, 0 → 10) ──────────────────

    private static final double[] MULTIPLIER_TABLE = {
            0.20,  // niv. 0
            0.50,  // niv. 1
            0.80,  // niv. 2
            1.00,  // niv. 3
            1.10,  // niv. 4
            1.20,  // niv. 5
            1.30,  // niv. 6
            1.40,  // niv. 7
            1.50,  // niv. 8
            1.75,  // niv. 9
            2.00   // niv. 10
    };

    /** Chance de drop d'un charbon supplémentaire lors du cassage d'un log (niv. 3+). */
    private static final double[] CHARCOAL_CHANCE = {
            0,     // niv. 0
            0,     // niv. 1
            0,     // niv. 2
            0.15,  // niv. 3
            0.20,  // niv. 4
            0.25,  // niv. 5
            0.30,  // niv. 6
            0.35,  // niv. 7
            0.40,  // niv. 8
            0.45,  // niv. 9
            0.50   // niv. 10
    };

    /**
     * Probabilité TOTALE de drop d'une pomme en cassant des feuilles (remplace la chance vanilla ~0.5%).
     * Négatif aux bas niveaux (presque aucune pomme), neutre au niv.3, puis bonus croissant.
     */
    private static final double[] APPLE_LEAF_CHANCE = {
            0.001,  // niv. 0  — 0.1%  (quasi rien)
            0.002,  // niv. 1  — 0.2%
            0.003,  // niv. 2  — 0.3%
            0.005,  // niv. 3  — 0.5%  (vanilla)
            0.055,  // niv. 4  — 5.5%
            0.085,  // niv. 5  — 8.5%
            0.115,  // niv. 6  — 11.5%
            0.155,  // niv. 7  — 15.5%
            0.205,  // niv. 8  — 20.5%
            0.275,  // niv. 9  — 27.5%
            0.355   // niv. 10 — 35.5%
    };

    /**
     * Multiplicateur de dégâts de feu/brûlure (niv. 0-4 = effet négatif, 5+ = vanilla).
     * À partir du niv. 7, l'effet {@link #onPlayerCombust} donne la résistance au feu.
     */
    private static final double[] FIRE_DAMAGE_MULT = {
            2.00,  // niv. 0  — 2× les dégâts de feu
            1.75,  // niv. 1
            1.50,  // niv. 2
            1.25,  // niv. 3
            1.10,  // niv. 4
            1.00,  // niv. 5
            1.00,  // niv. 6
            1.00,  // niv. 7
            1.00,  // niv. 8
            1.00,  // niv. 9
            1.00   // niv. 10
    };

    // ─── Cultures récoltables (harvest via BlockBreakEvent) ──────────────────────
    private static final Set<Material> HARVEST_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.MELON, Material.PUMPKIN,
            Material.COCOA, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.TORCHFLOWER_CROP, Material.PITCHER_CROP, Material.PITCHER_PLANT
    );

    // ─── Mobs passifs qui se reproduisent (kills par un fermier) ─────────────────
    private static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.BEE, EntityType.DONKEY, EntityType.STRIDER, EntityType.AXOLOTL,
            EntityType.MOOSHROOM, EntityType.CAT, EntityType.PIG, EntityType.HORSE,
            EntityType.GOAT, EntityType.CAMEL, EntityType.FROG, EntityType.HOGLIN,
            EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.RABBIT, EntityType.WOLF,
            EntityType.SHEEP, EntityType.MULE, EntityType.OCELOT, EntityType.PANDA,
            EntityType.CHICKEN, EntityType.FOX, EntityType.SNIFFER, EntityType.ARMADILLO,
            EntityType.TURTLE, EntityType.COW
            // Note : "Nautile" non identifié comme EntityType vanilla, ignoré
    );

    /** Multiplicateur de drops FERMIER (cultures + mobs passifs). Identique à MULTIPLIER_TABLE. */
    private static double getFermierMultiplier(int level) { return MULTIPLIER_TABLE[level]; }

    // ─── Tables ENCHANTEUR ────────────────────────────────────────────────────────

    /**
     * Multiplicateur de perte de durabilité par niveau ENCHANTEUR.
     * < 1.0 = items durent plus longtemps · > 1.0 = items se cassent plus vite.
     * 0.0 = aucune perte (niv.9-10, couplé au mending XP).
     */
    private static final double[] DURABILITY_LOSS_MULT = {
            3.00,  // niv. 0 — 3× plus rapide
            2.00,  // niv. 1
            1.50,  // niv. 2
            1.25,  // niv. 3
            1.00,  // niv. 4 — vanilla
            0.75,  // niv. 5
            0.50,  // niv. 6
            0.25,  // niv. 7
            0.10,  // niv. 8
            0.00,  // niv. 9 — aucune perte + réparation XP (2 dur/xp)
            0.00   // niv. 10 — aucune perte + réparation XP (4 dur/xp)
    };

    /**
     * Coût XP maximal autorisé sur l'enclume par niveau ENCHANTEUR.
     * En dessous de la limite vanilla (40), certaines opérations sont bloquées.
     * À niv.10 : aucun cap (toujours possible, quel que soit le coût).
     */
    private static final int[] ANVIL_MAX_COST = {
            2,             // niv. 0 — très restreint
            4,             // niv. 1
            7,             // niv. 2
            11,             // niv. 3
            16,             // niv. 4
            20,             // niv. 5
            25,             // niv. 6
            30,             // niv. 7
            35,             // niv. 8
            40,             // niv. 9 — cap vanilla
            Integer.MAX_VALUE  // niv. 10 — aucun cap
    };

    // ─── Tables AVENTURIER ────────────────────────────────────────────────────────

    /** Nombre de logs SUPPLÉMENTAIRES cassés d'un coup via tree feller (niv. 5+).
     * niv.5 = +1 log (2 au total), niv.10 = +6 logs (7 au total). */
    private static final int[] TREE_FELLER_EXTRA = {
            0,  // niv. 0
            0,  // niv. 1
            0,  // niv. 2
            0,  // niv. 3
            0,  // niv. 4
            1,  // niv. 5
            2,  // niv. 6
            3,  // niv. 7
            4,  // niv. 8
            5,  // niv. 9
            6   // niv. 10
    };

    /** Durée en ticks de la résistance au feu accordée à la combustion (niv. 7+). */
    private static final int[] FIRE_RESISTANCE_TICKS = {
            0,    // niv. 0
            0,    // niv. 1
            0,    // niv. 2
            0,    // niv. 3
            0,    // niv. 4
            0,    // niv. 5
            0,    // niv. 6
            60,   // niv. 7  — 3 s
            100,  // niv. 8  — 5 s
            160,  // niv. 9  — 8 s
            240   // niv. 10 — 12 s
    };

    private static double getMultiplier(int level) {
        return MULTIPLIER_TABLE[level];
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  MINEUR / BUCHERON – drops sur les blocs
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        Material type = block.getType();

        if (ORE_BLOCKS.contains(type)) {
            try (var t = PerfTimer.start("JobListener.dropWithMultiplier")) {
                dropWithMultiplier(event, getMultiplier(alpha.getJobLevel(EJob.MINEUR)));
            }
            return;
        }

        if (LOG_BLOCKS.contains(type)) {
            int level = alpha.getJobLevel(EJob.BUCHERON);
            try (var t = PerfTimer.start("JobListener.dropWithMultiplier")) {
                dropWithMultiplier(event, getMultiplier(level));
            }
            if (level >= 3) dropBonusCharcoal(block, level);
            if (level >= 5) {
                autoReplant(block);
                if (AXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
                    try (var t = PerfTimer.start("JobListener.treeFeller")) {
                        treeFeller(block, player, level);
                    }
                }
            }
            return;
        }

        if (APPLE_LEAF_BLOCKS.contains(type)) {
            int level = alpha.getJobLevel(EJob.BUCHERON);
            handleAppleLeaves(event, level);
            return;
        }

        if (HARVEST_CROPS.contains(type)) {
            int level = alpha.getJobLevel(EJob.FERMIER);
            try (var t = PerfTimer.start("JobListener.dropWithMultiplier")) {
                dropWithMultiplier(event, getFermierMultiplier(level));
            }
        }
    }

    /**
     * Annule les drops vanilla et les remplace en appliquant le multiplicateur.
     *
     * <p>Le multiplicateur représente la quantité totale droppée par rapport à la normale :
     * 1.0 = drops identiques à vanilla, 0.5 = moitié, 2.0 = double.</p>
     *
     * <p>{@code block.getDrops(tool)} est l'appel le plus coûteux ici (calcul Bukkit +
     * enchantements). Si le PerfTimer signale cette méthode, envisager un cache des drops
     * par {@link Material} pour les enchantements communs.</p>
     */
    private static void dropWithMultiplier(BlockBreakEvent event, double multiplier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> baseDrops = block.getDrops(tool);
        event.setDropItems(false);

        for (ItemStack drop : baseDrops) {
            double totalAmount = drop.getAmount() * multiplier;
            int amount = (int) totalAmount;
            double fractional = totalAmount - amount;
            if (fractional > 0 && RANDOM.nextDouble() < fractional) amount++;
            if (amount > 0) {
                ItemStack toDrop = drop.clone();
                toDrop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), toDrop);
            }
            // amount == 0 → le joueur n'obtient rien pour cet item (possible aux bas niveaux)
        }
    }

    // ─── Bucheron : charbon bonus ─────────────────────────────────────────────────

    private static void dropBonusCharcoal(Block block, int level) {
        if (RANDOM.nextDouble() < CHARCOAL_CHANCE[level]) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHARCOAL));
        }
    }

    // ─── Bucheron : drop de pomme (courbe complète, remplace la chance vanilla) ────

    /**
     * Remplace le drop vanilla de pomme par notre propre probabilité.
     * Les autres drops de la feuille (sapling, bâton) sont conservés tels quels.
     *
     * <p>Effet négatif aux bas niveaux (moins de pommes que vanilla),
     * effet positif à partir du niv. 4.</p>
     */
    private static void handleAppleLeaves(BlockBreakEvent event, int level) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> vanillaDrops = block.getDrops(tool);
        event.setDropItems(false);

        for (ItemStack drop : vanillaDrops) {
            if (drop.getType() == Material.APPLE) continue; // on gère les pommes nous-mêmes
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        if (RANDOM.nextDouble() < APPLE_LEAF_CHANCE[level]) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
        }
    }

    // ─── Bucheron : auto-replant (base de l'arbre uniquement) ────────────────────

    /**
     * Plante un sapling de la même essence après la destruction d'un log,
     * mais seulement si le bloc du dessous est un bloc de terre — ce qui signifie
     * qu'on est à la base de l'arbre.
     */
    private static void autoReplant(Block brokenLog) {
        Material sapling = LOG_TO_SAPLING.get(brokenLog.getType());
        if (sapling == null) return;
        if (!SOIL_BLOCKS.contains(brokenLog.getRelative(BlockFace.DOWN).getType())) return;

        Location loc = brokenLog.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block target = loc.getBlock();
            if (target.getType() == Material.AIR) target.setType(sapling);
        }, 1L);
    }

    // ─── Bucheron : tree feller ───────────────────────────────────────────────────

    /**
     * Casse les logs adjacents connectés en BFS orthogonal, jusqu'au cap du niveau.
     * Chaque log supplémentaire consomme 1 point de durabilité sur la hache.
     * Les drops de chaque log sont multipliés par le multiplicateur du niveau.
     */
    private static void treeFeller(Block origin, Player player, int level) {
        int extra = TREE_FELLER_EXTRA[level];
        if (extra <= 0) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        double multiplier = getMultiplier(level);

        List<Block> toBreak = new ArrayList<>(extra);
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        outer:
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            for (BlockFace face : ORTHO_FACES) {
                Block neighbor = current.getRelative(face);
                if (!visited.add(neighbor)) continue;
                if (!LOG_BLOCKS.contains(neighbor.getType())) continue;
                toBreak.add(neighbor);
                if (toBreak.size() >= extra) break outer;
                queue.add(neighbor);
            }
        }

        for (Block logBlock : toBreak) {
            Collection<ItemStack> drops = logBlock.getDrops(tool);
            logBlock.setType(Material.AIR);
            for (ItemStack drop : drops) {
                double totalAmount = drop.getAmount() * multiplier;
                int amount = (int) totalAmount;
                if (RANDOM.nextDouble() < totalAmount - amount) amount++;
                if (amount > 0) {
                    ItemStack toDrop = drop.clone();
                    toDrop.setAmount(amount);
                    logBlock.getWorld().dropItemNaturally(logBlock.getLocation(), toDrop);
                }
            }
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  BUCHERON – résistance au feu
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Lorsqu'un bûcheron de niv. 7+ prend feu, lui accorde temporairement une
     * résistance au feu (durée croissante avec le niveau).
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.BUCHERON);
        if (level < 7 || player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS[level], 0, false, false));
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  BUCHERON – dégâts de feu amplifiés aux bas niveaux
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Aux bas niveaux (0-4), le feu inflige plus de dégâts.
     * À partir du niv. 5, les dégâts sont vanilla (×1.0).
     * À partir du niv. 7, l'événement {@link #onPlayerCombust} empêche même de prendre feu.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucheronFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) return;

        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        int level = alpha.getJobLevel(EJob.BUCHERON);
        if (level >= 5) return; // vanilla dès niv. 5

        event.setDamage(event.getDamage() * FIRE_DAMAGE_MULT[level]);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  FERMIER – drops des mobs passifs
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Applique le multiplicateur FERMIER aux drops des mobs passifs tués par un joueur.
     * Effet négatif aux bas niveaux (0-2), positif à partir du niv. 4.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPassiveMobDeath(EntityDeathEvent event) {
        if (!PASSIVE_MOBS.contains(event.getEntityType())) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        AlphaPlayer alpha = AlphaPlayer.get(killer.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.FERMIER);
        applyDropMultiplier(event.getDrops(), getFermierMultiplier(level));
    }

    /**
     * Modifie en place la liste de drops d'un EntityDeathEvent en appliquant le multiplicateur.
     * Les items réduits à 0 sont retirés de la liste.
     */
    private static void applyDropMultiplier(List<ItemStack> drops, double multiplier) {
        if (multiplier == 1.0) return;
        drops.removeIf(item -> {
            if (item == null || item.getType().isAir()) return true;
            double totalAmount = item.getAmount() * multiplier;
            int amount = (int) totalAmount;
            if (RANDOM.nextDouble() < totalAmount - amount) amount++;
            if (amount <= 0) return true;
            item.setAmount(Math.min(amount, item.getMaxStackSize()));
            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ENCHANTEUR – table d'enchantement
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Filtre les offres de la table d'enchantement selon le niveau du métier.
     * Une offre est supprimée (null) si :
     *   - Le joueur est niv.0 (aucun enchantement autorisé)
     *   - Le coût XP de l'offre dépasse {@code niveau × 3}
     *   - Le niveau de l'enchantement proposé dépasse le niveau du métier
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        Player player = event.getEnchanter();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        int jobLevel  = alpha.getJobLevel(EJob.ENCHANTEUR);
        int maxXpCost = jobLevel * 3;

        EnchantmentOffer[] offers = event.getOffers();
        for (int i = 0; i < offers.length; i++) {
            if (offers[i] == null) continue;
            boolean costTooHigh  = offers[i].getCost() > maxXpCost;
            boolean levelTooHigh = offers[i].getEnchantmentLevel() > jobLevel;
            if (jobLevel == 0 || costTooHigh || levelTooHigh) {
                offers[i] = null;
            }
        }
    }

    /**
     * Vérification de sécurité au moment du clic : annule l'enchantement si le
     * joueur n'a pas le niveau requis (double protection avec PrepareItemEnchantEvent).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getEnchanter().getUniqueId());
        if (alpha == null) return;

        int jobLevel = alpha.getJobLevel(EJob.ENCHANTEUR);

        if (jobLevel == 0) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(
                    Component.text("✗ Vous ne pouvez pas encore enchanter. Progressez dans le métier ")
                            .color(NamedTextColor.RED)
                            .append(EJob.ENCHANTEUR.toComponent())
                            .append(Component.text(".", NamedTextColor.RED)));
            return;
        }

        boolean tooHigh = false;
        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            if (entry.getValue() > jobLevel) {
                tooHigh = true;
                break;
            }
        }

        if (tooHigh) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(
                    Component.text("✗ Cet enchantement dépasse votre niveau de métier ")
                            .color(NamedTextColor.RED)
                            .append(EJob.ENCHANTEUR.toComponent())
                            .append(Component.text(" (max niv." + jobLevel + " d'enchantement).", NamedTextColor.RED)));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ENCHANTEUR – enclume (cap XP + bypass "Too Expensive")
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Gère les opérations d'enclume selon le niveau ENCHANTEUR :
     *
     * <ul>
     *   <li>Bloque si un enchantement dépasse le niveau du métier.</li>
     *   <li>Bloque si le coût XP dépasse {@code ANVIL_MAX_COST[niveau]}.</li>
     *   <li>Niv.10 : bypass complet du cap "Too Expensive" — reconstruit le résultat
     *       si vanilla le refuserait, en fusionnant les enchantements.</li>
     *   <li>Sur tout résultat valide : réinitialise le RepairCost à 0 pour que l'item
     *       ne s'accumule jamais vers "Too Expensive" pour de futures opérations.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        int jobLevel  = alpha.getJobLevel(EJob.ENCHANTEUR);
        AnvilInventory anvil = event.getInventory();
        ItemStack first  = anvil.getItem(0);
        ItemStack second = anvil.getItem(1);

        // ── Cas 1 : résultat nul (vanilla a refusé l'opération) ─────────────────
        if (event.getResult() == null || event.getResult().getType() == Material.AIR) {
            // Niv.10 uniquement : tenter de reconstruire le résultat
            if (jobLevel >= 10 && first != null && !first.getType().isAir()) {
                ItemStack rebuilt = constructAnvilResult(first, second, anvil.getRenameText(), jobLevel);
                if (rebuilt != null) {
                    event.setResult(rebuilt);
                    anvil.setRepairCost(39); // juste sous le cap vanilla — opération coûteuse
                }
            }
            return;
        }

        // ── Cas 2 : résultat présent — appliquer les restrictions du niveau ──────
        if (first == null || first.getType() == Material.AIR) return;

        Map<Enchantment, Integer> firstEnchants  = getEnchants(first);
        Map<Enchantment, Integer> resultEnchants = getEnchants(event.getResult());

        boolean enchantmentChanged = false;
        boolean tooHighLevel       = false;

        for (Map.Entry<Enchantment, Integer> entry : resultEnchants.entrySet()) {
            int previousLevel = firstEnchants.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > previousLevel) {
                enchantmentChanged = true;
                if (jobLevel == 0 || entry.getValue() > jobLevel) {
                    tooHighLevel = true;
                    break;
                }
            }
        }

        // Enchantement trop haut pour ce niveau → bloquer
        if (enchantmentChanged && tooHighLevel) {
            event.setResult(null);
            return;
        }

        // Coût XP dépasse le cap du niveau → bloquer
        int cost = anvil.getRepairCost();
        if (cost > ANVIL_MAX_COST[jobLevel]) {
            event.setResult(null);
            return;
        }

        // ── Résultat valide : réinitialiser le RepairCost pour éviter l'accumulation
        ItemStack finalResult = event.getResult().clone();
        if (finalResult.getItemMeta() instanceof Repairable r) {
            r.setRepairCost(0);
            finalResult.setItemMeta(r);
            event.setResult(finalResult);
        }
    }

    /**
     * Reconstruit un résultat d'enclume manuellement (bypass "Too Expensive" pour niv.10).
     * Fusionne les enchantements de {@code addition} dans {@code base} et réinitialise
     * le RepairCost à 0.
     */
    private static ItemStack constructAnvilResult(ItemStack base, ItemStack addition,
                                                  String rename, int jobLevel) {
        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return null;

        // Renommage
        if (rename != null && !rename.isBlank()) {
            meta.displayName(Component.text(rename, NamedTextColor.WHITE));
        }

        // Fusion des enchantements depuis l'addition (livre ou même matériau)
        if (addition != null && !addition.getType().isAir()) {
            Map<Enchantment, Integer> addEnchants = getEnchants(addition);
            for (Map.Entry<Enchantment, Integer> entry : addEnchants.entrySet()) {
                Enchantment ench = entry.getKey();
                int addLvl = entry.getValue();
                if (addLvl > jobLevel) continue; // respect du niveau métier
                int existLvl = meta.getEnchantLevel(ench);
                int newLvl   = (existLvl == addLvl) ? existLvl + 1 : Math.max(existLvl, addLvl);
                if (ench.canEnchantItem(result)) meta.addEnchant(ench, newLvl, true);
            }

            // Réparation partielle si même matériau
            if (addition.getType() == base.getType() && meta instanceof Damageable d && d.getDamage() > 0) {
                d.setDamage(Math.max(0, d.getDamage() - base.getType().getMaxDurability() / 2));
            }
        }

        // Réinitialiser le RepairCost → jamais "Too Expensive" pour les futures opérations
        if (meta instanceof Repairable r) r.setRepairCost(0);

        result.setItemMeta(meta);
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ENCHANTEUR – durabilité accélérée / réduite
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Modifie la perte de durabilité selon le niveau ENCHANTEUR.
     * Aux bas niveaux (0-3) les items se cassent plus vite, aux hauts niveaux (5-8)
     * ils durent beaucoup plus longtemps. Niv.9-10 : aucune perte de durabilité.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.ENCHANTEUR);
        double mult = DURABILITY_LOSS_MULT[level];

        if (mult <= 0) { event.setCancelled(true); return; }
        if (mult == 1.0) return;

        double total = event.getDamage() * mult;
        int damage = (int) total;
        if (RANDOM.nextDouble() < total - damage) damage++;
        if (damage <= 0) { event.setCancelled(true); return; }
        event.setDamage(damage);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ENCHANTEUR – réparation XP (mending-like, niv.9-10)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * À partir du niv.9, chaque XP gagné répare l'item le plus endommagé du joueur
     * (main principale, secondaire, armure), en consommant l'XP proportionnellement.
     * Niv.9 : 2 points de durabilité par XP · Niv.10 : 4 points par XP.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchanteurExpGain(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return;
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.ENCHANTEUR);
        if (level < 9) return;

        int repairPerXP = (level == 10) ? 4 : 2;
        ItemStack target = findMostDamagedItem(event.getPlayer());
        if (target == null) return;
        if (!(target.getItemMeta() instanceof Damageable dmg) || dmg.getDamage() == 0) return;

        int currentDmg  = dmg.getDamage();
        int xpAvailable = event.getAmount();
        int xpNeeded    = (int) Math.ceil((double) currentDmg / repairPerXP);
        int xpUsed      = Math.min(xpAvailable, xpNeeded);
        int repaired    = xpUsed * repairPerXP;

        dmg.setDamage(Math.max(0, currentDmg - repaired));
        target.setItemMeta(dmg);
        event.setAmount(Math.max(0, xpAvailable - xpUsed));
    }

    /** Retourne l'item le plus endommagé parmi la main principale, la main secondaire et l'armure. */
    private static ItemStack findMostDamagedItem(Player player) {
        ItemStack best = null;
        int maxDmg = 0;
        ItemStack[] candidates = {
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand(),
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        };
        for (ItemStack item : candidates) {
            if (item == null || item.getType().isAir()) continue;
            if (!(item.getItemMeta() instanceof Damageable d)) continue;
            if (d.getDamage() > maxDmg) { maxDmg = d.getDamage(); best = item; }
        }
        return best;
    }

    // ─── Utilitaire : enchantements d'un ItemStack ───────────────────────────────

    /**
     * Retourne les enchantements d'un ItemStack.
     * Pour un livre enchanté, retourne les enchantements stockés.
     */
    private static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        if (item == null) return Map.of();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Map.of();
        if (meta instanceof EnchantmentStorageMeta esm) return esm.getStoredEnchants();
        return meta.getEnchants();
    }
}