package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.quest.quest.QuestManager;
import fr.miuby.survi.system.block.EPlantFamily;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.lib.log.MLLogManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

public class QuestListener implements Listener {

    private final PlacedBlockTracker placedBlockTracker;

    public QuestListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    public static final NamespacedKey ENCHANT_MARKER_KEY = new NamespacedKey("survi", "enchanted");

    // ─── Item de reroll de quête ─────────────────────────────────────────────────

    /**
     * Consommation de {@link fr.miuby.survi.item.ECustomItem#QUEST_REROLL}. Délègue à
     * {@link QuestManager#rerollQuest} ; si refusé (aucune quête active, déjà terminée, ou
     * limite quotidienne atteinte), l'event est annulé pour ne pas consommer l'item.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrinkQuestReroll(PlayerItemConsumeEvent event) {
        if (!QuestManager.isQuestRerollItem(event.getItem())) return;

        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player == null) return;

        boolean success = GameManager.getInstance().getQuestManager().rerollQuest(player);
        if (!success) event.setCancelled(true);
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

    /**
     * Progresse les quêtes CRAFT en comptant exactement les items produits au moment du craft.
     *
     * <p>Clic normal : {@code recipe.getResult().getAmount()} items produits (1 passe de la recette).</p>
     * <p>Shift-clic : la matrice de craft est lue AVANT consommation des ingrédients.
     * Le nombre de passes possibles est le minimum des stacks présents dans les slots non-vides
     * (chaque slot perd 1 item par passe). Le total est {@code minStack × amountPerCraft}.
     * Cas limite inventaire plein : léger surcomptage possible, acceptable.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        AlphaPlayer player = AlphaPlayer.get(p.getUniqueId());
        if (player == null) return;

        ItemStack result = event.getRecipe().getResult();
        Material material = result.getType();
        int totalCrafted = getTotalCrafted(event, result);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[CraftEvent] " + p.getName() + " — " + material + " x" + totalCrafted + (event.isShiftClick() ? " (shift)" : ""));

        GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.CRAFT, material, totalCrafted);
        GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.CRAFT, material, totalCrafted);
    }

    private static int getTotalCrafted(CraftItemEvent event, ItemStack result) {
        int amountPerCraft = result.getAmount();

        int totalCrafted;
        if (event.isShiftClick()) {
            int minStack = Integer.MAX_VALUE;
            for (ItemStack slot : event.getInventory().getMatrix()) {
                if (slot != null && !slot.getType().isAir()) minStack = Math.min(minStack, slot.getAmount());
            }
            totalCrafted = (minStack == Integer.MAX_VALUE ? 1 : minStack) * amountPerCraft;
        } else {
            totalCrafted = amountPerCraft;
        }
        return totalCrafted;
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
     * Si le bloc est {@link Ageable} et n'est pas une plante en colonne (wheat, carrots,
     * beetroots, cocoa...), il doit être à maturité (age == maximumAge) : une culture juste
     * plantée et cassée aussitôt ne compte pas.
     * Les plantes en colonne ({@link MaterialUtils#COLUMN_HARVEST_CROPS} : sugar cane, cactus)
     * sont exemptées de ce contrôle même si leur BlockData est {@link Ageable} : leur 'age' est
     * un minuteur de pousse interne (déclenche l'apparition du bloc suivant dans la colonne),
     * pas un indicateur de maturité du bloc cassé — il compte donc sans condition, comme melon,
     * pumpkin ou les champignons (non-Ageable).
     * La cible transmise est {@link EPlantFamily#questTarget} — {@code CAVE_VINES_PLANT}
     * est ainsi normalisé automatiquement sur {@code CAVE_VINES}.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();
        Material target = MaterialUtils.QUEST_CROP_TARGET.get(blockType);
        if (target == null) return;
        if (!MaterialUtils.COLUMN_HARVEST_CROPS.contains(blockType)
                && event.getBlock().getBlockData() instanceof Ageable ageable
                && ageable.getAge() < ageable.getMaximumAge()) return;
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                    "[HarvestCrop] " + event.getPlayer().getName() + " — " + target);
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.HARVEST_CROP, target, 1);
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.HARVEST_CROP, target, 1);
        }
    }

    /**
     * Progresse la quête HARVEST_CROP sur la récolte par clic droit des baies
     * (baies sucrées, baies brillantes/lianes de caverne) qui ne déclenchent pas de
     * BlockBreakEvent. Ignoré si aucun drop n'est produit (buisson immature, etc.).
     * {@code CAVE_VINES_PLANT} est normalisé sur {@code CAVE_VINES}.
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
     * Récolter le miel ou la cire d'une ruche/nid d'abeilles.
     * La récolte du miel se détecte via {@link PlayerInteractEvent} (clic droit) en vérifiant
     * que le honey_level du bloc est au maximum avant traitement de l'event — {@link Beehive#getHoneyLevel()}
     * reflète encore l'état pré-récolte à ce stade, la remise à zéro n'intervenant qu'après.
     * Target : HONEY_BOTTLE si bouteille en verre, HONEYCOMB si cisailles.
     * Pas de filtrage sur la main : Minecraft n'essaie la main secondaire que si l'interaction de la
     * main principale ne produit rien (item non pertinent) ; dès qu'une main récolte effectivement,
     * l'autre n'est jamais tentée — aucun risque de double comptage.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestBeehive(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || (block.getType() != Material.BEEHIVE && block.getType() != Material.BEE_NEST)) return;
        if (!(block.getBlockData() instanceof Beehive beehive) || beehive.getHoneyLevel() < beehive.getMaximumHoneyLevel()) return;

        ItemStack item = event.getItem();
        Material drop;
        if (item != null && item.getType() == Material.GLASS_BOTTLE) {
            drop = Material.HONEY_BOTTLE;
        } else if (item != null && item.getType() == Material.SHEARS) {
            drop = Material.HONEYCOMB;
        } else {
            return;
        }

        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                    "[HarvestBeehive] " + event.getPlayer().getName() + " — " + drop
                            + " (honeyLevel=" + beehive.getHoneyLevel() + "/" + beehive.getMaximumHoneyLevel() + ")");
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
        ItemMeta meta = event.getItem().getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(ENCHANT_MARKER_KEY, PersistentDataType.BOOLEAN))
            return;

        AlphaPlayer player = AlphaPlayer.get(event.getEnchanter().getUniqueId());

        pdc.set(ENCHANT_MARKER_KEY, PersistentDataType.BOOLEAN, true);
        event.getItem().setItemMeta(meta);

        GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.ENCHANT, event.getItem().getType(), 1);
        GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.ENCHANT, event.getItem().getType(), 1);
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
        int beforeLevels = getEnchants(before).stream().mapToInt(Integer::intValue).sum();
        int afterLevels = getEnchants(after).stream().mapToInt(Integer::intValue).sum();
        return afterLevels - beforeLevels;
    }

    // Les livres enchantés stockent leurs enchantements via EnchantmentStorageMeta, pas ItemMeta#getEnchants().
    private static Collection<Integer> getEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Collections.emptyList();
        if (meta instanceof EnchantmentStorageMeta esm) return esm.getStoredEnchants().values();
        return meta.getEnchants().values();
    }
}