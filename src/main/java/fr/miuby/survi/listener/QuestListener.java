package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.system.block.EPlantFamily;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.lib.log.MLLogManager;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class QuestListener implements Listener {

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
     * Si le bloc est {@link Ageable} (wheat, carrots, beetroots, cocoa...), il doit être
     * à maturité (age == maximumAge) : une culture juste plantée et cassée aussitôt ne compte pas.
     * Les blocs non-Ageable (melon, pumpkin, sugar cane, cactus, champignons...) n'ont pas
     * d'état de croissance sur le bloc posé et comptent donc sans condition.
     * La cible transmise est {@link EPlantFamily#questTarget} — {@code CAVE_VINES_PLANT}
     * est ainsi normalisé automatiquement sur {@code CAVE_VINES}.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        Material target = MaterialUtils.QUEST_CROP_TARGET.get(event.getBlock().getType());
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
        AlphaPlayer player = AlphaPlayer.get(event.getEnchanter().getUniqueId());;
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