package fr.miuby.survi.player;

import fr.miuby.lib.player.MLPlayer;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.*;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.quest.QuestManager;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.potion.PotionEffect;

public class AlphaPlayer extends MLPlayer implements Serializable {
    @Getter
    private final AlphaScoreboard scoreboard;
    @Getter
    private final AlphaLife alphaLife;

    @Getter
    private final List<Role> subRoles = new ArrayList<>();

    @Getter
    private final Map<String, Integer> reputationByTrader = new HashMap<>();
    @Getter
    private final List<PlayerQuestData> activeQuests = new ArrayList<>();

    @Setter
    private MLWorld world;
    @Getter
    private int mort = 0;
    @Getter
    private int success = 0;

    @Setter
    @Getter
    private boolean isTakingNoDamage;
    @Setter
    @Getter
    private Role role;

    @Getter
    private final Map<Attribute, Double> baseAttributes = new HashMap<>();

    //region Modifier
    @Getter
    @Setter
    private float resistanceModifier = 0.2f;
    @Setter
    @Getter
    private float damageModifier = 0.2f;
    @Getter
    private final float endResistanceModifier = 0.7f;
    @Getter
    private final float endDamageModifier = 0.7f;
    //endregion

    public AlphaPlayer(UUID uuid) {
        super(uuid);
        this.scoreboard = new AlphaScoreboard();
        this.alphaLife = new AlphaLife(this);
    }

    public static AlphaPlayer get(UUID uuid) {
        return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
    }

    // -----------------------------------------------------------------------
    // Helpers quêtes
    // -----------------------------------------------------------------------

    /**
     * Retourne la quête active en cours (non complétée OU non réclamée) pour la journée.
     * Utile pour la progression et les messages d'erreur.
     */
    public PlayerQuestData getCurrentActiveQuest() {
        return activeQuests.stream()
                .filter(q -> !q.isClaimed())
                .findFirst()
                .orElse(null);
    }

    /**
     * Nombre de quêtes acceptées aujourd'hui (peu importe leur état).
     */
    public int countTodayQuests() {
        return activeQuests.size();
    }

    /**
     * Ajoute ou remplace un slot de quête en mémoire.
     */
    public void putQuest(PlayerQuestData data) {
        activeQuests.removeIf(q -> q.getSlot() == data.getSlot());
        activeQuests.add(data);
    }

    /**
     * Supprime un slot de quête en mémoire.
     */
    public void removeQuest(int slot) {
        activeQuests.removeIf(q -> q.getSlot() == slot);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void onJoinServer() {
        this.player.setScoreboard(this.scoreboard.getScoreboard());

        this.world = WorldRegistry.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);

        GameManager.getInstance().getVillagerFactory().applyAllCurrentBlessing(this);
        GameManager.getInstance().getAlphaPlayerFactory().setPlayersToTeam(this.scoreboard);

        this.getAlphaLife().actualizeDeath();
        this.getAlphaLife().actualizeSuccess();

        this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());

        // Chargement réputation + quêtes
        this.reputationByTrader.putAll(GameManager.getInstance().getDatabase().quests().getReputation(this.getUuid()));
        List<PlayerQuestData> loaded = GameManager.getInstance().getDatabase().quests().getPlayerQuests(this.getUuid());

        cleanupExpiredQuestsOnJoin(loaded);
    }

    /**
     * Trie les quêtes chargées depuis la DB :
     * - Quêtes d'aujourd'hui : conservées en mémoire, buffs réappliqués si réclamées.
     * - Quêtes d'avant le reset : buffs retirés si réclamées, supprimées de la DB.
     */
    private void cleanupExpiredQuestsOnJoin(List<PlayerQuestData> loaded) {
        this.activeQuests.clear();
        LocalDate today = LocalDate.now();

        List<PlayerQuestData> expired = new ArrayList<>();
        List<PlayerQuestData> valid = new ArrayList<>();

        for (PlayerQuestData quest : loaded) {
            if (today.isEqual(quest.getLastAccepted())) {
                valid.add(quest);
            } else {
                expired.add(quest);
            }
        }

        // Supprimer les quêtes expirées de la DB
        for (PlayerQuestData q : expired) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(this.getUuid(), q.getSlot());
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST,
                    "Quête expirée (slot " + q.getSlot() + ") supprimée pour " + this.getPseudo());
        }

        // Retirer les buffs des quêtes expirées qui avaient été réclamées
        List<PotionEffect> effectsToRemove = new ArrayList<>();
        for (PlayerQuestData q : expired) {
            if (q.isClaimed()) {
                Quest questDef = QuestManager.getInstance().getQuest(q.getQuestId());
                if (questDef != null) effectsToRemove.addAll(questDef.getRewards());
            }
        }

        if (!effectsToRemove.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                if (!this.getPlayer().isOnline()) return;
                for (PotionEffect effect : effectsToRemove) {
                    this.getPlayer().removePotionEffect(effect.getType());
                }
            }, 2L);
        }

        // Garder les quêtes valides en mémoire
        this.activeQuests.addAll(valid);

        // Réappliquer les buffs des quêtes d'aujourd'hui déjà réclamées
        List<PotionEffect> effectsToReapply = new ArrayList<>();
        for (PlayerQuestData q : valid) {
            if (q.isClaimed()) {
                Quest questDef = QuestManager.getInstance().getQuest(q.getQuestId());
                if (questDef != null) effectsToReapply.addAll(questDef.getRewards());
            }
        }

        if (!effectsToReapply.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                if (!this.getPlayer().isOnline()) return;
                for (PotionEffect effect : effectsToReapply) {
                    this.getPlayer().addPotionEffect(effect);
                }
            }, 5L);
        }
    }

    public void gainOneSuccess(boolean challenge) {
        if(challenge) {
            this.success++;
            this.addSuccess(this.success);
        }
    }

    public void addMort(int mort) {
        this.mort += mort;
        this.alphaLife.setDeath(this.mort);
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateMort(this);
    }

    public void addSuccess(int success) {
        this.success = success;
        this.getAlphaLife().regenHealth(() -> this.getAlphaLife().setSuccess(success));
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateSuccess(this);
    }

    public void teleport(MLWorld monde) {
        if (getPlayer() != null)
            getPlayer().teleport(monde.getSpawnPoint());
    }

    //region Getters Setters
    public EWorld getWorld() {
        return (EWorld) this.world.getType();
    }

    public void resetPlayer(){
        this.player = null;
    }

    public void setMort(int mort) {
        this.mort = mort;
        this.alphaLife.setDeath(mort);
    }

    public void setSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);
    }

    public boolean addSubRole(Role role) {
        if (this.subRoles.contains(role))
            return false;

        Role removeRole = null;
        for (Role subRole : this.subRoles) {
            if (subRole.roleId().equals(role.roleId()))
                removeRole = subRole;
        }

        if (removeRole != null)
            this.removeSubRole(removeRole);

        this.subRoles.add(role);

        if (this.getPlayer() != null && this.getPlayer().isOnline()) {
            this.getPlayer().sendMessage(
                    Component.text("Le sous-role ").color(NamedTextColor.YELLOW)
                            .append(role.displayName())
                            .append(Component.text(" a ete ajouté !").color(NamedTextColor.YELLOW))
            );
        }
        return true;
    }

    public boolean removeSubRole(Role role) {
        if (!this.subRoles.remove(role))
            return false;

        if (this.getPlayer() != null && this.getPlayer().isOnline()) {
            this.getPlayer().sendMessage(Component
                    .text("Le sous-role ").color(NamedTextColor.YELLOW)
                    .append(role.displayName())
                    .append(Component.text(" a ete retire !").color(NamedTextColor.YELLOW))
            );
        }
        return true;
    }

    public int getReputation(String traderId) {
        return reputationByTrader.getOrDefault(traderId, 0);
    }

    /** Somme de toutes les réputations avec tous les Traders. */
    public int getTotalReputation() {
        return reputationByTrader.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Rang global calculé à partir de la réputation totale. */
    public GlobalRank getGlobalRank() {
        return GlobalRank.fromReputation(getTotalReputation());
    }

    public void addReputation(String traderId, int amount) {
        GlobalRank previousRank = getGlobalRank();
        int newRep = getReputation(traderId) + amount;
        reputationByTrader.put(traderId, newRep);
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), traderId, newRep);

        GlobalRank newRank = getGlobalRank();
        if (newRank != previousRank && getPlayer() != null) {
            getPlayer().sendMessage(
                    Component.text("✦ Nouveau rang atteint : ", NamedTextColor.GOLD)
                            .append(newRank.displayComponent())
                            .append(Component.text(" (réputation totale : " + getTotalReputation() + ")", NamedTextColor.GRAY))
            );
        }
    }
    //endregion
}