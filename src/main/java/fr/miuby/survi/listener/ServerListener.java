package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.Quest;
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

import java.util.List;
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
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "Reset journalier — annulation des effets des quêtes réclamées. Capacité cumulée : " + capacity + ".");

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {

            List<PlayerQuestData> claimedQuests = player.getActiveQuests().stream()
                    .filter(PlayerQuestData::isClaimed)
                    .toList();

            if (claimedQuests.isEmpty()) {
                // Aucune quête réclamée — notifier quand même si de nouveaux créneaux s'ouvrent
                if (player.getPlayer() != null && capacity > 0) notifyNewSlots(player, capacity);
                continue;
            }

            boolean isOnline = player.getPlayer() != null;

            // 1. Annuler les effets de potion des quêtes réclamées
            if (isOnline) {
                for (PlayerQuestData data : claimedQuests) {
                    Quest quest = GameManager.getInstance().getQuestManager().getQuest(data.getQuestId());
                    if (quest == null) continue;
                    for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effect.resetEffect(player);
                    }
                }
            }

            // 2. Supprimer les quêtes réclamées de player_quest et de la mémoire
            for (PlayerQuestData data : claimedQuests) {
                player.removeQuest(data.getSlot());
                GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
            }

            // 3. Arrêter le refresh d'actionbar uniquement si plus aucune quête active
            if (player.getActiveQuests().stream().noneMatch(q -> !q.isClaimed())) {
                GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
                if (glowService != null && isOnline) glowService.disableGlow(player);
            }

            // 4. Notifier le joueur des nouveaux créneaux
            if (isOnline) notifyNewSlots(player, capacity);
        }
    }

    private void notifyNewSlots(AlphaPlayer player, int capacity) {
        int used      = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();
        int remaining = capacity - used;
        if (remaining <= 0) return;
        var ls = GameManager.getInstance().getLangService();
        player.getPlayer().sendMessage(
                ls.text(player.getPlayer(), "quest.new_slots.prefix")
                        .append(ls.text(player.getPlayer(), "quest.new_slots.body", remaining, used, capacity)));
    }
}