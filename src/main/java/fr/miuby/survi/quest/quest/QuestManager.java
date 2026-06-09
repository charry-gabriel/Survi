package fr.miuby.survi.quest.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.AbstractQuestManager;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.quest.QuestYamlLoader;
import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class QuestManager extends AbstractQuestManager<Quest> {

    /**
     * Quêtes journalières supplémentaires débloquées chaque jour de jeu depuis {@code StartVillageZoneEffect}.
     * La capacité totale d'un joueur = {@link #DAILY_QUEST_BONUS} × {@link fr.miuby.survi.world.VillageZoneManager#getGameDayCount()}.
     */
    public static final int DAILY_QUEST_BONUS = 2;

    /**
     * Capacité totale de quêtes journalières disponibles pour tous les joueurs au moment de l'appel.
     * Vaut 0 si la partie n'a pas encore démarré (avant le premier levelup de villageois).
     */
    public int getTotalCapacity() {
        return GameManager.getInstance().getVillageZoneManager().getGameDayCount() * DAILY_QUEST_BONUS;
    }

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
     * <h3>Comportement sur les quêtes en cours (joueurs connectés)</h3>
     *
     * <p>Le nouveau pool est d'abord chargé en mémoire temporaire avant de remplacer l'actuel,
     * de sorte que les définitions des quêtes orphelines sont encore accessibles pour
     * appliquer les récompenses éventuelles.
     *
     * <ul>
     *   <li><b>Quête dont l'id existe encore</b> — le joueur continue normalement.</li>
     *   <li><b>Quête terminée (completed) mais non réclamée, id supprimé</b> — les récompenses sont
     *       accordées immédiatement (blessing effects + réputation métier si le trader est encore
     *       disponible). La quête est marquée comme réclamée en DB.</li>
     *   <li><b>Quête en cours (non terminée), id supprimé</b> — la quête est supprimée et le slot
     *       est libéré : le joueur peut en accepter une nouvelle dès sa prochaine interaction avec
     *       un Trader. Un message d'information lui est envoyé.</li>
     * </ul>
     *
     * @return le nombre de quêtes présentes dans le pool après rechargement
     */
    public int reload() {
        // 1. Charger le nouveau pool sans encore remplacer l'actuel
        //    (le pool courant reste lisible pour traiter les quêtes orphelines)
        List<Quest> incoming = QuestYamlLoader.loadQuests();
        Set<String> incomingIds = incoming.stream().map(Quest::getId).collect(Collectors.toSet());

        // 2. Traiter les joueurs connectés qui ont une quête orpheline
        int rewardedCount = 0;
        int droppedCount  = 0;
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getPlayer() == null) continue;

            for (PlayerQuestData data : new ArrayList<>(player.getActiveQuests())) {
                if (incomingIds.contains(data.getQuestId())) continue; // quête encore valide — rien à faire

                Quest oldQuest = getQuest(data.getQuestId()); // pool encore intact à ce stade

                if (data.isCompleted() && !data.isClaimed() && oldQuest != null) {
                    // Le joueur avait terminé la quête avant le reload → récompense immédiate
                    applyOrphanRewards(player, data, oldQuest);
                    rewardedCount++;
                } else if (!data.isCompleted()) {
                    // Quête en cours supprimée → libérer le slot
                    player.removeQuest(data.getSlot());
                    GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
                    GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
                    player.getPlayer().sendMessage(Component.text(
                            "Votre quête en cours a été supprimée suite à une mise à jour. Vous pouvez en accepter une nouvelle auprès d'un Trader.",
                            NamedTextColor.YELLOW));
                    droppedCount++;
                }
                // Cas data.isCompleted() && data.isClaimed() : déjà réclamée → rien à faire
            }
        }

        // 3. Remplacer le pool
        questPool.clear();
        questPool.addAll(incoming);

        if (rewardedCount > 0) MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] " + rewardedCount + " quête(s) terminée(s) récompensée(s) automatiquement (id supprimé du nouveau fichier).");
        if (droppedCount > 0) MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] " + droppedCount + " quête(s) en cours supprimée(s) — slots libérés.");

        return questPool.size();
    }

    /**
     * Applique les récompenses d'une quête orpheline directement, sans passer par un Trader.
     * Utilisé dans {@link #reload()} pour les quêtes terminées dont l'id a été supprimé du YAML.
     */
    private void applyOrphanRewards(AlphaPlayer player, PlayerQuestData data, Quest quest) {
        // Blessing effects (potions, réputation de blessing, etc.)
        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            effect.applyEffect(player);
        }

        // Réputation de métier — récupère le job du trader d'origine s'il est encore en jeu
        if (data.getTraderId() != null) {
            MLVillager villager = VillagerRegistry.get(data.getTraderId());
            if (villager instanceof Trader trader && trader.getJob() != null
                    && SurviConfig.getInstance().getQuestCompletionReputation() > 0) {
                player.addJobReputation(trader.getJob(), SurviConfig.getInstance().getQuestCompletionReputation());
            }
        }

        // Supprime de player_quest (même logique que claimQuest) et incrémente le compteur
        player.removeQuest(data.getSlot());
        GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
        player.setTotalDailyQuestsClaimed(player.getTotalDailyQuestsClaimed() + 1);

        // Historique persistant (marque la quête comme réclamée dans l'historique)
        GameManager.getInstance().getDatabase().questHistory().insert(
                player.getUuid(),
                player.getPseudo(),
                quest.getId(),
                LocalDate.now(),
                quest.getDifficulty(),
                null,
                "daily",
                0
        );

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(Component.text(
                    "Votre quête « " + quest.getName() + " » a été récompensée automatiquement suite à une mise à jour.",
                    NamedTextColor.GREEN));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] Récompenses accordées à " + player.getPseudo() + " pour la quête orpheline : " + quest.getId());
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
     * Appelé à la connexion d'un joueur : restaure ses quêtes actives (non réclamées) depuis la DB.
     *
     * <p>Les quêtes réclamées encore présentes dans {@code player_quest} (données résiduelles d'une
     * session précédente) sont supprimées de la table — elles sont déjà enregistrées dans
     * {@code quest_history} et leur comptage est reflété dans {@link AlphaPlayer#getTotalDailyQuestsClaimed()}.
     * Seules les quêtes non réclamées (en cours ou terminées-non-validées) sont conservées en mémoire
     * et dans la DB.</p>
     *
     * <p>Active le glow du Trader cible si le joueur a une quête terminée non réclamée.</p>
     */
    public void restoreQuestsOnJoin(AlphaPlayer player, List<PlayerQuestData> loaded) {
        player.getActiveQuests().clear();

        List<PlayerQuestData> claimed   = new ArrayList<>();
        List<PlayerQuestData> unclaimed = new ArrayList<>();

        for (PlayerQuestData q : loaded) {
            if (q.isClaimed()) claimed.add(q);
            else               unclaimed.add(q);
        }

        // Supprime les quêtes réclamées résiduelles de player_quest (déjà dans quest_history).
        for (PlayerQuestData q : claimed) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), q.getSlot());
            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                    "Quête réclamée résiduelle (slot " + q.getSlot() + ") nettoyée pour " + player.getPseudo());
        }

        player.getActiveQuests().addAll(unclaimed);

        // Active le glow du Trader cible si le joueur a une quête terminée non réclamée.
        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) {
            for (PlayerQuestData q : unclaimed) {
                if (q.isCompleted() && q.getTraderId() != null) {
                    glowService.enableGlow(player, q.getTraderId());
                    break; // Au plus une quête active non réclamée à la fois
                }
            }
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

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(player);
        GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(Component.text("Votre quête a été réinitialisée par un administrateur. Vous pouvez en accepter une nouvelle !", NamedTextColor.YELLOW));
        }
        return true;
    }

    public void assignQuest(AlphaPlayer player, Trader trader) {
        assignQuest(player, trader, false);
    }

    /**
     * Attribue une nouvelle quête au joueur si la capacité cumulative n'est pas atteinte.
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

        int capacity  = getTotalCapacity();
        int usedSlots = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();

        if (capacity == 0) {
            player.getPlayer().sendMessage(Component.text("Aucune quête n'est encore disponible. Attendez le début de la partie !", NamedTextColor.RED));
            return;
        }

        if (!force && usedSlots >= capacity) {
            player.getPlayer().sendMessage(Component.text("Vous avez complété ", NamedTextColor.RED)
                    .append(Component.text(usedSlots + "/" + capacity, NamedTextColor.GOLD))
                    .append(Component.text(" quêtes. Revenez demain pour de nouveaux créneaux !", NamedTextColor.RED)));
            return;
        }

        int difficulty = computeDifficulty(player, trader);
        Quest quest = getRandomQuest(difficulty, trader.getJob(), player.getUuid());
        if (quest == null) return;

        int nextSlot = usedSlots; // slot global unique, croissant
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[AssignQuest] " + player.getPseudo() + " → " + quest.getId() + " (diff=" + difficulty + ") slot=" + nextSlot + " [" + (usedSlots + 1) + "/" + capacity + "]");
        player.getPlayer().sendMessage(Component.text("Nouvelle quête acceptée auprès de ", NamedTextColor.GREEN)
                .append(Component.text(trader.getNameId(), NamedTextColor.AQUA))
                .append(Component.text(" : ", NamedTextColor.GREEN))
                .append(Component.text(quest.getName(), NamedTextColor.GOLD)));
        player.getPlayer().sendMessage(Component.text(quest.getFormattedDescription(), NamedTextColor.GRAY));
        player.getPlayer().sendMessage(Component.text("Quête " + (usedSlots + 1) + "/" + capacity + " disponibles.", NamedTextColor.DARK_GRAY));
    }

    /**
     * Attribue une quête spécifique au joueur (mode test admin).
     */
    public void assignSpecificQuest(AlphaPlayer player, Quest quest) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            player.removeQuest(current.getSlot());
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService != null) glowService.disableGlow(player);
            GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
        }

        int nextSlot = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, null, false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        if (player.getPlayer() != null) {
            String jobsStr = quest.getJobs().isEmpty() ? "tous" : quest.getJobs().stream().map(EJob::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("tous");
            player.getPlayer().sendMessage(Component.text("[TEST] Quête de test assignée : ", NamedTextColor.YELLOW).append(Component.text(quest.getName(), NamedTextColor.GOLD)));
            player.getPlayer().sendMessage(Component.text(quest.getFormattedDescription(), NamedTextColor.GRAY));
            player.getPlayer().sendMessage(Component.text("Objectif : " + quest.getGoal() + " | Difficulté : " + quest.getDifficulty() + " | Métiers : " + jobsStr, NamedTextColor.DARK_GRAY));
        }
    }

    // =========================================================================
    // Progression
    // =========================================================================

    /**
     * Réclame la récompense de la quête complétée auprès du bon Trader.
     * La quête est supprimée de {@code player_quest} après réclamation (elle est déjà dans {@code quest_history})
     * et le compteur en mémoire {@link AlphaPlayer#getTotalDailyQuestsClaimed()} est incrémenté.
     */
    public boolean claimQuest(AlphaPlayer player, Trader trader, boolean force) {
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

        int capacity  = getTotalCapacity();
        int usedAfter = player.getTotalDailyQuestsClaimed() + 1; // +1 = celle qu'on vient de réclamér

        player.getPlayer().sendMessage(Component.text("Vous avez reçu les récompenses de la quête ! ", NamedTextColor.GREEN)
                .append(Component.text(usedAfter + "/" + capacity, NamedTextColor.GOLD))
                .append(Component.text(" quêtes complétées.", NamedTextColor.GREEN)));

        // Supprime de player_quest (déjà sauvée dans quest_history) et met à jour le compteur en mémoire
        player.removeQuest(data.getSlot());
        GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
        player.setTotalDailyQuestsClaimed(usedAfter);

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(player);

        // Historique persistant
        GameManager.getInstance().getDatabase().questHistory().insert(
                player.getUuid(),
                player.getPseudo(),
                quest.getId(),
                LocalDate.now(),
                quest.getDifficulty(),
                trader.getJob() != null ? trader.getJob().name() : null,
                "daily",
                0
        );

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
            finishQuest(player, quest);
        } else {
            GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
            GameManager.getInstance().getQuestActionBarService().showProgress(player, quest, data);
        }
    }

    private void finishQuest(AlphaPlayer player, Quest quest) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null) return;

        data.setProgress(quest.getGoal());
        data.setCompleted(true);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[QuestFinished] " + player.getPseudo() + " — " + quest.getId());
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);

        SoundService.play(player.getPlayer(), ESound.QUEST_COMPLETE);
        player.getPlayer().sendMessage(Component.text("Quête terminée : ", NamedTextColor.GREEN).append(Component.text(quest.getName(), NamedTextColor.GOLD)));
        player.getPlayer().sendMessage(Component.text("Allez voir le Trader pour obtenir votre récompense !", NamedTextColor.GRAY));
        GameManager.getInstance().getQuestActionBarService().showFinished(player, quest);

        // Active le glow du Trader cible pour guider le joueur
        if (data.getTraderId() != null) {
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService != null) glowService.enableGlow(player, data.getTraderId());
        }
    }
}