package fr.miuby.survi.player;

import fr.miuby.lib.player.MLPlayer;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.role.*;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.world.WorldInitializer;
import fr.miuby.survi.world.WorldPortalManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
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
    private final Map<EJob, Integer> reputationByJob = new EnumMap<>(EJob.class);

    @Getter
    private final Map<EJob, Integer> jobLevels = new EnumMap<>(EJob.class);

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
    private float resistanceModifier;
    @Setter
    @Getter
    private float damageModifier;
    @Getter
    private float endResistanceModifier;
    @Getter
    private float endDamageModifier;
    //endregion

    public AlphaPlayer(UUID uuid) {
        super(uuid);
        SurviConfig cfg = SurviConfig.getInstance();
        this.resistanceModifier    = cfg.getNormalResistanceModifier();
        this.damageModifier        = cfg.getNormalDamageModifier();
        this.endResistanceModifier = cfg.getEndResistanceModifier();
        this.endDamageModifier     = cfg.getEndDamageModifier();
        this.scoreboard = new AlphaScoreboard();
        this.alphaLife = new AlphaLife(this);
    }

    public static AlphaPlayer get(UUID uuid) {
        return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
    }

    // -----------------------------------------------------------------------
    // Helpers quêtes
    // -----------------------------------------------------------------------

    public PlayerQuestData getCurrentActiveQuest() {
        return activeQuests.stream()
                .filter(q -> !q.isClaimed())
                .findFirst()
                .orElse(null);
    }

    public int countTodayQuests() {
        return activeQuests.size();
    }

    public void putQuest(PlayerQuestData data) {
        activeQuests.removeIf(q -> q.getSlot() == data.getSlot());
        activeQuests.add(data);
    }

    public void removeQuest(int slot) {
        activeQuests.removeIf(q -> q.getSlot() == slot);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void onJoinServer() {
        this.player.setScoreboard(this.scoreboard.getScoreboard());

        checkOrTeleportToValidWorld();

        this.world = WorldRegistry.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);

        GameManager.getInstance().getVillagerFactory().applyAllCurrentBlessing(this);
        GameManager.getInstance().getAlphaPlayerFactory().setPlayersToTeam(this.scoreboard);

        this.getAlphaLife().actualizeDeath();
        this.getAlphaLife().actualizeSuccess();

        this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());

        WorldPortalManager.getInstance().sendAllFakePortalBlocks(this.player);

        // Chargement réputation par métier depuis la DB
        // La DB stocke job.name() comme clé (ex. "MINEUR"), on convertit en EJob.
        Map<String, Integer> rawRep = GameManager.getInstance().getDatabase().quests().getReputation(this.getUuid());
        rawRep.forEach((key, value) -> {
            try {
                reputationByJob.put(EJob.valueOf(key), value);
            } catch (IllegalArgumentException _) {
                // Ancienne donnée avec un nom de trader — ignorée après migration.
            }
        });

        List<PlayerQuestData> loaded = GameManager.getInstance().getDatabase().quests().getPlayerQuests(this.getUuid());
        cleanupExpiredQuestsOnJoin(loaded);

        // Calcul initial des niveaux de métier
        initJobLevels();
    }

    private void checkOrTeleportToValidWorld() {
        boolean validWorld = false;
        for (String name : WorldInitializer.getWorlds().values())
            if (getPlayer().getWorld().getName().equals(name))
                validWorld = true;

        if (!validWorld) {
            World village = WorldRegistry.get(EWorld.VILLAGE).getWorld();
            Location safeSpawn = village.getSpawnLocation();
            getPlayer().teleport(safeSpawn);
        }
    }

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

        for (PlayerQuestData q : expired) {
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(this.getUuid(), q.getSlot());
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.QUEST,
                    "Quête expirée (slot " + q.getSlot() + ") supprimée pour " + this.getPseudo());
        }

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

        this.activeQuests.addAll(valid);

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

    // -----------------------------------------------------------------------
    // Réputation
    // -----------------------------------------------------------------------

    /**
     * Retourne la réputation du joueur pour le métier lié à ce trader.
     * Compatibilité ascendante : VillagerListener, TabDisplayManager, VillagerCommand
     * utilisent cette méthode sans avoir à connaître EJob.
     * Retourne 0 si le trader n'est associé à aucun métier.
     */
    public int getReputation(String traderId) {
        return GameManager.getInstance().getVillagerFactory().getTraders().stream()
                .filter(t -> traderId.equals(t.getNameId()) && t.getJob() != null)
                .map(t -> reputationByJob.getOrDefault(t.getJob(), 0))
                .findFirst()
                .orElse(0);
    }

    /** Réputation directe pour un métier donné. */
    public int getJobReputation(EJob job) {
        return reputationByJob.getOrDefault(job, 0);
    }

    /** Somme de toutes les réputations par métier. */
    public int getTotalReputation() {
        return reputationByJob.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Rang global calculé à partir de la réputation totale. */
    public GlobalRank getGlobalRank() {
        return GlobalRank.fromReputation(getTotalReputation());
    }

    /**
     * Ajoute de la réputation à un métier.
     * Tous les traders partageant ce métier contribuent à la même valeur.
     *
     * @param job    le métier concerné
     * @param amount montant à ajouter (peut être négatif pour retirer)
     */
    public void addJobReputation(EJob job, int amount) {
        GlobalRank previousRank = getGlobalRank();

        int newRep = Math.max(0, getJobReputation(job) + amount);
        reputationByJob.put(job, newRep);

        // Persistance : on utilise job.name() comme clé dans la colonne trader_id de la DB
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), job.name(), newRep);

        updateJobLevel(job, newRep);

        GlobalRank newRank = getGlobalRank();
        if (newRank != previousRank && getPlayer() != null) {
            getPlayer().sendMessage(
                    Component.text("✦ Nouveau rang atteint : ", NamedTextColor.GOLD)
                            .append(newRank.displayComponent())
                            .append(Component.text(" (réputation totale : " + getTotalReputation() + ")", NamedTextColor.GRAY))
            );
        }
    }

    // -----------------------------------------------------------------------
    // Métiers
    // -----------------------------------------------------------------------

    /**
     * Initialise les niveaux de métier depuis la réputation chargée.
     * Appelé dans onJoinServer(), après le chargement de reputationByJob.
     */
    private void initJobLevels() {
        for (EJob job : EJob.values()) {
            int rep = reputationByJob.getOrDefault(job, 0);
            jobLevels.put(job, JobLevelConfig.computeLevel(rep));
        }
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.JOB,
                "Niveaux de métier initialisés pour " + getPseudo());
    }

    /**
     * Recalcule et met à jour le niveau d'un métier.
     * Envoie un message au joueur en cas de level-up.
     */
    private void updateJobLevel(EJob job, int newReputation) {
        int oldLevel = jobLevels.getOrDefault(job, 0);
        int newLevel = JobLevelConfig.computeLevel(newReputation);
        jobLevels.put(job, newLevel);

        if (newLevel > oldLevel && getPlayer() != null && getPlayer().isOnline()) {
            int nextThreshold = JobLevelConfig.getNextThreshold(newReputation);
            String progress = nextThreshold >= 0
                    ? " (" + newReputation + "/" + nextThreshold + " rep)"
                    : " (niveau maximum atteint !)";
            getPlayer().sendMessage(
                    Component.text("⚒ Métier ", NamedTextColor.GREEN)
                            .append(job.toComponent())
                            .append(Component.text(" : ", NamedTextColor.GREEN))
                            .append(Component.text(JobLevelConfig.getLevelName(newLevel), NamedTextColor.GOLD))
                            .append(Component.text(progress, NamedTextColor.GRAY))
            );
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.JOB,
                    getPseudo() + " : métier " + job.name()
                            + " niv. " + oldLevel + " -> " + newLevel
                            + " (rep=" + newReputation + ")");
        }
    }

    /**
     * Retourne le niveau actuel d'un métier pour ce joueur.
     *
     * @param job le métier voulu
     * @return niveau du métier (0 au minimum)
     */
    public int getJobLevel(EJob job) {
        return jobLevels.getOrDefault(job, 0);
    }
    //endregion
}
