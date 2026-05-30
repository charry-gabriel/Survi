package fr.miuby.survi.listener;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Gère les effets passifs liés au niveau des métiers :
 *
 * <ul>
 *   <li>{@link EJob#MINEUR}     — multiplicateur de drops sur les minerais</li>
 *   <li>{@link EJob#BUCHERON}   — multiplicateur de drops sur les bûches</li>
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

    // ─── Table de multiplicateurs par niveau (0 → 10) ────────────────────────────
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

    private static double getMultiplier(int level) {
        return MULTIPLIER_TABLE[level];
    }

    /**
     * Annule les drops vanilla et les remplace en appliquant le multiplicateur.
     *
     * <p>Le multiplicateur représente la quantité totale droppée par rapport à la normale :
     * 1.0 = drops identiques à vanilla, 0.5 = moitié, 2.0 = double.</p>
     *
     * <p>Exemple : 3 items de base, multiplicateur 0.5 →
     * total = 3 × 0.5 = 1.5 → 1 item garanti + 50 % de chance d'un 2e.</p>
     */
    private static void dropWithMultiplier(BlockBreakEvent event, double multiplier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        // Récupère les drops vanilla (tient compte de la pioche et de ses enchantements)
        Collection<ItemStack> baseDrops = block.getDrops(tool);

        // Supprime les drops vanilla ; on gère nous-mêmes la quantité
        event.setDropItems(false);

        for (ItemStack drop : baseDrops) {
            double totalAmount = drop.getAmount() * multiplier;
            int amount = (int) totalAmount;
            double fractional = totalAmount - amount;
            if (fractional > 0 && RANDOM.nextDouble() < fractional) {
                amount++;
            }
            if (amount > 0) {
                ItemStack toDrop = drop.clone();
                toDrop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), toDrop);
            }
            // amount == 0 → le joueur n'obtient rien pour cet item (possible aux bas niveaux)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  MINEUR – drops sur les minerais
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        Material type = block.getType();

        if (ORE_BLOCKS.contains(type)) {
            int level = alpha.getJobLevel(EJob.MINEUR);
            dropWithMultiplier(event, getMultiplier(level));

        } else if (LOG_BLOCKS.contains(type)) {
            int level = alpha.getJobLevel(EJob.BUCHERON);
            dropWithMultiplier(event, getMultiplier(level));
        }
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

        int jobLevel = alpha.getJobLevel(EJob.ENCHANTEUR);
        int maxXpCost = jobLevel * 3; // niv.1 → 3, niv.5 → 15, niv.10 → 30

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
                            .append(Component.text(".", NamedTextColor.RED))
            );
            return;
        }

        // Annule si l'un des enchantements dépasse le niveau métier
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
                            .append(Component.text(" (max niv." + jobLevel + " d'enchantement).", NamedTextColor.RED))
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ENCHANTEUR – enclume
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Bloque la préparation d'une opération d'enclume si elle ajoute ou monte
     * un enchantement au-delà du niveau métier de l'ENCHANTEUR.
     *
     * <p>Les simples renommages (aucun enchantement ajouté/monté) sont toujours
     * autorisés.</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        int jobLevel = alpha.getJobLevel(EJob.ENCHANTEUR);

        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) return;

        ItemStack firstItem = event.getInventory().getFirstItem();
        if (firstItem == null || firstItem.getType() == Material.AIR) return;

        // Enchantements actuels sur le premier item (avant opération)
        Map<Enchantment, Integer> firstEnchants = getEnchants(firstItem);
        // Enchantements sur le résultat (après opération)
        Map<Enchantment, Integer> resultEnchants = getEnchants(result);

        // Vérifie si l'opération ajoute ou monte un enchantement
        boolean enchantmentChanged = false;
        boolean tooHighLevel = false;

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

        // Si aucun enchantement n'est modifié (simple renommage), on laisse passer
        if (!enchantmentChanged) return;

        if (tooHighLevel) {
            event.setResult(null);
            // Message discret : PrepareAnvilEvent se déclenche souvent lors de la saisie,
            // on évite donc de spammer le joueur ici. L'absence de résultat est suffisante.
        }
    }

    // ─── Utilitaire : récupère les enchantements d'un ItemStack ──────────────────

    /**
     * Retourne les enchantements d'un ItemStack.
     * Pour un livre enchanté, retourne les enchantements stockés.
     * Pour tout autre item, retourne les enchantements appliqués.
     */
    private static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        if (item == null) return Map.of();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Map.of();
        if (meta instanceof EnchantmentStorageMeta esm) {
            return esm.getStoredEnchants();
        }
        return meta.getEnchants();
    }
}
