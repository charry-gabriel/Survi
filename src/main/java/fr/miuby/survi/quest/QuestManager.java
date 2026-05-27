package fr.miuby.survi.quest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.villager.trader.Trader;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    private void loadQuests() {
        File questFile = new File(GameManager.getInstance().getPlugin().getDataFolder(), "quests.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
        if (!config.contains("quests")) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Impossible de charger les quêtes depuis quests.yml !");
            return;
        }

        questPool.clear();

        List<?> questsList = config.getList("quests");
        if (questsList == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.QUEST, "La liste 'quests' est absente ou vide dans quests.yml");
            return;
        }

        for (Object obj : questsList) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;

            try {
                String id = (String) map.get("id");
                String name = (String) map.get("name");
                String description = (String) map.get("description");
                QuestType type = QuestType.valueOf((String) map.get("type"));
                QuestDifficulty difficulty = QuestDifficulty.valueOf((String) map.get("difficulty"));

                int goal = ((Number) map.get("goal")).intValue();
                int reputationReward = ((Number) map.get("reputation_reward")).intValue();

                Object targetObj = null;
                String targetStr = (String) map.get("target");
                if (targetStr != null) {
                    if (type == QuestType.MINE || type == QuestType.CRAFT || type == QuestType.SMELT) {
                        targetObj = Material.valueOf(targetStr);
                    } else if (type == QuestType.KILL || type == QuestType.SHEAR || type == QuestType.BREED) {
                        targetObj = EntityType.valueOf(targetStr);
                    }
                }

                List<PotionEffect> rewards = new ArrayList<>();
                List<?> rewardsList = (List<?>) map.get("rewards");
                if (rewardsList != null) {
                    for (Object rewardObj : rewardsList) {
                        if (rewardObj instanceof Map<?, ?> rawReward) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> rewardMap = (Map<String, Object>) rawReward;
                            String potionType = ((String) rewardMap.get("type")).toLowerCase();
                            NamespacedKey key = NamespacedKey.minecraft(potionType);
                            PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(key);

                            int duration = ((Number) rewardMap.get("duration")).intValue();
                            int amplifier = ((Number) rewardMap.get("amplifier")).intValue();
                            if (effectType != null) {
                                rewards.add(new PotionEffect(effectType, duration, amplifier));
                            }
                        }
                    }
                }

                questPool.add(Quest.builder()
                        .id(id)
                        .name(name)
                        .description(description)
                        .type(type)
                        .difficulty(difficulty)
                        .target(targetObj)
                        .goal(goal)
                        .reputationReward(reputationReward)
                        .rewards(rewards)
                        .build());

            } catch (Exception e) {
                LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.QUEST, "Erreur lors du chargement d'une quête dans quests.yml : " + e.getMessage());
            }
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, questPool.size() + " quêtes chargées depuis quests.yml");
    }

    public Quest getQuest(String id) {
        return questPool.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Retourne une quête aléatoire de la difficulté donnée en évitant de répéter
     * la dernière quête attribuée au joueur (persistée en DB, résiste aux restarts).
     */
    public Quest getRandomQuest(QuestDifficulty difficulty, UUID playerUuid) {
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
    public QuestDifficulty getRandomDifficulty() {
        int[] weights = GameManager.getInstance().getWorldLevelManager().getQuestDifficultyWeights();
        // weights = { commonWeight, rareWeight, legendaryWeight }, sum = 100
        int roll = random.nextInt(100);
        if (roll < weights[2])                   return QuestDifficulty.LEGENDARY;
        if (roll < weights[2] + weights[1])      return QuestDifficulty.RARE;
        return QuestDifficulty.COMMON;
    }

    /**
     * Reset admin : supprime la quête en cours (non encore réclamée) du joueur,
     * ce qui lui libère un slot pour en accepter une nouvelle.
     * Retourne false s'il n'y a rien à reset.
     */
    public boolean resetQuest(AlphaPlayer player) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current == null) return false;

        // Retirer les buffs si elle était déjà complétée (mais pas encore réclamée)
        if (current.isCompleted() && player.getPlayer() != null) {
            Quest quest = getQuest(current.getQuestId());
            if (quest != null) {
                for (PotionEffect effect : quest.getRewards()) {
                    player.getPlayer().removePotionEffect(effect.getType());
                }
            }
        }

        player.removeQuest(current.getSlot());
        GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage("§eVotre quête a été réinitialisée par un administrateur. Vous pouvez en accepter une nouvelle !");
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
                player.getPlayer().sendMessage("§cVous avez déjà une quête terminée à réclamer !");
            } else {
                player.getPlayer().sendMessage("§cVous avez déjà une quête en cours. Terminez-la d'abord !");
            }
            return;
        }

        // Vérifier la limite journalière
        if (!force && player.countTodayQuests() >= DAILY_QUEST_LIMIT) {
            player.getPlayer().sendMessage("§cVous avez atteint votre limite de §6" + DAILY_QUEST_LIMIT + " quête(s) aujourd'hui§c. Revenez demain !");
            return;
        }

        QuestDifficulty difficulty = getRandomDifficulty();
        Quest quest = getRandomQuest(difficulty, player.getUuid());
        if (quest == null) return;

        // Prochain slot = nombre total de quêtes du jour
        int nextSlot = player.countTodayQuests();

        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        // Mémoriser cette quête comme dernière attribuée pour l'anti-répétition
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        player.getPlayer().sendMessage("§aNouvelle quête acceptée auprès de §b" + trader.getNameId() + " §a: §6" + quest.getName());
        player.getPlayer().sendMessage("§7" + quest.getDescription());
        player.getPlayer().sendMessage("§8Quête " + (nextSlot + 1) + "/" + DAILY_QUEST_LIMIT + " du jour.");
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
            player.getPlayer().sendMessage("§e[TEST] Quête de test assignée : §6" + quest.getName());
            player.getPlayer().sendMessage("§7" + quest.getDescription());
            player.getPlayer().sendMessage("§8Objectif : " + quest.getGoal() + " | Difficulté : " + quest.getDifficulty().name());
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
            player.getPlayer().sendMessage("§cCette quête doit être validée auprès de §b" + data.getTraderId());
            return false;
        }

        Quest quest = getQuest(data.getQuestId());
        if (quest == null) return false;

        for (PotionEffect effect : quest.getRewards()) {
            player.getPlayer().addPotionEffect(effect);
        }

        // Résoudre le métier associé au trader et créditer la réputation
        EJob rewardJob = trader.getJob();
        if (rewardJob != null) {
            player.addJobReputation(rewardJob, quest.getReputationReward());
        }
        player.getPlayer().sendMessage("§aVous avez reçu les récompenses de la quête ! §b+" + quest.getReputationReward() + " réputation §aavec §b" + trader.getNameId());

        data.setClaimed(true);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        return true;
    }

    /**
     * Incrémente la progression de la quête active en cours (non complétée).
     */
    public void progressQuest(AlphaPlayer player, QuestType type, Object target, int amount) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null || data.isCompleted()) return;

        Quest quest = getQuest(data.getQuestId());
        if (quest == null || quest.getType() != type) return;

        boolean targetOk = (type == QuestType.FISH) || (quest.getTarget() == null) || quest.getTarget().equals(target);
        if (!targetOk) return;

        data.setProgress(data.getProgress() + amount);

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
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);

        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f);
        player.getPlayer().playSound(myCustomSound);
        player.getPlayer().sendMessage("§aQuête terminée : §6" + quest.getName());
        player.getPlayer().sendMessage("§7Allez voir le Trader pour obtenir votre récompense !");
    }
}