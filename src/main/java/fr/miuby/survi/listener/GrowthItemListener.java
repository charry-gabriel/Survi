package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GrowthItemListener implements Listener {

    private static final Random RANDOM = new Random();

    private final PlacedBlockTracker placedBlockTracker;

    public GrowthItemListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    // ─── Blocs minerais ──────────────────────────────────────────────────────
    private static final Set<org.bukkit.Material> ORE_BLOCKS = EnumSet.of(
            org.bukkit.Material.COAL_ORE,              org.bukkit.Material.DEEPSLATE_COAL_ORE,
            org.bukkit.Material.IRON_ORE,              org.bukkit.Material.DEEPSLATE_IRON_ORE,
            org.bukkit.Material.GOLD_ORE,              org.bukkit.Material.DEEPSLATE_GOLD_ORE,
            org.bukkit.Material.NETHER_GOLD_ORE,
            org.bukkit.Material.DIAMOND_ORE,           org.bukkit.Material.DEEPSLATE_DIAMOND_ORE,
            org.bukkit.Material.EMERALD_ORE,           org.bukkit.Material.DEEPSLATE_EMERALD_ORE,
            org.bukkit.Material.LAPIS_ORE,             org.bukkit.Material.DEEPSLATE_LAPIS_ORE,
            org.bukkit.Material.REDSTONE_ORE,          org.bukkit.Material.DEEPSLATE_REDSTONE_ORE,
            org.bukkit.Material.COPPER_ORE,            org.bukkit.Material.DEEPSLATE_COPPER_ORE,
            org.bukkit.Material.NETHER_QUARTZ_ORE,
            org.bukkit.Material.ANCIENT_DEBRIS
    );

    // ─── Blocs cultures ───────────────────────────────────────────────────────
    private static final Set<org.bukkit.Material> CROP_BLOCKS = EnumSet.of(
            org.bukkit.Material.WHEAT,
            org.bukkit.Material.CARROTS,
            org.bukkit.Material.POTATOES,
            org.bukkit.Material.BEETROOTS,
            org.bukkit.Material.NETHER_WART,
            org.bukkit.Material.SWEET_BERRY_BUSH,
            org.bukkit.Material.MELON,
            org.bukkit.Material.PUMPKIN,
            org.bukkit.Material.COCOA,
            org.bukkit.Material.PITCHER_CROP,
            org.bukkit.Material.TORCHFLOWER_CROP
    );

    // ═════════════════════════════════════════════════════════════════════════
    //  Items en main — GROWTH_PICKAXE, GROWTH_SWORD, GROWTH_BATON_FERMIER
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        GrowthItems.IncrementUses(event.getPlayer(), "BlockBreakEvent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_CASQUE_MINEUR — croît sur les minerais
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        GrowthItems.IncrementUsesFromHelmet(event.getPlayer(), "OreBreakEvent");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_BATON_FERMIER — croît sur les cultures + bonus drops
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CROP_BLOCKS.contains(block.getType())) return;
        if (placedBlockTracker.isPlaced(block)) return;

        Player player = event.getPlayer();
        ItemStack baton = GrowthItems.findGrowthItemInHands(player, "GROWTH_BATON_FERMIER");
        if (baton != null) {
            int tier = baton.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(GrowthItems.TIER_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
            if (tier > 0) applyBatonCropBonus(event, tier);
        }

        GrowthItems.IncrementUses(player, "CropBreakEvent");
    }

    private void applyBatonCropBonus(BlockBreakEvent event, int tier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> vanillaDrops = block.getDrops(tool);
        if (vanillaDrops.isEmpty()) return;

        double bonusMultiplier = tier * 0.5;
        List<ItemStack> bonusDrops = new ArrayList<>();
        for (ItemStack drop : vanillaDrops) {
            double totalBonus = drop.getAmount() * bonusMultiplier;
            int bonus = (int) totalBonus;
            if (totalBonus - bonus > 0 && RANDOM.nextDouble() < (totalBonus - bonus)) bonus++;
            if (bonus > 0) {
                ItemStack b = drop.clone();
                b.setAmount(bonus);
                bonusDrops.add(b);
            }
        }
        bonusDrops.forEach(d -> block.getWorld().dropItemNaturally(block.getLocation(), d));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_BOUSSOLE_AVENTURIER — croît à chaque nouveau biome découvert
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Déclenche l'incrément de la boussole quand le joueur entre dans un biome
     * qu'il n'a jamais visité avec cet item.
     *
     * <p>Optimisation : la vérification du biome (comparaison d'enum) est faite
     * en premier. Le coût réel (recherche de l'item dans les mains + lecture PDC)
     * ne s'applique que lors d'un vrai changement de biome, ce qui est rare même
     * pour un joueur actif.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Biome fromBiome = event.getFrom().getBlock().getBiome();
        Biome toBiome   = event.getTo().getBlock().getBiome();
        if (fromBiome == toBiome) return; // même biome, rien à faire

        String biomeKey = toBiome.getKey().toString();
        GrowthItems.IncrementUsesOnNewBiome(event.getPlayer(), biomeKey);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_EPEE_SHINY — croît à chaque nouveau type de mob niv. 30+ tué
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Incrémente l'épée shiny quand le joueur tue un mob de niveau ≥ 30
     * d'un type jamais tué avec cet item.
     *
     * <p>Le niveau est lu via {@link fr.miuby.survi.mob.MobLevelManager#getStoredLevel}
     * (clé PDC {@code survi:mob_level}).
     * La validation "épée en main principale" est déléguée à
     * {@link GrowthItems#IncrementUsesOnNewMobType} pour garder le listener fin.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        int mobLevel = GameManager.getInstance().getMobLevelManager().getStoredLevel(living);
        if (mobLevel < 30) return;

        GrowthItems.IncrementUsesOnNewMobType(player, event.getEntity().getType().name());
    }
}