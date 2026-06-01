package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.miuby.survi.world.WorldLevelManager;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {

    /**
     * Nombre maximum de quêtes qu'un joueur peut accepter par jour.
     * Un admin peut permettre des quêtes supplémentaires via /quest remove.
     */
    public static final int DAILY_QUEST_LIMIT = 2;

    @Getter
    private final List<Quest> questPool = new ArrayList<>();
    private final Random random = new Random();

    public QuestManager() {
        loadQuests();
    }

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

        // Inspecter les joueurs connectés dont une quête active n'existe plus dans le nouveau pool
        int orphanCount = 0;
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            // Ignorer les joueurs hors ligne : leur état ne change pas en temps réel
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

    private void loadQuests() {
        File questFile = new File(GameManager.getInstance().getPlugin().getDataFolder(), "quests.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
        if (!config.contains("quests")) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Impossible de charger les quêtes depuis quests.yml !");
            return;
        }

        questPool.clear();

        List<?> questsList = config.getList("quests");
        if (questsList == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST, "La liste 'quests' est absente ou vide dans quests.yml");
            return;
        }

        for (Object obj : questsList) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;

            try {
                QuestYamlLoader.BaseFields base = QuestYamlLoader.parseBase(map);
                EQuestDifficulty difficulty = EQuestDifficulty.valueOf((String) map.get("difficulty"));

                questPool.add(Quest.builder()
                        .id(base.id())
                        .name(base.name())
                        .description(base.description())
                        .type(base.type())
                        .target(base.target())
                        .goal(base.goal())
                        .rewards(base.rewards())
                        .difficulty(difficulty)
                        .build());

            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST, "Erreur lors du chargement d'une quête dans quests.yml", e);
            }
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST, questPool.size() + " quêtes chargées depuis quests.yml");
    }

    public Quest getQuest(String id) {
        return questPool.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Retourne une quête aléatoire de la difficulté donnée en évitant de répéter
     * la dernière quête attribuée au joueur (persistée en DB, résiste aux restarts).
     */
    public Quest getRandomQuest(EQuestDifficulty difficulty, UUID playerUuid) {
        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .toList();
        if (filtered.isEmpty()) return null;
        if (filtered.size() == 1) return filtered.getFirst();

        // Lire le dernier questId depuis la DB (persistant entre les restarts)
        String lastId = GameManager.getInstance().getDatabase().quests().getLastQuestId(playerUuid);

        List<Quest> candidates = filtered.stream()
                .filter(q -> !q.getId().equals(lastId))
                .toList();

        return candidates.isEmpty()
                ? filtered.get(random.nextInt(filtered.size()))
                : candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Rolls a quest difficulty whose distribution scales with the world level.
     * At world level 0 the odds reproduce the original values (70 / 25 / 5).
     * As the level rises, COMMON weight shrinks and LEGENDARY weight grows.
     *
     * @see WorldLevelManager#getQuestDifficultyWeights()
     */
    public EQuestDifficulty getRandomDifficulty() {
        int[] weights = GameManager.getInstance().getWorldLevelManager().getQuestDifficultyWeights();
        int roll = random.nextInt(100);
        if (roll < weights[2])                   return EQuestDifficulty.LEGENDARY;
        if (roll < weights[2] + weights[1])      return EQuestDifficulty.RARE;
        return EQuestDifficulty.COMMON;
    }

    /**
     * Appelé à la connexion d'un joueur : restaure ses quêtes du jour et supprime
     * les expirées (date ≠ aujourd'hui). Ré-applique ou retire les effets de
     * potion selon l'état de chaque quête.
     *
     * <p>Extrait d'AlphaPlayer pour garder toute la logique métier de quête ici.
     */
    public void restoreQuestsOnJoin(AlphaPlayer player, List<PlayerQuestData> loaded) {
        player.getActiveQuests().clear();
        java.time.LocalDate today = java.time.LocalDate.now();

        List<PlayerQuestData> expired = new ArrayList<>();
        List<PlayerQuestData> valid   = new ArrayList<>();

        for (PlayerQuestData quest : loaded) {
            if (today.isEqual(quest.getLastAccepted())) valid.add(quest);
            else                                         expired.add(quest);
        }

        // Nettoyage des quêtes expirées
        for (PlayerQuestData q : expired) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), q.getSlot());
            MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                    "Quête expirée (slot " + q.getSlot() + ") supprimée pour " + player.getPseudo());
        }

        // Retrait des effets de potion des quêtes expirées réclamées
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

        // Chargement des quêtes valides
        player.getActiveQuests().addAll(valid);

        // Ré-application des effets de potion des quêtes valides déjà réclamées
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

    /**
     * Reset admin : supprime la quête en cours et retire les effets de potion
     * si elle était déjà complétée. Libère un slot pour une nouvelle quête.
     */
    public boolean resetQuest(AlphaPlayer player) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current == null) return false;

        // Retirer les buffs si elle était déjà complétée (mais pas encore réclamée)
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

    /**
     * Attribue une nouvelle quête au joueur s'il n'a pas atteint sa limite journalière.
     */
    public void assignQuest(AlphaPlayer player, Trader trader) {
        assignQuest(player, trader, false);
    }

    /**
     * Attribue une nouvelle quête au joueur s'il n'a pas atteint sa limite journalière.
     */
    public void assignQuest(AlphaPlayer player, Trader trader, boolean force) {
        // Vérifier si le joueur a encore une quête non réclamée
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            if (current.isCompleted()) {
                player.getPlayer().sendMessage(Component.text("Vous avez déjà une quête terminée à réclamer !", NamedTextColor.RED));
            } else {
                player.getPlayer().sendMessage(Component.text("Vous avez déjà une quête en cours. Terminez-la d'abord !", NamedTextColor.RED));
            }
            return;
        }

        // Vérifier la limite journalière
        if (!force && player.countTodayQuests() >= DAILY_QUEST_LIMIT) {
            player.getPlayer().sendMessage(Component.text("Vous avez atteint votre limite de ", NamedTextColor.RED)
                    .append(Component.text(String.valueOf(DAILY_QUEST_LIMIT), NamedTextColor.GOLD))
                    .append(Component.text(" quête(s) aujourd'hui. Revenez demain !", NamedTextColor.RED)));
            return;
        }

        EQuestDifficulty difficulty = getRandomDifficulty();
        Quest quest = getRandomQuest(difficulty, player.getUuid());
        if (quest == null) return;

        // Prochain slot = nombre total de quêtes du jour
        int nextSlot = player.countTodayQuests();

        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        // Mémoriser cette quête comme dernière attribuée pour l'anti-répétition
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[AssignQuest] " + player.getPseudo() + " → " + quest.getId() + " (" + difficulty + ") slot=" + nextSlot);
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
     * Si le joueur a déjà une quête active, elle est remplacée après confirmation.
     */
    public void assignSpecificQuest(AlphaPlayer player, Quest quest) {
        // Si une quête non réclamée est déjà active, on la supprime d'abord
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            player.removeQuest(current.getSlot());
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());
        }

        int nextSlot = player.countTodayQuests();
        // traderId null en mode test : pas besoin d'un villageois pour valider
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, null, false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(Component.text("[TEST] Quête de test assignée : ", NamedTextColor.YELLOW)
                    .append(Component.text(quest.getName(), NamedTextColor.GOLD)));
            player.getPlayer().sendMessage(Component.text(quest.getDescription(), NamedTextColor.GRAY));
            player.getPlayer().sendMessage(Component.text("Objectif : " + quest.getGoal() + " | Difficulté : " + quest.getDifficulty().name(), NamedTextColor.DARK_GRAY));
        }
    }

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
        if (trader.getJob() != null && trader.getQuestCompletionReputation() > 0) {
            player.addJobReputation(trader.getJob(), trader.getQuestCompletionReputation());
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
