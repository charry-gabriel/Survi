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
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.world.WorldInitializer;
import lombok.Getter;
import lombok.Setter;
import fr.miuby.survi.system.lang.LangService;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import java.util.*;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class AlphaPlayer extends MLPlayer implements Serializable {
    @Getter
    private final AlphaLife alphaLife;

    @Getter
    private final List<Role> subRoles = new ArrayList<>();

    @Getter
    private final Map<EJob, Integer> reputationByJob = new EnumMap<>(EJob.class);

    @Getter
    private final Map<EJob, Integer> jobLevels = new EnumMap<>(EJob.class);

    /**
     * {@code true} une fois que {@link #reputationByJob} a été chargé avec succès depuis la BDD
     * pour la session en cours. Tant que c'est {@code false}, {@link #addJobReputation} refuse
     * tout gain — sans ce garde-fou, un échec de lecture silencieux (BDD temporairement indisponible)
     * laisserait {@code reputationByJob} vide, et le moindre gain de réputation derrière écraserait
     * la vraie valeur en base via {@code INSERT OR REPLACE}, détruisant la progression réelle du joueur.
     */
    private volatile boolean reputationDataReady = false;

    @Getter
    private final List<PlayerQuestData> activeQuests = new ArrayList<>();

    /**
     * Nombre total de quêtes journalières réclamées par ce joueur depuis le début de la partie,
     * chargé depuis {@code quest_history} à la connexion et incrémenté en mémoire à chaque réclamation.
     */
    @Getter
    @Setter
    private int totalDailyQuestsClaimed = 0;

    @Getter
    @Setter
    private MLWorld world;
    @Getter
    private int death = 0;
    @Getter
    private int success = 0;

    @Setter
    @Getter
    private boolean isTakingNoDamage;
    @Setter
    @Getter
    private Role role;

    /** Métier responsable des derniers dégâts liés à un mécanisme de métier (null si aucun).
     *  Positionné par les tâches/listeners de métier avant d'appliquer des dégâts custom,
     *  consommé puis réinitialisé par {@link fr.miuby.survi.player.service.DeathMessageService}. */
    @Getter @Setter
    private EJob lastJobDamageCause;

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

    /** Nombre de quêtes actives non encore réclamées (en cours ou terminées-non-réclamées). */
    public int countActiveUnclaimedQuests() {
        return (int) activeQuests.stream().filter(q -> !q.isClaimed()).count();
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
        this.world = checkOrTeleportToValidWorld();

        GameManager.getInstance().getAlphaPlayerFactory().getEffectRestoreService().restoreOnJoin(this);

        this.getAlphaLife().actualizeDeath();
        this.getAlphaLife().actualizeSuccess();

        this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());
        GameManager.getInstance().getLockedItemsFactory().applyLockState(this);

        loadReputationFromDatabase(0);

        // Charge TOUTES les quêtes actives (non réclamées) — le système cumulatif ne les expire pas par date.
        // Les quêtes réclamées sont déjà en quest_history ; on les déduit du total via countDailyCompleted.
        this.totalDailyQuestsClaimed = GameManager.getInstance().getDatabase().questHistory().countDailyCompleted(this.getUuid());
        List<PlayerQuestData> loaded = GameManager.getInstance().getDatabase().quests().getPlayerQuests(this.getUuid());
        GameManager.getInstance().getQuestManager().restoreQuestsOnJoin(this, loaded);
    }

    private static final int MAX_REPUTATION_LOAD_ATTEMPTS = 3;
    private static final long REPUTATION_LOAD_RETRY_DELAY_TICKS = 100L; // 5s

    /**
     * Charge {@link #reputationByJob} depuis la BDD et marque {@link #reputationDataReady} à
     * {@code true} en cas de succès uniquement. En cas d'échec, retente automatiquement
     * {@link #MAX_REPUTATION_LOAD_ATTEMPTS} fois avant d'abandonner pour la session — tant que
     * {@code reputationDataReady} reste à {@code false}, {@link #addJobReputation} refuse tout gain.
     */
    private void loadReputationFromDatabase(int attempt) {
        Map<String, Integer> rawRep = GameManager.getInstance().getDatabase().quests().getReputation(this.getUuid());

        if (rawRep == null) {
            if (attempt + 1 >= MAX_REPUTATION_LOAD_ATTEMPTS) {
                MLLogManager.getInstance().log(Level.SEVERE, ELogTag.REPUTATION,
                        "Échec définitif du chargement de la réputation pour " + getPseudo() + " après "
                                + MAX_REPUTATION_LOAD_ATTEMPTS + " tentatives — gains de métier bloqués pour cette session (reco nécessaire).");
                return;
            }
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.REPUTATION,
                    "Échec du chargement de la réputation pour " + getPseudo() + ", nouvelle tentative dans 5s (essai "
                            + (attempt + 1) + "/" + MAX_REPUTATION_LOAD_ATTEMPTS + ")");
            MiubyLib.runLater(() -> loadReputationFromDatabase(attempt + 1), REPUTATION_LOAD_RETRY_DELAY_TICKS);
            return;
        }

        reputationByJob.clear();
        rawRep.forEach((key, value) -> {
            try {
                reputationByJob.put(EJob.valueOf(key), value);
            } catch (IllegalArgumentException _) {
                // Ancienne donnée avec un nom de trader — ignorée après migration.
            }
        });
        initJobLevels();
        reputationDataReady = true;
    }

    private MLWorld checkOrTeleportToValidWorld() {
        boolean validWorld = WorldInitializer.getWorlds().values().stream()
                .anyMatch(name -> getPlayer().getWorld().getName().equals(name));

        if (validWorld)
            return WorldRegistry.get(getPlayer().getWorld().getUID());

        Location safeSpawn = GameManager.getInstance().getVillageZoneManager().getCurrentSpawnLocation();
        if (safeSpawn == null)
            throw new IllegalStateException("Spawn du Village introuvable — checkOrTeleportToValidWorld");

        MiubyLib.runLater(() -> {
            if (getPlayer() != null && getPlayer().isOnline()) getPlayer().teleport(safeSpawn);
        }, 1L);

        return WorldRegistry.get(safeSpawn.getWorld().getUID());
    }

    public void gainOneSuccess(boolean challenge) {
        if(challenge) {
            this.success++;
            this.addSuccess(this.success);
        }
    }

    public void addDeath(int death) {
        this.death = Math.max(0, this.death + death);
        this.alphaLife.setDeath(this.death);
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateDeath(this);
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

    public void setDeath(int death) {
        this.death = death;
        this.alphaLife.setDeath(death);
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
            LangService langService = GameManager.getInstance().getLangService();
            this.getPlayer().sendMessage(
                    langService.text(this.getPlayer(), "player.subrole.added",
                            Placeholder.component("role", role.displayName()))
            );
        }
        return true;
    }

    public boolean removeSubRole(Role role) {
        if (!this.subRoles.remove(role))
            return false;

        if (this.getPlayer() != null && this.getPlayer().isOnline()) {
            LangService langService = GameManager.getInstance().getLangService();
            this.getPlayer().sendMessage(
                    langService.text(this.getPlayer(), "player.subrole.removed",
                            Placeholder.component("role", role.displayName()))
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
        if (!reputationDataReady) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.REPUTATION,
                    "Gain de réputation ignoré pour " + getPseudo() + " (" + job.name() + ", " + amount
                            + ") : données de réputation non chargées — évite d'écraser une valeur potentiellement incorrecte en base.");
            if (getPlayer() != null && getPlayer().isOnline()) {
                getPlayer().sendMessage(GameManager.getInstance().getLangService().text(getPlayer(), "job.reputation_unavailable"));
            }
            return;
        }

        EGlobalRank previousRank = getGlobalRank();

        int newRep = Math.max(0, getJobReputation(job) + amount);
        reputationByJob.put(job, newRep);

        // Persistance : on utilise job.name() comme clé dans la colonne trader_id de la DB
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), job.name(), newRep);

        updateJobLevel(job, newRep);

        EGlobalRank newRank = getGlobalRank();
        if (newRank != previousRank && getPlayer() != null) {
            LangService langService = GameManager.getInstance().getLangService();
            getPlayer().sendMessage(langService.text(getPlayer(), "player.rank_up",
                    Placeholder.component("rank", newRank.displayComponent())));
        }
    }

    /**
     * Définit directement la réputation d'un métier, sans déclencher {@link AlphaPlayerJobLevelUpEvent}
     * ni les notifications/son associés. Réservé aux outils admin de réparation de données
     * (ex : reconstruction de {@code player_reputation} depuis {@code quest_history}) — un recalcul en masse
     * via {@link #addJobReputation} spammerait un broadcast + son par palier franchi et par joueur traité.
     *
     * @param job        le métier concerné
     * @param reputation nouvelle valeur de réputation (négatif ramené à 0)
     */
    public void setJobReputationSilently(EJob job, int reputation) {
        int newRep = Math.max(0, reputation);
        reputationByJob.put(job, newRep);
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), job.name(), newRep);
        jobLevels.put(job, JobLevelConfig.computeLevel(newRep));
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
            getPlayer().sendMessage(GameManager.getInstance().getLangService().text(getPlayer(), "job.note",
                    Placeholder.component("job", job.toComponent())));
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

    /**
     * @param job le métier voulu
     * @return true si ce métier a atteint son niveau maximum pour ce joueur
     */
    public boolean isJobMaxLevel(EJob job) {
        return getJobLevel(job) >= JobLevelConfig.getMaxLevel();
    }
    //endregion
}