package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.system.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.display.TutorialBookService;
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

        GameManager.getInstance().getPlugin().getServer().getScheduler()
                .runTaskLater(
                        GameManager.getInstance().getPlugin(),
                        () -> TutorialBookService.giveTutorialBookIfNew(event.getPlayer()),
                        1L
                );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[Quit] " + event.getPlayer().getName());

        // Retire les faux joueurs de la colonne info avant la déconnexion
        GameManager.getInstance().getTabDisplayManager().removeInfoColumn(event.getPlayer());

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

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST, "Reset des quêtes journalières...");

        int resetCount = 0;

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getActiveQuests().isEmpty()) continue;

            boolean isOnline = player.getPlayer() != null;

            if (isOnline) {
                for (PlayerQuestData questData : player.getActiveQuests()) {
                    if (questData.isClaimed()) {
                        Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
                        if (quest != null) {
                            for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                                if (effect instanceof PotionsEffect) effect.resetEffect(player);
                            }
                        }
                    }
                }
                player.getActiveQuests().clear();
                GameManager.getInstance().getDatabase().quests().clearAllPlayerQuests(player.getUuid());
                player.getPlayer().sendMessage(Component.text("[Quêtes] ", NamedTextColor.GOLD)
                        .append(Component.text("Vos quêtes du jour ont expiré.", NamedTextColor.YELLOW)));
            } else {
                player.getActiveQuests().clear();
            }

            resetCount++;
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST, resetCount + " joueurs réinitialisés !");
    }
}