package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.ReputationEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

public class GlobalQuestManager extends AbstractQuestManager<GlobalQuest> {

    @Getter private GlobalQuest activeQuest = null;
    @Getter private int progress = 0;
    @Getter private final Set<UUID> participants = new HashSet<>();

    private BukkitTask timerTask = null;
    private long endTime = 0L;

    public GlobalQuestManager() {
        loadQuests();
    }

    // =========================================================================
    // Pool — source YAML
    // =========================================================================

    @Override
    protected List<GlobalQuest> fetchPool() {
        return QuestYamlLoader.loadGlobalQuests();
    }

    // =========================================================================
    // API publique
    // =========================================================================

    public boolean startQuest(String questId) {
        if (activeQuest != null) return false;

        GlobalQuest quest = getQuest(questId);
        if (quest == null) return false;

        activeQuest = quest;
        progress    = 0;
        participants.clear();
        endTime = System.currentTimeMillis() + quest.getTimeLimitSeconds() * 1000L;

        broadcastQuestStart(quest);

        timerTask = GameManager.getInstance().getScheduler().runTaskLater(
                GameManager.getInstance().getPlugin(),
                this::onTimeout,
                (long) quest.getTimeLimitSeconds() * 20L
        );

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête démarrée : " + quest.getId());
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestStarted(quest);
        return true;
    }

    public void cancelQuest() {
        if (activeQuest == null) return;
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        String name = activeQuest.getName();
        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        participants.clear();

        broadcastMessage(Component.text("⚔ Quête Globale ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("« " + name + " » annulée par un administrateur. Aucune récompense.", NamedTextColor.YELLOW)));
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestEnded();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête annulée par admin : " + name);
    }

    public void progressGlobalQuest(AlphaPlayer player, EQuestType type, Object target, int amount) {
        if (activeQuest == null) return;
        if (!activeQuest.matchesAction(type, target)) return;

        participants.add(player.getUuid());
        progress = Math.min(progress + amount, activeQuest.getGoal() + amount);

        if (progress >= activeQuest.getGoal()) {
            onComplete();
        } else {
            GameManager.getInstance().getGlobalQuestBossBarService().onProgressUpdate(activeQuest, progress);
        }
    }

    public long getRemainingSeconds() {
        if (activeQuest == null) return 0L;
        return Math.max(0L, (endTime - System.currentTimeMillis()) / 1000L);
    }

    // =========================================================================
    // Logique interne
    // =========================================================================

    private void onComplete() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        GlobalQuest quest = activeQuest;
        Set<UUID> winners = new HashSet<>(participants);

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        participants.clear();

        broadcastQuestComplete(quest, winners.size());
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestCompleted(quest);

        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        for (UUID uuid : winners) {
            AlphaPlayer ap = factory.getAlphaPlayer(uuid);
            if (ap == null) continue;

            for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                effect.applyEffect(ap);
            }

            Player p = ap.getPlayer();
            if (p != null) p.sendMessage(buildRewardMessage(quest));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête complétée : " + quest.getId() + " | participants=" + winners.size());
    }

    private void onTimeout() {
        if (activeQuest == null) return;
        String name = activeQuest.getName();

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        participants.clear();
        timerTask   = null;

        broadcastMessage(
                Component.text("⏰ Quête Globale ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("« " + name + " »", NamedTextColor.YELLOW))
                        .append(Component.text(" non complétée dans les temps ! Aucune récompense.", NamedTextColor.RED))
        );
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestEnded();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête expirée : " + name);
    }

    // =========================================================================
    // Broadcasts
    // =========================================================================

    private void broadcastQuestStart(GlobalQuest quest) {
        String timeStr = formatSeconds(quest.getTimeLimitSeconds());

        Component msg = Component.newline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("  ⚔  QUÊTE GLOBALE LANCÉE  ⚔", NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getDescription(), NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("  Objectif : ", NamedTextColor.GRAY))
                .append(Component.text(quest.getGoal() + " ", NamedTextColor.AQUA))
                .append(Component.text("| Temps : ", NamedTextColor.GRAY))
                .append(Component.text(timeStr, NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("  Récompenses :", NamedTextColor.GRAY));

        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            if (effect instanceof ReputationEffect re) {
                msg = msg.appendNewline()
                        .append(Component.text("    +" + re.getReputation() + " rép. ", NamedTextColor.GREEN))
                        .append(re.getJob().toComponent());
            }
        }

        msg = msg.appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                .appendNewline();

        Sound startSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 0.8f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            p.playSound(startSound);
        }
    }

    private void broadcastQuestComplete(GlobalQuest quest, int participantCount) {
        Component msg = Component.newline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("  ✔  QUÊTE GLOBALE COMPLÉTÉE  ✔", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + participantCount + " participant(s) récompensé(s) !", NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
                .appendNewline();

        Sound completeSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.2f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            p.playSound(completeSound);
        }
    }

    private Component buildRewardMessage(GlobalQuest quest) {
        Component msg = Component.text("[Quête Globale] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("Récompenses reçues :", NamedTextColor.GREEN));
        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            if (effect instanceof ReputationEffect re) {
                msg = msg.append(Component.text(" +" + re.getReputation(), NamedTextColor.GREEN))
                        .append(Component.text(" rép. ", NamedTextColor.GRAY))
                        .append(re.getJob().toComponent());
            }
        }
        return msg;
    }

    private void broadcastMessage(Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
    }

    // =========================================================================
    // Utilitaires
    // =========================================================================

    public static String formatSeconds(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh%02dm%02ds", h, m, s);
        if (m > 0) return String.format("%dm%02ds", m, s);
        return s + "s";
    }
}