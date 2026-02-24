package fr.miuby.survi.quest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.Trader;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {
    private static QuestManager instance;
    
    @Getter
    private final List<Quest> questPool = new ArrayList<>();
    private final Random random = new Random();

    public static QuestManager getInstance() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }

    public QuestManager() {
        loadQuests();
    }

    private void loadQuests() {
        File questFile = new File(GameManager.getInstance().getPlugin().getDataFolder(), "quests.yml");
        if (!questFile.exists()) {
            GameManager.getInstance().getPlugin().saveResource("quests.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
        if (!config.contains("quests")) {
            // Tentative de lecture depuis les ressources si vide ou corrompu
            try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(GameManager.getInstance().getPlugin().getResource("quests.yml")))) {
                config = YamlConfiguration.loadConfiguration(reader);
            } catch (Exception e) {
                LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.QUEST, "Impossible de charger les quêtes depuis le YAML !");
                return;
            }
        }

        questPool.clear();

        List<?> questsList = config.getList("quests");
        if (questsList == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.QUEST, "La liste 'quests' est absente ou vide dans quests.yml");
            return;
        }

        for (Object obj : questsList) {
            if (!(obj instanceof Map<?, ?> map)) continue;

            try {
                String id = (String) map.get("id");
                String name = (String) map.get("name");
                String description = (String) map.get("description");
                QuestType type = QuestType.valueOf((String) map.get("type"));
                QuestDifficulty difficulty = QuestDifficulty.valueOf((String) map.get("difficulty"));
                
                // Use Number for safer conversion from YAML to int
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
                        if (rewardObj instanceof Map<?, ?> rewardMap) {
                            PotionEffectType effectType = PotionEffectType.getByName((String) rewardMap.get("type"));
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
                e.printStackTrace();
            }
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST, questPool.size() + " quêtes chargées depuis quests.yml");
    }

    public Quest getQuest(String id) {
        return questPool.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    public Quest getRandomQuest(QuestDifficulty difficulty) {
        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .toList();
        if (filtered.isEmpty()) return null;
        return filtered.get(random.nextInt(filtered.size()));
    }

    public QuestDifficulty getRandomDifficulty() {
        int chance = random.nextInt(100);
        if (chance < 5) return QuestDifficulty.LEGENDARY; // 5%
        if (chance < 30) return QuestDifficulty.RARE;     // 25%
        return QuestDifficulty.COMMON;                    // 70%
    }

    public void resetQuest(AlphaPlayer player) {
        player.setActiveQuest(null);
        GameManager.getInstance().getDatabase().clearPlayerQuest(player.getUuid());
        
        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage("§eVotre quête du jour a été réinitialisée par un administrateur.");
        }
    }

    public void assignQuest(AlphaPlayer player, Trader trader) {
        QuestDifficulty difficulty = getRandomDifficulty();
        Quest quest = getRandomQuest(difficulty);
        if (quest == null) return;

        // Si le joueur a déjà une quête aujourd'hui, ne pas écraser
        PlayerQuestData existing = player.getActiveQuest();
        if (existing != null && existing.getLastAccepted() != null && existing.getLastAccepted().isEqual(LocalDate.now())) {
            player.getPlayer().sendMessage("§cVous avez déjà une quête active aujourd'hui.");
            return;
        }

        PlayerQuestData data = new PlayerQuestData(quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.setActiveQuest(data);
        GameManager.getInstance().getDatabase().updatePlayerQuest(player.getUuid(), data);
        
        player.getPlayer().sendMessage("§aNouvelle quête acceptée auprès de §b" + trader.getNameId() + " §a: §6" + quest.getName());
        player.getPlayer().sendMessage("§7" + quest.getDescription());
    }

    public void completeQuest(AlphaPlayer player, Trader trader) {
        PlayerQuestData data = player.getActiveQuest();
        if (data == null || !data.isCompleted() || data.isClaimed()) return;
        if (data.getTraderId() != null && !data.getTraderId().equals(trader.getNameId())) {
            player.getPlayer().sendMessage("§cCette quête doit être validée auprès de §b" + data.getTraderId());
            return;
        }

        Quest quest = getQuest(data.getQuestId());
        if (quest == null) return;

        // Apply rewards
        for (PotionEffect effect : quest.getRewards()) {
            player.getPlayer().addPotionEffect(effect);
        }

        // Give reputation
        player.addReputation(trader.getNameId(), quest.getReputationReward());
        
        player.getPlayer().sendMessage("§aVous avez reçu les récompenses de la quête ! §b+" + quest.getReputationReward() + " réputation §aavec §b" + trader.getNameId());
        
        // Marquer comme réclamée (claim) pour empêcher les doubles récompenses
        data.setClaimed(true);
        GameManager.getInstance().getDatabase().updatePlayerQuest(player.getUuid(), data);
    }

    public void progressQuest(AlphaPlayer player, QuestType type, Object target, int amount) {
        PlayerQuestData data = player.getActiveQuest();
        if (data == null || data.isCompleted() || !LocalDate.now().isEqual(data.getLastAccepted())) return;

        Quest quest = getQuest(data.getQuestId());
        if (quest == null || quest.getType() != type) return;

        boolean targetOk = true;
        if (type != QuestType.FISH) { // FISH: accepter n'importe quel poisson
            targetOk = (quest.getTarget() == null) || quest.getTarget().equals(target);
        }
        if (!targetOk) return;

        data.setProgress(data.getProgress() + amount);
        
        if (data.getProgress() >= quest.getGoal()) {
            completeQuestInternal(player, quest);
        } else {
            GameManager.getInstance().getDatabase().updatePlayerQuest(player.getUuid(), data);
        }
    }

    private void completeQuestInternal(AlphaPlayer player, Quest quest) {
        PlayerQuestData data = player.getActiveQuest();
        data.setProgress(quest.getGoal());
        data.setCompleted(true);
        GameManager.getInstance().getDatabase().updatePlayerQuest(player.getUuid(), data);

        player.getPlayer().sendMessage("§aQuête terminée : §6" + quest.getName());
        player.getPlayer().sendMessage("§7Allez voir le Trader pour obtenir votre récompense !");
    }
}
