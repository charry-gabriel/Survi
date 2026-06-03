package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.EQuestType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
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
        if (event.getWhoClicked() instanceof Player killer) {
            AlphaPlayer player = AlphaPlayer.get(killer.getUniqueId());
            if (player != null) {
                int amount = event.getRecipe().getResult().getAmount();
                GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.CRAFT, event.getRecipe().getResult().getType(), amount);
                GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.CRAFT, event.getRecipe().getResult().getType(), amount);
            }
        }
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
}