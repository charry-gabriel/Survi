package fr.miuby.survi.player;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.player.MLPlayer;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.role.*;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.world.WorldInitializer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import java.util.*;
import java.util.logging.Level;

public class AlphaPlayer extends MLPlayer implements Serializable {
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

    @Getter
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
        checkOrTeleportToValidWorld();

        this.world = WorldRegistry.get(getPlayer().getWorld().getUID());

        GameManager.getInstance().getAlphaPlayerFactory().getEffectRestoreService().restoreOnJoin(this);

        this.getAlphaLife().actualizeDeath();
        this.getAlphaLife().actualizeSuccess();

        this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());

        Map<String, Integer> rawRep = GameManager.getInstance().getDatabase().quests().getReputation(this.getUuid());
        rawRep.forEach((key, value) -> {
            try {
                reputationByJob.put(EJob.valueOf(key), value);
            } catch (IllegalArgumentException _) {
                // Ancienne donnée avec un nom de trader — ignorée après migration.
            }
        });

        LocalDate today = LocalDate.now();
        GameManager.getInstance().getDatabase().quests().deleteExpiredPlayerQuests(this.getUuid(), today);
        List<PlayerQuestData> loaded = GameManager.getInstance().getDatabase().quests().getActivePlayerQuests(this.getUuid(), today);
        GameManager.getInstance().getQuestManager().restoreQuestsOnJoin(this, loaded);

        // Calcul initial des niveaux de métier
        initJobLevels();
    }

    private void checkOrTeleportToValidWorld() {
        boolean validWorld = WorldInitializer.getWorlds().values().stream()
                .anyMatch(name -> getPlayer().getWorld().getName().equals(name));

        if (!validWorld) {
            Location safeSpawn = WorldRegistry.get(EWorld.VILLAGE).getWorld().getSpawnLocation();
            MiubyLib.runLater(() -> {
                if (getPlayer() != null && getPlayer().isOnline()) getPlayer().teleport(safeSpawn);
            }, 1L);
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

    /** Réputation directe pour un métier donné. */
    public int getJobReputation(EJob job) {
        return reputationByJob.getOrDefault(job, 0);
    }

    /** Somme de toutes les réputations par métier. */
    public int getTotalReputation() {
        return reputationByJob.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Rang global calculé à partir de la réputation totale. */
    public EGlobalRank getGlobalRank() {
        return EGlobalRank.fromReputation(getTotalReputation());
    }

    /**
     * Ajoute de la réputation à un métier.
     * Tous les traders partageant ce métier contribuent à la même valeur.
     *
     * @param job    le métier concerné
     * @param amount montant à ajouter (peut être négatif pour retirer)
     */
    public void addJobReputation(EJob job, int amount) {
        EGlobalRank previousRank = getGlobalRank();

        int newRep = Math.max(0, getJobReputation(job) + amount);
        reputationByJob.put(job, newRep);

        // Persistance : on utilise job.name() comme clé dans la colonne trader_id de la DB
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), job.name(), newRep);

        updateJobLevel(job, newRep);

        EGlobalRank newRank = getGlobalRank();
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
        MLLogManager.getInstance().log(Level.INFO, ELogTag.JOB,
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

        if (newLevel > oldLevel) {
            MiubyLib.callEvent(new AlphaPlayerJobLevelUpEvent(this, job, oldLevel, newLevel));
        }

        if (newLevel > oldLevel && getPlayer() != null && getPlayer().isOnline()) {
            getPlayer().sendMessage(Component.text("⚒ ", NamedTextColor.GREEN).append(job.toComponent()));
            MLLogManager.getInstance().log(Level.INFO, ELogTag.JOB,
                    getPseudo() + " : " + job.name() + " niv. " + oldLevel + " -> " + newLevel);
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