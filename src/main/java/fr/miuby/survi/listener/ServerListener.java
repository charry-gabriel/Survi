package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.quest.QuestManager;
import org.bukkit.potion.PotionEffect;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.system.time.event.DailyResetEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.Objects;
import java.util.logging.Level;

public class ServerListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        GameManager.getInstance().getAlphaPlayerFactory().onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        AlphaPlayer.get(event.getPlayer().getUniqueId()).resetPlayer();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        if (event.getEntity() instanceof EnderDragon dragon) {
            try {
                Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2000);
                dragon.setHealth(2000);
            } catch (Exception exception) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, Errors.nullException + " (" + exception.getMessage() + ")");
            }
        }
    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, "Reset des quêtes journalières...");

        int resetCount = 0;

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getActiveQuests().isEmpty()) continue;

            boolean isOnline = player.getPlayer() != null;

            if (isOnline) {
                // Retirer les buffs de toutes les quêtes réclamées du jour
                for (PlayerQuestData questData : player.getActiveQuests()) {
                    if (questData.isClaimed()) {
                        Quest quest = QuestManager.getInstance().getQuest(questData.getQuestId());
                        if (quest != null) {
                            for (PotionEffect effect : quest.getRewards()) {
                                player.getPlayer().removePotionEffect(effect.getType());
                            }
                        }
                    }
                }
                // Effacer en mémoire et en DB
                player.getActiveQuests().clear();
                GameManager.getInstance().getDatabase().quests().clearAllPlayerQuests(player.getUuid());
                player.getPlayer().sendMessage("§6[Quêtes] §eVos quêtes du jour ont expiré.");
            } else {
                // Joueur déconnecté : on vide uniquement la mémoire.
                // Les entrées restent en DB avec leur date d'hier, ainsi
                // cleanupExpiredQuestsOnJoin() retirera les buffs à la reconnexion.
                player.getActiveQuests().clear();
            }

            resetCount++;
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, resetCount + " joueurs réinitialisés !");
    }
}