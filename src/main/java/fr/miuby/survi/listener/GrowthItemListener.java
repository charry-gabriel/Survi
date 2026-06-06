package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
    //  Détection reload — mise à jour paresseuse des items stale
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Quand le joueur change de slot actif, vérifie si le nouvel item en main est un
     * growth item dont la config a changé depuis la dernière application.
     *
     * <p>Couvre le cas nominal : item au palier max sans use (ne peut plus progresser
     * naturellement) et items récupérés d'un coffre après un reload.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (GrowthItems.getGrowthId(newItem) == null) return;
        try {
            if (GrowthItems.checkAndReapplyIfStale(newItem, player))
                player.getInventory().setItem(event.getNewSlot(), newItem);
        } catch (AlphaPlayerNotFoundException ignored) {}
    }

    /**
     * À la connexion, vérifie tous les emplacements tenus ou équipés (main, offhand, armure).
     * Couvre le cas d'un joueur qui se reconnecte après un reload effectué pendant sa déconnexion.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        try {
            // Main (slot actif du hotbar)
            ItemStack mainHand = inv.getItemInMainHand();
            if (GrowthItems.getGrowthId(mainHand) != null
                    && GrowthItems.checkAndReapplyIfStale(mainHand, player))
                inv.setItemInMainHand(mainHand);

            // Offhand
            ItemStack offHand = inv.getItemInOffHand();
            if (GrowthItems.getGrowthId(offHand) != null
                    && GrowthItems.checkAndReapplyIfStale(offHand, player))
                inv.setItemInOffHand(offHand);

            // Armure (getArmorContents retourne un tableau de copies)
            ItemStack[] armor = inv.getArmorContents();
            boolean anyUpdated = false;
            for (ItemStack piece : armor) {
                if (GrowthItems.getGrowthId(piece) != null
                        && GrowthItems.checkAndReapplyIfStale(piece, player))
                    anyUpdated = true;
            }
            if (anyUpdated) inv.setArmorContents(armor);

        } catch (AlphaPlayerNotFoundException ignored) {}
    }

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Biome fromBiome = event.getFrom().getBlock().getBiome();
        Biome toBiome   = event.getTo().getBlock().getBiome();
        if (fromBiome == toBiome) return;

        String biomeKey = toBiome.getKey().toString();
        GrowthItems.IncrementUsesOnNewBiome(event.getPlayer(), biomeKey);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_EPEE_SHINY — croît à chaque nouveau type de mob niv. 30+ tué
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        int mobLevel = GameManager.getInstance().getMobLevelManager().getStoredLevel(living);
        if (mobLevel < 30) return;

        GrowthItems.IncrementUsesOnNewMobType(player, event.getEntity().getType().name());
    }
}