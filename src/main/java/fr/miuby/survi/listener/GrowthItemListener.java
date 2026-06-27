package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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

    // ─── Blocs bûches naturelles (10 types, sans stripped/wood/bamboo) ────────
    private static final Set<org.bukkit.Material> LOG_BLOCKS = EnumSet.of(
            org.bukkit.Material.OAK_LOG,
            org.bukkit.Material.SPRUCE_LOG,
            org.bukkit.Material.BIRCH_LOG,
            org.bukkit.Material.JUNGLE_LOG,
            org.bukkit.Material.ACACIA_LOG,
            org.bukkit.Material.DARK_OAK_LOG,
            org.bukkit.Material.MANGROVE_LOG,
            org.bukkit.Material.CHERRY_LOG,
            org.bukkit.Material.CRIMSON_STEM,
            org.bukkit.Material.WARPED_STEM
    );

    // ═════════════════════════════════════════════════════════════════════════
    //  Détection reload — mise à jour paresseuse des items stale
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Quand le joueur change de slot actif, vérifie si le nouvel item en main est un
     * growth item dont la config a changé depuis la dernière application.
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
        GrowthItems.checkAndReapplyHeldAndEquipped(event.getPlayer());
    }

    /**
     * Quand le joueur swap main/offhand (touche F), vérifie le staleness des deux items
     * concernés avant que le serveur n'applique réellement le swap.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        ItemStack mainHandResult = event.getMainHandItem();
        if (GrowthItems.getGrowthId(mainHandResult) != null
                && GrowthItems.checkAndReapplyIfStale(mainHandResult, player))
            event.setMainHandItem(mainHandResult);

        ItemStack offHandResult = event.getOffHandItem();
        if (GrowthItems.getGrowthId(offHandResult) != null
                && GrowthItems.checkAndReapplyIfStale(offHandResult, player))
            event.setOffHandItem(offHandResult);
    }

    /**
     * Détecte tout changement d'équipement détecté par Paper, quelle qu'en soit la cause —
     * notamment l'équipement par <b>clic droit direct</b> (sans ouvrir l'inventaire), qui ne
     * déclenche aucun {@code InventoryClickEvent} et n'était donc pas couvert jusqu'ici.
     * Complète {@code onInventoryClick} (clics en interface) et {@code onItemHeld}/
     * {@code onSwapHandItems} (main). L'event est notifié après coup, le changement est
     * déjà appliqué — pas besoin de planifier au tick suivant.
     */
    @EventHandler
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean involvesGrowth = event.getEquipmentChanges().values().stream().anyMatch(change ->
                GrowthItems.getGrowthId(change.newItem()) != null
                        || GrowthItems.getGrowthId(change.oldItem()) != null);
        if (!involvesGrowth) return;

        GrowthItems.checkAndReapplyHeldAndEquipped(player);
    }

    /**
     * Détecte tout clic dans l'inventaire du joueur impliquant un growth item ou un slot armure
     * (slots 36–40 : bottes, jambières, plastron, casque, main secondaire).
     * Planifie au tick suivant, après que Bukkit ait effectivement déplacé l'item, une
     * vérification de staleness sur tout ce que le joueur tient/porte (couvre l'équipement
     * direct, le shift-click depuis l'inventaire ou un coffre ouvert).
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Slots armure + offhand, mais uniquement si le clic porte sur le PlayerInventory lui-même
        // (un coffre double a aussi des slots 36-40, qui n'ont rien à voir avec l'armure du joueur)
        boolean isArmorOrOffhand = event.getClickedInventory() instanceof PlayerInventory
                && event.getSlot() >= 36 && event.getSlot() <= 40;
        // Ou l'item cliqué / en curseur est un growth item — couvre aussi le swap hotbar (touches 1-9)
        // depuis un coffre ouvert : getCurrentItem() renvoie alors l'item du coffre qui va atterrir
        // dans le hotbar, même si getClickedInventory() est le coffre et non le PlayerInventory.
        boolean involvesGrowth = GrowthItems.getGrowthId(event.getCurrentItem()) != null
                || GrowthItems.getGrowthId(event.getCursor()) != null;

        if (!isArmorOrOffhand && !involvesGrowth) return;

        // Tick suivant : l'item est réellement dans son nouveau slot
        org.bukkit.plugin.Plugin plugin = GameManager.getInstance().getPlugin();
        plugin.getServer().getScheduler().runTask(plugin,
                () -> GrowthItems.checkAndReapplyHeldAndEquipped(player));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Items en main — GROWTH_PICKAXE, GROWTH_SWORD, GROWTH_BATON_FERMIER
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        GrowthItems.IncrementUses(event.getPlayer(), "BlockBreakEvent", EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_CASQUE_MINEUR / GROWTH_JAMBIERES_MINEUR — minerais
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        Player player = event.getPlayer();
        GrowthItems.IncrementUses(player, "OreBreakEvent", EquipmentSlot.HEAD);
        GrowthItems.IncrementUses(player, "OreBreakEvent", EquipmentSlot.LEGS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LogBreakEvent — bûches naturelles (OAK_LOG … WARPED_STEM)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Déclenché pour les 10 types de bûches naturelles : OAK_LOG, SPRUCE_LOG, BIRCH_LOG,
     * JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG, CRIMSON_STEM, WARPED_STEM.
     * Stripped logs, wood et bamboo_block sont exclus.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent event) {
        if (!LOG_BLOCKS.contains(event.getBlock().getType())) return;
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        Player player = event.getPlayer();
        GrowthItems.IncrementUses(player, "LogBreakEvent", EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
        GrowthItems.IncrementUses(player, "LogBreakEvent", EquipmentSlot.HEAD);
        GrowthItems.IncrementUses(player, "LogBreakEvent", EquipmentSlot.CHEST);
        GrowthItems.IncrementUses(player, "LogBreakEvent", EquipmentSlot.LEGS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FishCatchEvent — pêche (lorsqu'un poisson / loot est effectivement pris)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Déclenché uniquement quand l'état est {@code CAUGHT_FISH} (hameçon ramené avec un poisson
     * ou un item de trésor/ordure). Les états FISHING, REEL_IN, BITE, etc. sont ignorés.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFishCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        GrowthItems.IncrementUses(player, "FishCatchEvent", EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
        GrowthItems.IncrementUses(player, "FishCatchEvent", EquipmentSlot.HEAD);
        GrowthItems.IncrementUses(player, "FishCatchEvent", EquipmentSlot.LEGS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  XpGainEvent — tout gain d'expérience (orbes, smelting, pêche, etc.)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Déclenché pour tout gain d'XP via {@code PlayerExpChangeEvent}.
     * Couvre les orbes physiques, le smelting, la pêche, les enchantements, etc.
     * Ignoré si {@code amount <= 0} (cas rare : plugin tiers mettant exp à 0).
     */
    @EventHandler(ignoreCancelled = true)
    public void onXpGain(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return;
        Player player = event.getPlayer();
        GrowthItems.IncrementUses(player, "XpGainEvent", EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
        GrowthItems.IncrementUses(player, "XpGainEvent", EquipmentSlot.HEAD);
        GrowthItems.IncrementUses(player, "XpGainEvent", EquipmentSlot.LEGS);
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

        GrowthItems.IncrementUses(player, "CropBreakEvent", EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
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
    //  GROWTH_HOUE_FERMIER — met les ennemis en feu au corps à corps
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) return;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;

        Integer fireSeconds = meta.getPersistentDataContainer()
                .get(GrowthItems.FIRE_SECONDS_KEY, PersistentDataType.INTEGER);
        if (fireSeconds == null || fireSeconds <= 0) return;

        target.setFireTicks(fireSeconds * 20);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROWTH_EXPLORER_COMPASS — croît à chaque nouveau biome découvert
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