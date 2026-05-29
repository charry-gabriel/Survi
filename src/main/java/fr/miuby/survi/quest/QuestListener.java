package fr.miuby.survi.quest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
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
        if (event.getBreeder() instanceof Player killer) {
            AlphaPlayer player = AlphaPlayer.get(killer.getUniqueId());
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
                GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.CRAFT, event.getRecipe().getResult().getType(), 1);
                GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.CRAFT, event.getRecipe().getResult().getType(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        AlphaPlayer player = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (player != null) {
            GameManager.getInstance().getQuestManager().progressQuest(player, EQuestType.SMELT, event.getItemType(), event.getItemAmount());
            GameManager.getInstance().getGlobalQuestManager().progressGlobalQuest(player, EQuestType.SMELT, event.getItemType(), event.getItemAmount());
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
