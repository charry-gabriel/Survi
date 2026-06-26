package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.QuestGlowService;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.time.event.DailyResetEvent;

import java.util.Objects;
import java.util.logging.Level;

public class ServerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[Join] " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");

        GameManager.getInstance().getAlphaPlayerFactory().onPlayerJoin(event.getPlayer());
        GameManager.getInstance().getGlobalQuestBossBarService().showToPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[Quit] " + event.getPlayer().getName());

        // Retire les faux joueurs de la colonne info avant la déconnexion
        GameManager.getInstance().getTabDisplayManager().removeInfoColumn(event.getPlayer());

        // Nettoie l'entrée glow sans envoyer de paquet (joueur déconnecté)
        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.cleanupOnQuit(event.getPlayer().getUniqueId());

        // Arrête le rafraîchissement de l'actionbar si une quête journalière était en cours
        GameManager.getInstance().getQuestActionBarService().stopRefresh(event.getPlayer().getUniqueId());

        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        alphaPlayer.getAlphaLife().saveHealthOnQuit();
        alphaPlayer.resetPlayer();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon) {
            try {
                var attr = Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH));
                attr.setBaseValue(1024);
                attr.addModifier(new AttributeModifier(
                        new NamespacedKey("survi", "dragon_hp_bonus"),
                        1024,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.ANY
                ));
                dragon.setHealth(attr.getValue());
            } catch (Exception exception) {
                MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD,
                        Errors.nullException + " (EnderDragon)", exception);
            }
        }
    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        GameManager.getInstance().getWorldResetManager().checkAndPerformResets();
        GameManager.getInstance().getQuestManager().performDailyQuestReset();
        GameManager.getInstance().getFoodOfTheDayManager().performDailyDraw();
    }
}