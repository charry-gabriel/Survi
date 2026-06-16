package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.EQuestType;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

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
}