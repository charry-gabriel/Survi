package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.display.TutorialBookService;   // ← AJOUT
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.system.time.event.DailyResetEvent;

import java.util.Objects;
import java.util.logging.Level;

public class ServerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        LogManager.getInstance().log(Level.FINE, LogManager.ETagLog.PLAYER,
                "[Join] " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");

        GameManager.getInstance().getAlphaPlayerFactory().onPlayerJoin(event.getPlayer());

        // ── Livre de tutoriel pour les nouveaux joueurs ───────────────────────
        // giveTutorialBookIfNew vérifie player.hasPlayedBefore() en interne.
        // Le scheduler décale d'un tick pour s'assurer que l'inventaire est prêt.
        GameManager.getInstance().getPlugin().getServer().getScheduler()
                .runTaskLater(
                        GameManager.getInstance().getPlugin(),
                        () -> TutorialBookService.giveTutorialBookIfNew(event.getPlayer()),
                        1L
                );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        LogManager.getInstance().log(Level.FINE, LogManager.ETagLog.PLAYER,
                "[Quit] " + event.getPlayer().getName());
        AlphaPlayer.get(event.getPlayer().getUniqueId()).resetPlayer();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon) {
            LogManager.getInstance().log(Level.FINE, LogManager.ETagLog.WORLD,
                    "[EntitySpawn] EnderDragon spawned at " + dragon.getLocation());
            try {
                Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2000);
                dragon.setHealth(2000);
            } catch (Exception exception) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD,
                        Errors.nullException + " (EnderDragon)", exception);
            }
        }
    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        GameManager.getInstance().getWorldResetManager().checkAndPerformResets();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, "Reset des quêtes journalières...");

        int resetCount = 0;

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getActiveQuests().isEmpty()) continue;

            boolean isOnline = player.getPlayer() != null;

            if (isOnline) {
                for (PlayerQuestData questData : player.getActiveQuests()) {
                    if (questData.isClaimed()) {
                        Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
                        if (quest != null) {
                            for (PotionEffect effect : quest.getRewards()) {
                                player.getPlayer().removePotionEffect(effect.getType());
                            }
                        }
                    }
                }
                player.getActiveQuests().clear();
                GameManager.getInstance().getDatabase().quests().clearAllPlayerQuests(player.getUuid());
                player.getPlayer().sendMessage("§6[Quêtes] §eVos quêtes du jour ont expiré.");
            } else {
                player.getActiveQuests().clear();
            }

            resetCount++;
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, resetCount + " joueurs réinitialisés !");
    }
}