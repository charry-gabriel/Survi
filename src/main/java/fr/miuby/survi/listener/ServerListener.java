package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
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

        // Pour chaque joueur
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            PlayerQuestData quest = player.getActiveQuest();

            // Si la quête n'est pas terminée ou pas claimed, on la reset
            if (quest != null && (!quest.isCompleted() || !quest.isClaimed())) {
                player.setActiveQuest(null);
                GameManager.getInstance().getDatabase().quests().clearPlayerQuest(player.getUuid());
                resetCount++;

                // Notifie le joueur s'il est connecté
                if (player.getPlayer() != null) {
                    player.getPlayer().sendMessage("§6[Quêtes] §eVotre quête du jour a expiré.");
                }
            }
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, resetCount + " quêtes réinitialisées !");
    }
}
