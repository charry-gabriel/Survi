package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.EQuestType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.Map;

public class QuestListener implements Listener {

    /**
     * Mapping bloc culture → cible YAML pour les quêtes HARVEST_CROP.
     * La cible est le {@link Material} du bloc lui-même (pas du drop) car c'est ce que
     * l'auteur YAML renseigne dans {@code targets}. CAVE_VINES_PLANT est normalisé sur
     * CAVE_VINES pour que la config n'ait qu'un seul identifiant à gérer.
     */
    private static final Map<Material, Material> CROP_BLOCK_TO_TARGET = new EnumMap<>(Material.class);
    static {
        CROP_BLOCK_TO_TARGET.put(Material.WHEAT,            Material.WHEAT);
        CROP_BLOCK_TO_TARGET.put(Material.CARROTS,          Material.CARROTS);
        CROP_BLOCK_TO_TARGET.put(Material.POTATOES,         Material.POTATOES);
        CROP_BLOCK_TO_TARGET.put(Material.BEETROOTS,        Material.BEETROOTS);
        CROP_BLOCK_TO_TARGET.put(Material.NETHER_WART,      Material.NETHER_WART);
        CROP_BLOCK_TO_TARGET.put(Material.MELON,            Material.MELON);
        CROP_BLOCK_TO_TARGET.put(Material.PUMPKIN,          Material.PUMPKIN);
        CROP_BLOCK_TO_TARGET.put(Material.COCOA,            Material.COCOA);
        CROP_BLOCK_TO_TARGET.put(Material.SUGAR_CANE,       Material.SUGAR_CANE);
        CROP_BLOCK_TO_TARGET.put(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRY_BUSH);
        CROP_BLOCK_TO_TARGET.put(Material.CAVE_VINES,       Material.CAVE_VINES);
        CROP_BLOCK_TO_TARGET.put(Material.CAVE_VINES_PLANT, Material.CAVE_VINES);
        CROP_BLOCK_TO_TARGET.put(Material.TORCHFLOWER_CROP, Material.TORCHFLOWER_CROP);
        CROP_BLOCK_TO_TARGET.put(Material.PITCHER_CROP,     Material.PITCHER_CROP);
        CROP_BLOCK_TO_TARGET.put(Material.CACTUS,           Material.CACTUS);
        CROP_BLOCK_TO_TARGET.put(Material.RED_MUSHROOM,     Material.RED_MUSHROOM);
        CROP_BLOCK_TO_TARGET.put(Material.BROWN_MUSHROOM,   Material.BROWN_MUSHROOM);
    }

    private final PlacedBlockTracker placedBlockTracker;

    public QuestListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    /**
     * Progresse la quête MINE uniquement si le bloc est d'origine naturelle.
     * Un bloc posé par un joueur — qu'il ait été simplement replacé, poussé par piston,
     * ou les deux — est ignoré et ne compte pas pour les quêtes ni les global quests.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.MINE, event.getBlock().getType(), 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.MINE, event.getBlock().getType(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            AlphaPlayer player = AlphaPlayer.get(event.getEntity().getKiller().getUniqueId());
            if (player != null) {
                GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.KILL, event.getEntity().getType(), 1);
                GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.KILL, event.getEntity().getType(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player p) {
            AlphaPlayer player = AlphaPlayer.get(p.getUniqueId());
            if (player != null) {
                GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.BREED, event.getEntityType(), 1);
                GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.BREED, event.getEntityType(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
            if (player != null) {
                GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.FISH, null, 1);
                GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.FISH, null, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        AlphaPlayer player = AlphaPlayer.get(p.getUniqueId());
        if (player == null) return;

        Material result = event.getRecipe().getResult().getType();
        int amount = event.getRecipe().getResult().getAmount();

        // Global quests : comportement event-based conservé (progression partagée)
        GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.CRAFT, result, amount);

        // Daily quests : l'item n'est pas encore dans l'inventaire au moment de l'event — délai 1 tick
        GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
            if (player.getPlayer() == null || !player.getPlayer().isOnline()) return;
            GameManager.getInstance().getQuestManager().syncCraftProgress(player, result);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            int amount = event.getItemAmount();
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.SMELT, event.getItemType(), amount);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.SMELT, event.getItemType(), amount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.SHEAR, event.getEntity().getType(), 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.SHEAR, event.getEntity().getType(), 1);
        }
    }

    /** Apprivoiser un animal (cheval, loup, chat, lama, etc.). Target : EntityType de l'animal. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player p)) return;
        AlphaPlayer player = AlphaPlayer.get(p.getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.TAME, event.getEntityType(), 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.TAME, event.getEntityType(), 1);
        }
    }

    /**
     * Progresse la quête HARVEST_CROP sur la casse d'un bloc culture.
     * Ne vérifie PAS le PlacedBlockTracker : les cultures plantées par les joueurs
     * doivent compter (contrairement aux quêtes MINE où les blocs posés sont exclus).
     * Si le bloc est {@link Ageable} (wheat, carrots, beetroots, cocoa...), il doit être
     * à maturité (age == maximumAge) : une culture juste plantée et cassée aussitôt ne compte pas.
     * Les blocs non-Ageable (melon, pumpkin, sugar cane, cactus, champignons...) n'ont pas
     * d'état de croissance sur le bloc posé et comptent donc sans condition.
     * La cible transmise à progressQuest est le Material du bloc (pas du drop) ;
     * CAVE_VINES_PLANT est normalisé sur CAVE_VINES.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        Material target = CROP_BLOCK_TO_TARGET.get(event.getBlock().getType());
        if (target == null) return;
        if (event.getBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) return;
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.HARVEST_CROP, target, 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.HARVEST_CROP, target, 1);
        }
    }

    /**
     * Progresse la quête HARVEST_CROP sur la récolte par clic droit des baies
     * (baies sucrées, baies brillantes/lianes de caverne) qui ne déclenchent pas de
     * BlockBreakEvent. Ignoré si aucun drop n'est produit (buisson immature, etc.).
     * CAVE_VINES_PLANT est normalisé sur CAVE_VINES.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCropRightClick(PlayerHarvestBlockEvent event) {
        Material blockType = event.getHarvestedBlock().getType();
        if (blockType != Material.SWEET_BERRY_BUSH && blockType != Material.CAVE_VINES && blockType != Material.CAVE_VINES_PLANT) return;
        if (event.getItemsHarvested().isEmpty()) return;
        Material target = (blockType == Material.CAVE_VINES_PLANT) ? Material.CAVE_VINES : blockType;
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.HARVEST_CROP, target, 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.HARVEST_CROP, target, 1);
        }
    }

    /**
     * Récolter une ruche ou un nid d'abeilles.
     * Target : Material du drop (HONEY_BOTTLE si bouteille, HONEYCOMB si cisailles).
     * L'event se déclenche uniquement si le bloc est BEEHIVE ou BEE_NEST.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestBeehive(PlayerHarvestBlockEvent event) {
        Block block = event.getHarvestedBlock();
        if (block.getType() != Material.BEEHIVE && block.getType() != Material.BEE_NEST) return;
        if (event.getItemsHarvested().isEmpty()) return;
        Material drop = event.getItemsHarvested().getFirst().getType();
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.HARVEST_BEEHIVE, drop, 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.HARVEST_BEEHIVE, drop, 1);
        }
    }

    /**
     * Enchanter un item à la table d'enchantement.
     * Target : Material de l'item enchanté.
     * Tourne en MONITOR pour ne pas progresser si l'EnchanterListener a annulé l'action (job trop bas).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        AlphaPlayer player = AlphaPlayer.get(event.getEnchanter().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.ENCHANT, event.getItem().getType(), 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.ENCHANT, event.getItem().getType(), 1);
        }
    }

    /**
     * Gagner des niveaux d'XP.
     * S'incrémente du nombre de niveaux effectivement gagnés (newLevel - oldLevel).
     * Ignorer les pertes de niveau (mort, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelGain(PlayerLevelChangeEvent event) {
        int gained = event.getNewLevel() - event.getOldLevel();
        if (gained <= 0) return;
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.GAIN_XP_LEVELS, null, gained);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.GAIN_XP_LEVELS, null, gained);
        }
    }

    /**
     * Ajouter un enchantement (type inédit, pas juste un niveau supérieur) sur un item via l'enclume.
     * Target : Material de l'item résultant. Amount : nombre de nouveaux types d'enchantement ajoutés.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilEnchant(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory anvil) || event.getSlot() != 2) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;

        ItemStack before = anvil.getItem(0);
        ItemStack after = event.getCurrentItem();
        if (before == null || before.getType().isAir() || after == null || after.getType().isAir()) return;

        int added = countNewEnchants(before, after);
        if (added <= 0) return;

        AlphaPlayer player = AlphaPlayer.get(p.getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.ANVIL_ENCHANT, after.getType(), added);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.ANVIL_ENCHANT, after.getType(), added);
        }
    }

    private static int countNewEnchants(ItemStack before, ItemStack after) {
        Map<Enchantment, Integer> beforeEnchants = getEnchants(before);
        int added = 0;
        for (Enchantment e : getEnchants(after).keySet()) {
            if (!beforeEnchants.containsKey(e)) added++;
        }
        return added;
    }

    // Les livres enchantés stockent leurs enchantements via EnchantmentStorageMeta, pas ItemMeta#getEnchants().
    private static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Map.of();
        if (meta instanceof EnchantmentStorageMeta esm) return esm.getStoredEnchants();
        return meta.getEnchants();
    }
}