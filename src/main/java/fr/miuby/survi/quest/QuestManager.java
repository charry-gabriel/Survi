package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class QuestManager extends AbstractQuestManager<Quest> {

    /**
     * Nombre maximum de quêtes qu'un joueur peut accepter par jour.
     * Un admin peut permettre des quêtes supplémentaires via /quest remove.
     */
    public static final int DAILY_QUEST_LIMIT = 2;

    private final Random random = new Random();

    public QuestManager() {
        loadQuests();
    }

    // =========================================================================
    // Pool — source YAML
    // =========================================================================

    @Override
    protected List<Quest> fetchPool() {
        return QuestYamlLoader.loadQuests();
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge le pool de quêtes depuis {@code quests.yml} à chaud, sans redémarrage.
     *
     * <h3>Comportement sur les quêtes en cours</h3>
     * <ul>
     *   <li><b>Quête modifiée (même id, champs changés)</b> — la nouvelle définition
     *       s'applique immédiatement : si la progression actuelle du joueur dépasse
     *       le nouveau goal, la prochaine action déclenchante complètera la quête.</li>
     *   <li><b>Quête supprimée (id absent du nouveau fichier)</b> — la quête orpheline
     *       ne peut plus progresser (aucun match possible), reste en DB et expire
     *       naturellement à minuit. Un avertissement est loggé par joueur concerné.</li>
     *   <li><b>Quête réclamée (claimed)</b> — les effets de potion déjà appliqués
     *       persistent jusqu'à leur expiration naturelle ; aucune action requise.</li>
     * </ul>
     *
     * @return le nombre de quêtes présentes dans le pool après rechargement
     */
    public int reload() {
        loadQuests();

        int orphanCount = 0;
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getPlayer() == null) continue;

            for (PlayerQuestData data : player.getActiveQuests()) {
                if (!data.isClaimed() && getQuest(data.getQuestId()) == null) {
                    orphanCount++;
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                            "[Reload] " + player.getPseudo() + " a une quête active orpheline : "
                                    + data.getQuestId() + " (progression " + data.getProgress()
                                    + " conservée en DB, expire à minuit)");
                }
            }
        }

        if (orphanCount > 0) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                    "[Reload] " + orphanCount + " quête(s) active(s) orpheline(s) détectée(s). "
                            + "Elles ne progresseront plus et seront nettoyées à minuit.");
        }

        return questPool.size();
    }

    // =========================================================================
    // Sélection
    // =========================================================================

    /**
     * Calcule la difficulté applicable pour un joueur auprès d'un trader donné.
     * <ul>
     *   <li>Difficulté 0 si le trader a un métier ET que le joueur y a 0 réputation
     *       (premier contact) — on lui donne une quête introductive.</li>
     *   <li>Niveau du monde sinon.</li>
     * </ul>
     */
    public int computeDifficulty(AlphaPlayer player, Trader trader) {
        return (trader.getJob() != null && player.getJobReputation(trader.getJob()) == 0)
                ? 0
                : GameManager.getInstance().getWorldLevelManager().getLevel();
    }

    /**
     * Indique si ce trader a au moins une quête disponible dans le pool
     * pour le métier et la difficulté donnés.
     * Si {@code job} est null, toutes les quêtes de la bonne difficulté sont éligibles.
     */
    public boolean hasAvailableQuestFor(EJob job, int difficulty) {
        return questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .anyMatch(q -> job == null || q.getJobs().contains(job));
    }

    /**
     * Retourne une quête aléatoire du niveau de difficulté donné en évitant de répéter
     * la dernière quête attribuée au joueur (persistée en DB, résiste aux restarts).
     */
    public Quest getRandomQuest(int difficulty, UUID playerUuid) {
        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .toList();
        if (filtered.isEmpty()) return null;
        if (filtered.size() == 1) return filtered.getFirst();

        String lastId = GameManager.getInstance().getDatabase().quests().getLastQuestId(playerUuid);
        List<Quest> candidates = filtered.stream()
                .filter(q -> !q.getId().equals(lastId))
                .toList();

        return candidates.isEmpty()
                ? filtered.get(random.nextInt(filtered.size()))
                : candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Variante avec filtre métier. Si aucune quête ne correspond au métier,
     * retourne null (pas de fallback sur toutes les difficultés).
     *
     * @param job métier pour filtrer (null = pas de filtre métier)
     */
    public Quest getRandomQuest(int difficulty, EJob job, UUID playerUuid) {
        if (job == null) return getRandomQuest(difficulty, playerUuid);

        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .filter(q -> q.getJobs().contains(job))
                .toList();

        if (filtered.isEmpty()) return null;
        if (filtered.size() == 1) return filtered.getFirst();

        String lastId = GameManager.getInstance().getDatabase().quests().getLastQuestId(playerUuid);
        List<Quest> candidates = filtered.stream()
                .filter(q -> !q.getId().equals(lastId))
                .toList();

        return candidates.isEmpty()
                ? filtered.get(random.nextInt(filtered.size()))
                : candidates.get(random.nextInt(candidates.size()));
    }

    // =========================================================================
    // État joueur
    // =========================================================================

    /**
     * Appelé à la connexion d'un joueur : restaure ses quêtes du jour et supprime
     * les expirées (date ≠ aujourd'hui). Ré-applique ou retire les effets de
     * potion selon l'état de chaque quête.
     */
    public void restoreQuestsOnJoin(AlphaPlayer player, List<PlayerQuestData> loaded) {
        player.getActiveQuests().clear();
        LocalDate today = LocalDate.now();

        List<PlayerQuestData> expired = new ArrayList<>();
        List<PlayerQuestData> valid   = new ArrayList<>();

        for (PlayerQuestData quest : loaded) {
            if (today.isEqual(quest.getLastAccepted())) valid.add(quest);
            else                                         expired.add(quest);
        }

        for (PlayerQuestData q : expired) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), q.getSlot());
            MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                    "Quête expirée (slot " + q.getSlot() + ") supprimée pour " + player.getPseudo());
        }

        List<BlessingEffect> effectsToRemove = new ArrayList<>();
        for (PlayerQuestData q : expired) {
            if (q.isClaimed()) {
                Quest questDef = getQuest(q.getQuestId());
                if (questDef != null) {
                    for (BlessingEffect effect : questDef.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effectsToRemove.add(effect);
                    }
                }
            }
        }
        if (!effectsToRemove.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                if (!player.getPlayer().isOnline()) return;
                for (BlessingEffect effect : effectsToRemove) effect.resetEffect(player);
            }, 2L);
        }

        player.getActiveQuests().addAll(valid);

        List<BlessingEffect> effectsToReapply = new ArrayList<>();
        for (PlayerQuestData q : valid) {
            if (q.isClaimed()) {
                Quest questDef = getQuest(q.getQuestId());
                if (questDef != null) {
                    for (BlessingEffect effect : questDef.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effectsToReapply.add(effect);
                    }
                }
            }
        }
        if (!effectsToReapply.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                if (!player.getPlayer().isOnline()) return;
                for (BlessingEffect effect : effectsToReapply) effect.applyEffect(player);
            }, 5L);
        }
    }

    // =========================================================================
    // Attribution
    // =========================================================================

    /**
     * Reset admin : supprime la quête en cours et retire les effets de potion
     * si elle était déjà complétée. Libère un slot pour une nouvelle quête.
     */
    public boolean resetQuest(AlphaPlayer player) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current == null) return false;

        if (current.isCompleted() && player.getPlayer() != null) {
            Quest quest = getQuest(current.getQuestId());
            if (quest != null) {
                for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                    if (effect instanceof PotionsEffect) effect.resetEffect(player);
                }
            }
        }

        player.removeQuest(current.getSlot());
        GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(Component.text("Votre quête a été réinitialisée par un administrateur. Vous pouvez en accepter une nouvelle !", NamedTextColor.YELLOW));
        }
        return true;
    }

    public void assignQuest(AlphaPlayer player, Trader trader) {
        assignQuest(player, trader, false);
    }

    /**
     * Attribue une nouvelle quête au joueur s'il n'a pas atteint sa limite journalière.
     * Si le Trader a un métier défini, seules les quêtes compatibles sont proposées.
     */
    public void assignQuest(AlphaPlayer player, Trader trader, boolean force) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            if (current.isCompleted()) {
                player.getPlayer().sendMessage(Component.text("Vous avez déjà une quête terminée à réclamer !", NamedTextColor.RED));
            } else {
                player.getPlayer().sendMessage(Component.text("Vous avez déjà une quête en cours. Terminez-la d'abord !", NamedTextColor.RED));
            }
            return;
        }

        if (!force && player.countTodayQuests() >= DAILY_QUEST_LIMIT) {
            player.getPlayer().sendMessage(Component.text("Vous avez atteint votre limite de ", NamedTextColor.RED)
                    .append(Component.text(String.valueOf(DAILY_QUEST_LIMIT), NamedTextColor.GOLD))
                    .append(Component.text(" quête(s) aujourd'hui. Revenez demain !", NamedTextColor.RED)));
            return;
        }

        int difficulty = computeDifficulty(player, trader);
        Quest quest = getRandomQuest(difficulty, trader.getJob(), player.getUuid());
        if (quest == null) return;

        int nextSlot = player.countTodayQuests();
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[AssignQuest] " + player.getPseudo() + " → " + quest.getId() + " (diff=" + difficulty + ") slot=" + nextSlot);
        player.getPlayer().sendMessage(Component.text("Nouvelle quête acceptée auprès de ", NamedTextColor.GREEN)
                .append(Component.text(trader.getNameId(), NamedTextColor.AQUA))
                .append(Component.text(" : ", NamedTextColor.GREEN))
                .append(Component.text(quest.getName(), NamedTextColor.GOLD)));
        player.getPlayer().sendMessage(Component.text(quest.getDescription(), NamedTextColor.GRAY));
        player.getPlayer().sendMessage(Component.text("Quête " + (nextSlot + 1) + "/" + DAILY_QUEST_LIMIT + " du jour.", NamedTextColor.DARK_GRAY));
    }

    /**
     * Attribue une quête spécifique au joueur (mode test admin).
     * Ignore la limite journalière et la sélection aléatoire.
     */
    public void assignSpecificQuest(AlphaPlayer player, Quest quest) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            player.removeQuest(current.getSlot());
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());
        }

        int nextSlot = player.countTodayQuests();
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, null, false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        if (player.getPlayer() != null) {
            String jobsStr = quest.getJobs().isEmpty() ? "tous" : quest.getJobs().stream().map(EJob::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("tous");
            player.getPlayer().sendMessage(Component.text("[TEST] Quête de test assignée : ", NamedTextColor.YELLOW)
                    .append(Component.text(quest.getName(), NamedTextColor.GOLD)));
            player.getPlayer().sendMessage(Component.text(quest.getDescription(), NamedTextColor.GRAY));
            player.getPlayer().sendMessage(Component.text("Objectif : " + quest.getGoal() + " | Difficulté : " + quest.getDifficulty() + " | Métiers : " + jobsStr, NamedTextColor.DARK_GRAY));
        }
    }

    // =========================================================================
    // Progression
    // =========================================================================

    /**
     * Réclame la récompense de la quête complétée auprès du bon Trader.
     */
    public boolean completeQuest(AlphaPlayer player, Trader trader, boolean force) {
        PlayerQuestData data = player.getCurrentActiveQuest();

        if (data == null || (!force && !data.isCompleted()) || data.isClaimed())
            return false;

        if (data.getTraderId() != null && !data.getTraderId().equals(trader.getNameId())) {
            player.getPlayer().sendMessage(Component.text("Cette quête doit être validée auprès de ", NamedTextColor.RED)
                    .append(Component.text(data.getTraderId(), NamedTextColor.AQUA)));
            return false;
        }

        Quest quest = getQuest(data.getQuestId());
        if (quest == null) return false;

        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            effect.applyEffect(player);
        }
        if (trader.getJob() != null && SurviConfig.getInstance().getQuestCompletionReputation() > 0) {
            player.addJobReputation(trader.getJob(), SurviConfig.getInstance().getQuestCompletionReputation());
        }
        player.getPlayer().sendMessage(Component.text("Vous avez reçu les récompenses de la quête ! ", NamedTextColor.GREEN));
        data.setClaimed(true);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        return true;
    }

    /**
     * Incrémente la progression de la quête active en cours (non complétée).
     */
    public void progressQuest(AlphaPlayer player, EQuestType type, Object target, int amount) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null || data.isCompleted()) return;

        Quest quest = getQuest(data.getQuestId());
        if (quest == null || !quest.matchesAction(type, target)) return;

        data.setProgress(data.getProgress() + amount);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[QuestProgress] " + player.getPseudo() + " — " + data.getQuestId() + " : " + data.getProgress() + "/" + quest.getGoal());

        if (data.getProgress() >= quest.getGoal()) {
            completeQuestInternal(player, quest);
        } else {
            GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        }
    }

    private void completeQuestInternal(AlphaPlayer player, Quest quest) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null) return;

        data.setProgress(quest.getGoal());
        data.setCompleted(true);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[QuestComplete] " + player.getPseudo() + " — " + quest.getId());
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);

        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f);
        player.getPlayer().playSound(myCustomSound);
        player.getPlayer().sendMessage(Component.text("Quête terminée : ", NamedTextColor.GREEN)
                .append(Component.text(quest.getName(), NamedTextColor.GOLD)));
        player.getPlayer().sendMessage(Component.text("Allez voir le Trader pour obtenir votre récompense !", NamedTextColor.GRAY));
    }
}