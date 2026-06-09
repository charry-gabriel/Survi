package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.lib.log.MLLogManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.logging.Level;

/**
 * Gère la zone autorisée dans le Village et le spawn associé.
 *
 * <ul>
 *   <li>Le rayon et le spawn évoluent selon les paliers définis dans {@code config.yml}
 *       (section {@code village-zone.stages}).</li>
 *   <li>Le timestamp de démarrage est persisté en DB (survit aux redémarrages).</li>
 *   <li>À chaque changement de palier : spawn monde mis à jour + portail village
 *       rafraîchi pour tous les joueurs présents.</li>
 * </ul>
 *
 * @see #start()  Lance le timer (déclenché par {@code StartVillageZoneEffect} au levelup d'un villageois)
 * @see #reset()  Remet le timer à zéro (tests — {@code /survi zone reset})
 */
public class VillageZoneManager {

    // ─── Clé DB ──────────────────────────────────────────────────────────────────
    private static final String DB_KEY_START = "village_zone_start_timestamp";

    /** Intervalle de vérification de changement de palier (en ticks). */
    private static final long CHECK_INTERVAL_TICKS = 20L * 60; // toutes les minutes
    public VillageZoneManager() {}

    // ─── État ────────────────────────────────────────────────────────────────────

    /** Epoch millis du début de la partie (-1 = pas encore démarré). */
    @Getter
    private long startTimestamp = -1L;

    /** Index du palier actuellement actif (-1 = non initialisé). */
    @Getter
    private int currentStageIndex = -1;

    /** {@code true} si le timer a été démarré (via {@link #start()} ou chargé depuis la DB). */
    @Getter
    private boolean started = false;

    private BukkitTask stageCheckTask;

    // ─── Initialisation (appelée par GameManager) ─────────────────────────────────

    /**
     * Charge le timestamp éventuellement persisté en DB.
     * S'il existe, reprend le timer là où il en était.
     */
    public void init() {
        loadStartTimestampFromDB();

        if (started) {
            applyCurrentStage(true);
            startStageCheckTimer();
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Reprise depuis DB — palier=" + currentStageIndex
                            + " rayon=" + getCurrentRadius() + " blocs"
                            + " écoulé=" + String.format("%.2f", getElapsedMinutes() / 60f) + "h");
        } else {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Initialisé — en attente du premier levelup de villageois");
        }
    }

    public void stop() {
        cancelStageCheckTimer();
    }

    // ─── Commandes ─────────────────────────────────────────────────────────────────

    /**
     * Démarre le timer de zone (déclenché par {@link StartVillageZoneEffect} au levelup d'un villageois).
     * Si le timer était déjà en cours, ne fait rien et retourne {@code false}.
     */
    public boolean start() {
        if (started) return false;

        startTimestamp    = System.currentTimeMillis();
        started           = true;
        currentStageIndex = -1;

        saveStartTimestampToDB();
        applyCurrentStage(true);
        startStageCheckTimer();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Partie démarrée — timestamp=" + startTimestamp);
        return true;
    }

    /**
     * Remet le timer à zéro (tests uniquement).
     * Arrête le timer en cours, efface la DB, repart de l'heure 0.
     */
    public void reset() {
        cancelStageCheckTimer();

        startTimestamp    = System.currentTimeMillis();
        started           = true;
        currentStageIndex = -1;

        saveStartTimestampToDB();
        applyCurrentStage(true);
        startStageCheckTimer();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Timer réinitialisé — palier 0 restauré");
    }

    // ─── API publique ─────────────────────────────────────────────────────────────

    /**
     * Retourne le rayon autorisé (blocs XZ) pour le palier actif.
     * Retourne {@link Integer#MAX_VALUE} si le timer n'est pas démarré.
     */
    public int getCurrentRadius() {
        if (!started) return Integer.MAX_VALUE;
        List<VillageZoneConfig.VillageZoneStage> stages = SurviConfig.getInstance().getVillageZoneConfig().stages();
        if (stages.isEmpty()) return Integer.MAX_VALUE;
        return stages.get(computeStageIndex()).radius();
    }

    /**
     * Retourne le spawn du village pour le palier actif, ou le spawn par défaut du monde
     * si le timer n'est pas démarré / aucun stage configuré.
     */
    public Location getCurrentSpawnLocation() {
        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        World world = villageName != null ? Bukkit.getWorld(villageName) : null;

        if (!started || world == null) {
            return world != null ? world.getSpawnLocation() : null;
        }

        List<VillageZoneConfig.VillageZoneStage> stages = SurviConfig.getInstance().getVillageZoneConfig().stages();
        if (stages.isEmpty()) return world.getSpawnLocation();

        VillageZoneConfig.VillageZoneSpawn s = stages.get(computeStageIndex()).spawn();
        return new Location(world, s.x() + 0.5, s.y(), s.z() + 0.5, s.yaw(), s.pitch());
    }

    /**
     * Indique si une {@link Location} dépasse la zone autorisée (contrôle XZ uniquement).
     * Retourne toujours {@code false} si le timer n'est pas démarré.
     */
    public boolean isLocationOutOfBounds(Location loc) {
        if (!started) return false;

        VillageZoneConfig cfg = SurviConfig.getInstance().getVillageZoneConfig();
        if (cfg.stages().isEmpty()) return false;

        double dx = Math.abs(loc.getX() - cfg.centerX());
        double dz = Math.abs(loc.getZ() - cfg.centerZ());

        double radius = getCurrentRadius();

        return dx > radius || dz > radius;
    }

    /** Nombre d'heures entières écoulées depuis le début de la partie. */
    public float getElapsedMinutes() {
        if (!started || startTimestamp < 0) return 0L;
        return (System.currentTimeMillis() - startTimestamp) / 60_000f;
    }

    /**
     * Retourne le numéro du jour de jeu courant (1-based) depuis le démarrage de la partie.
     * Le jour 1 commence dès que {@link #start()} est appelé ; le jour 2 commence 24 h plus tard, etc.
     * Retourne 0 si la partie n'a pas encore démarré.
     */
    public int getGameDayCount() {
        if (!started || startTimestamp < 0) return 0;
        long millisPerDay = 24L * 60 * 60 * 1000L;
        long elapsed = System.currentTimeMillis() - startTimestamp;
        return (int) (elapsed / millisPerDay) + 1;
    }

    // ─── Logique interne ─────────────────────────────────────────────────────────

    private void startStageCheckTimer() {
        stageCheckTask = GameManager.getInstance().getScheduler().runTaskTimer(
                GameManager.getInstance().getPlugin(),
                () -> applyCurrentStage(false),
                CHECK_INTERVAL_TICKS,
                CHECK_INTERVAL_TICKS
        );
    }

    private void cancelStageCheckTimer() {
        if (stageCheckTask != null) {
            stageCheckTask.cancel();
            stageCheckTask = null;
        }
    }

    /**
     * Calcule l'index du palier actif selon le temps écoulé.
     * Les stages doivent être triés par {@code afterHours} croissant dans le config.
     */
    private int computeStageIndex() {
        float elapsedMinutes = getElapsedMinutes() / 60f;
        List<VillageZoneConfig.VillageZoneStage> stages = SurviConfig.getInstance().getVillageZoneConfig().stages();

        int best = 0;
        for (int i = 0; i < stages.size(); i++) {
            if (elapsedMinutes >= stages.get(i).afterHours()) best = i;
        }
        return best;
    }

    /**
     * Applique le palier courant si celui-ci a changé (ou {@code force = true}).
     * Met à jour le spawn monde ET le portail village pour tous les joueurs présents.
     */
    private void applyCurrentStage(boolean force) {
        if (!started) return;

        int newIndex = computeStageIndex();
        if (!force && newIndex == currentStageIndex) return;
        currentStageIndex = newIndex;

        List<VillageZoneConfig.VillageZoneStage> stages = SurviConfig.getInstance().getVillageZoneConfig().stages();
        if (stages.isEmpty()) return;

        VillageZoneConfig.VillageZoneStage stage = stages.get(currentStageIndex);

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (villageName == null) return;
        World world = Bukkit.getWorld(villageName);
        if (world == null) return;

        // ── Mise à jour du spawn monde ─────────────────────────────────────────────
        VillageZoneConfig.VillageZoneSpawn sp = stage.spawn();
        world.setSpawnLocation(sp.x(), sp.y(), sp.z());

        // ── Mise à jour du portail (fakeblocks) ────────────────────────────────────
        VillageZoneConfig.VillageZonePortal portalCfg = stage.portal();
        Location min = new Location(world, portalCfg.minX(), portalCfg.minY(), portalCfg.minZ());
        Location max = new Location(world, portalCfg.maxX(), portalCfg.maxY(), portalCfg.maxZ());
        GameManager.getInstance().getWorldPortalManager().updateVillagePortal(villageName, min, max);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Palier " + currentStageIndex
                        + " — rayon=" + stage.radius() + " blocs"
                        + " | spawn=(" + sp.x() + "," + sp.y() + "," + sp.z() + ")"
                        + " | portail=(" + portalCfg.minX() + "," + portalCfg.minY() + "," + portalCfg.minZ()
                        + ")→(" + portalCfg.maxX() + "," + portalCfg.maxY() + "," + portalCfg.maxZ() + ")");
    }

    // ─── Persistance ─────────────────────────────────────────────────────────────

    private void loadStartTimestampFromDB() {
        var db = GameManager.getInstance().getDatabase().system();
        String stored = db.getServerData(DB_KEY_START);

        if (stored == null) {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Aucun timestamp en DB — en attente du premier levelup de villageois");
            return;
        }

        try {
            startTimestamp = Long.parseLong(stored);
            started        = true;
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Timestamp chargé depuis DB : " + startTimestamp);
        } catch (NumberFormatException e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "[VillageZoneManager] Timestamp DB invalide, ignoré", e);
        }
    }

    private void saveStartTimestampToDB() {
        GameManager.getInstance().getDatabase().system()
                .saveServerData(DB_KEY_START, String.valueOf(startTimestamp));
    }
}