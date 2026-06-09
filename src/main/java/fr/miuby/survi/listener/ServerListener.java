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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        AlphaPlayer.get(event.getPlayer().getUniqueId()).resetPlayer();
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

        int capacity = GameManager.getInstance().getQuestManager().getTotalCapacity();
        if (capacity == 0) return; // La partie n'a pas encore démarré — pas de notification

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "Nouveau jour de jeu — capacité quêtes portée à " + capacity + ".");

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getPlayer() == null) continue;
            int used = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();
            int remaining = capacity - used;
            if (remaining <= 0) continue;
            player.getPlayer().sendMessage(Component.text("[Quêtes] ", NamedTextColor.GOLD)
                    .append(Component.text(remaining + " nouveau(x) créneau(x) disponible(s) ! ", NamedTextColor.GREEN))
                    .append(Component.text("(" + used + "/" + capacity + " utilisés)", NamedTextColor.DARK_GRAY)));
        }
    }
}