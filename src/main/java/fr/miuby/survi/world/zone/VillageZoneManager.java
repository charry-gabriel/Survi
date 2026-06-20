package fr.miuby.survi.world.zone;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.PortalLocatorManager;
import fr.miuby.survi.world.WorldInitializer;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
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
 *   <li>Les dimensions (centre + largeur/profondeur) et le spawn évoluent selon les paliers
 *       définis dans {@code zone.yml} (section {@code stages}).</li>
 *   <li>La zone est rectangulaire : {@code half-width} sur l'axe X, {@code half-depth} sur l'axe Z.</li>
 *   <li>Le centre peut changer à chaque palier via {@code center-x} / {@code center-z}.</li>
 *   <li>Le timestamp de démarrage est persisté en DB (survit aux redémarrages).</li>
 *   <li>À chaque changement de palier : limite du monde ({@link fr.miuby.lib.world.MLWorld#setLimit}),
 *       spawn et portail village mis à jour pour tous les joueurs présents.</li>
 *   <li>{@link #stop()} restaure la limite "carte entière" posée au boot par {@code WorldInitializer}
 *       (utile pour {@code /survi containers clear} qui doit pouvoir scanner tout le monde).</li>
 * </ul>
 *
 * @see #start()  Lance le timer (déclenché par {@code StartVillageZoneEffect} au levelup d'un villageois)
 * @see #reset()  Remet le timer à zéro (tests — {@code /survi zone reset})
 */
public class VillageZoneManager {

    // ─── Clé DB ──────────────────────────────────────────────────────────────────
    private static final String DB_KEY_START = "village_zone_start_timestamp";

    /** Intervalle de vérification de changement de palier (en ticks). */
    private static final long CHECK_INTERVAL_TICKS = 20L * 60;

    public VillageZoneManager() {}

    // ─── État ────────────────────────────────────────────────────────────────────

    /** Epoch millis du début de la partie (-1 = pas encore démarré). */
    @Getter private long startTimestamp = -1L;

    /** Index du palier actuellement actif (-1 = non initialisé). */
    @Getter private int currentStageIndex = -1;

    /** {@code true} si le timer a été démarré (via {@link #start()} ou chargé depuis la DB). */
    @Getter private boolean started = false;

    @Getter private VillageZoneConfig config;

    /**
     * Limite "carte entière" du monde Village, capturée au boot (posée par {@code WorldInitializer}).
     * Sert de bornes Y pour la limite par palier, et de valeur de restauration sur {@link #stop()}.
     */
    private Rect defaultWorldLimit;

    private BukkitTask stageCheckTask;

    // ─── Initialisation (appelée par GameManager) ─────────────────────────────────

    /**
     * Charge la configuration zone depuis zone.yml, puis le timestamp éventuellement
     * persisté en DB. S'il existe, reprend le timer là où il en était.
     */
    public void init() {
        this.config = ZoneLoader.load(GameManager.getInstance().getPlugin());

        MLWorld mlWorld = WorldRegistry.get(EWorld.VILLAGE);
        if (mlWorld != null) this.defaultWorldLimit = mlWorld.getLimit();

        loadStartTimestampFromDB();

        if (started) {
            applyCurrentStage(true);
            startStageCheckTimer();
            VillageZoneConfig.VillageZoneStage stage = config.stages().isEmpty() ? null : config.stages().get(computeStageIndex());
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Reprise depuis DB — palier=" + currentStageIndex
                            + " zone=" + (stage != null ? stage.halfWidth() + "×" + stage.halfDepth() : "N/A") + " blocs"
                            + " écoulé=" + String.format("%.2f", getElapsedMinutes() / 60f) + "h");
        } else {
            applyLastStageAsDefault();
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[VillageZoneManager] Initialisé — en attente du premier levelup de villageois");
        }
    }

    // ─── Commandes ─────────────────────────────────────────────────────────────────

    /**
     * Démarre le timer de zone (déclenché par une commande).
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
     * Arrête complètement le timer et efface tout l'état (DB incluse).
     * Après un stop, {@link #start()} repart de zéro comme au premier démarrage.
     * Le dernier palier est appliqué comme état par défaut (spawn + portail + limite monde).
     */
    public void stop() {
        cancelStageCheckTimer();

        started           = false;
        startTimestamp    = -1L;
        currentStageIndex = -1;

        deleteStartTimestampFromDB();
        applyLastStageAsDefault();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[VillageZoneManager] Arrêté — état et DB effacés, dernier palier appliqué");
    }

    /**
     * Raccourci : stop complet puis redémarrage immédiat.
     * Équivalent à appeler {@link #stop()} puis {@link #start()}.
     */
    public void reset() {
        stop();
        start();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[VillageZoneManager] Timer réinitialisé — palier 0 restauré");
    }

    /**
     * Force le palier actif à {@code index}, en falsifiant {@link #startTimestamp} pour que
     * {@link #computeStageIndex()} retombe sur ce même index — sinon le vérificateur périodique
     * (toutes les {@link #CHECK_INTERVAL_TICKS} ticks) recalculerait le vrai palier au tick suivant
     * et écraserait l'effet de cette commande. Démarre le timer si besoin. Persiste en DB.
     *
     * <p>Effet de bord assumé : {@link #getElapsedMinutes()} (et donc le temps affiché par
     * {@code /survi zone status}) reflète désormais ce {@code startTimestamp} falsifié, plus le
     * vrai temps écoulé depuis cet appel.
     *
     * @return {@code false} si {@code index} est hors bornes ou si aucun palier n'est configuré.
     */
    public boolean goToStage(int index) {
        List<VillageZoneConfig.VillageZoneStage> stages = config.stages();
        if (index < 0 || index >= stages.size()) return false;

        float afterHours = stages.get(index).afterHours();
        startTimestamp = System.currentTimeMillis() - (long) (afterHours * 3_600_000L) - 1_000L;
        started        = true;

        saveStartTimestampToDB();
        if (stageCheckTask == null) startStageCheckTimer();
        applyCurrentStage(true);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Palier forcé manuellement à " + index + " — startTimestamp recalculé=" + startTimestamp);
        return true;
    }

    /**
     * Recharge {@code zone.yml} à chaud, sans toucher au timer ni à l'état {@link #started}.
     * Réapplique immédiatement le palier courant avec les nouvelles valeurs (timer démarré),
     * sinon réapplique le dernier palier comme état par défaut.
     */
    public void reloadConfig() {
        this.config = ZoneLoader.load(GameManager.getInstance().getPlugin());

        if (started) {
            applyCurrentStage(true);
        } else {
            applyLastStageAsDefault();
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[VillageZoneManager] Configuration zone.yml rechargée à chaud");
    }

    // ─── API publique ─────────────────────────────────────────────────────────────

    /**
     * Retourne le spawn du village pour le palier actif, ou le spawn du dernier palier
     * si le timer n'est pas démarré. Retourne le spawn monde par défaut si aucun stage n'est configuré.
     */
    public Location getCurrentSpawnLocation() {
        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        World world = villageName != null ? Bukkit.getWorld(villageName) : null;
        if (world == null) return null;
        if (config.stages().isEmpty()) return world.getSpawnLocation();

        int idx = started ? computeStageIndex() : config.stages().size() - 1;
        VillageZoneConfig.VillageZoneSpawn s = config.stages().get(idx).spawn();
        return new Location(world, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
    }

    /**
     * Demi-largeur (axe X) de la zone rectangulaire pour le palier actif.
     * Retourne la valeur du dernier palier si le timer n'est pas démarré.
     */
    public int getCurrentHalfWidth() {
        if (config.stages().isEmpty()) return Integer.MAX_VALUE;
        return config.stages().get(started ? computeStageIndex() : config.stages().size() - 1).halfWidth();
    }

    /**
     * Demi-profondeur (axe Z) de la zone rectangulaire pour le palier actif.
     * Retourne la valeur du dernier palier si le timer n'est pas démarré.
     */
    public int getCurrentHalfDepth() {
        if (config.stages().isEmpty()) return Integer.MAX_VALUE;
        return config.stages().get(started ? computeStageIndex() : config.stages().size() - 1).halfDepth();
    }

    /**
     * Centre de la zone sur l'axe X pour le palier actif.
     * Retourne la valeur du dernier palier si le timer n'est pas démarré.
     */
    public int getCurrentCenterX() {
        if (config.stages().isEmpty()) return 0;
        return config.stages().get(started ? computeStageIndex() : config.stages().size() - 1).centerX();
    }

    /**
     * Centre de la zone sur l'axe Z pour le palier actif.
     * Retourne la valeur du dernier palier si le timer n'est pas démarré.
     */
    public int getCurrentCenterZ() {
        if (config.stages().isEmpty()) return 0;
        return config.stages().get(started ? computeStageIndex() : config.stages().size() - 1).centerZ();
    }

    /**
     * Bornes de la zone active sous forme de {@link ZoneBounds}.
     * Retourne les bornes du dernier palier si le timer n'est pas démarré.
     * Retourne {@code null} uniquement si aucun stage n'est configuré.
     */
    public ZoneBounds getCurrentBounds() {
        if (config.stages().isEmpty()) return null;
        VillageZoneConfig.VillageZoneStage stage = config.stages().get(started ? computeStageIndex() : config.stages().size() - 1);
        return new ZoneBounds(stage.centerX(), stage.centerZ(), stage.halfWidth(), stage.halfDepth());
    }

    /**
     * Indique si une {@link Location} dépasse la zone autorisée (contrôle XZ uniquement).
     * La zone est rectangulaire : {@code half-width} sur X, {@code half-depth} sur Z,
     * centrée sur le {@code center-x/z} du palier actif (ou du dernier palier si non démarré).
     * Retourne toujours {@code false} si aucun stage n'est configuré.
     */
    public boolean isLocationOutOfBounds(Location loc) {
        ZoneBounds bounds = getCurrentBounds();
        return bounds != null && bounds.isOutside(loc);
    }

    /** Nombre de minutes écoulées depuis le début de la partie. */
    public float getElapsedMinutes() {
        if (!started || startTimestamp < 0) return 0f;
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
                () -> {
                    try {
                        applyCurrentStage(false);
                    } catch (Exception e) {
                        MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                                "[VillageZoneManager] Erreur dans le vérificateur de palier — tâche maintenue", e);
                    }
                },
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
     * Les stages doivent être triés par {@code afterHours} croissant dans zone.yml.
     */
    private int computeStageIndex() {
        float elapsedHours = getElapsedMinutes() / 60f;
        List<VillageZoneConfig.VillageZoneStage> stages = config.stages();

        int best = 0;
        for (int i = 0; i < stages.size(); i++) {
            if (elapsedHours >= stages.get(i).afterHours()) best = i;
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
        int oldIndex = currentStageIndex;
        currentStageIndex = newIndex;

        List<VillageZoneConfig.VillageZoneStage> stages = config.stages();
        if (stages.isEmpty()) return;

        VillageZoneConfig.VillageZoneStage stage = stages.get(currentStageIndex);

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (villageName == null) return;
        World world = Bukkit.getWorld(villageName);
        if (world == null) return;

        // ── Mise à jour de la limite du monde (XZ suit la zone, Y reprend la carte entière) ──
        MLWorld mlWorld = WorldRegistry.get(EWorld.VILLAGE);
        if (mlWorld != null && defaultWorldLimit != null) {
            mlWorld.setLimit(new Rect(
                    stage.centerX() + stage.halfWidth(),  stage.centerX() - stage.halfWidth(),
                    defaultWorldLimit.yMax(),              defaultWorldLimit.yMin(),
                    stage.centerZ() + stage.halfDepth(),  stage.centerZ() - stage.halfDepth()
            ));
        }

        // ── Mise à jour du spawn monde ─────────────────────────────────────────────
        VillageZoneConfig.VillageZoneSpawn sp = stage.spawn();
        world.setSpawnLocation(new Location(world, sp.x(), sp.y(), sp.z(), sp.yaw(), sp.pitch()));

        // ── Mise à jour du portail (fakeblocks) ────────────────────────────────────
        VillageZoneConfig.VillageZonePortal portalCfg = stage.portal();
        Location min = new Location(world, portalCfg.minX(), portalCfg.minY(), portalCfg.minZ());
        Location max = new Location(world, portalCfg.maxX(), portalCfg.maxY(), portalCfg.maxZ());
        GameManager.getInstance().getWorldPortalManager().updateVillagePortal(villageName, min, max);

        // ── Mise à jour du waypoint portail sur la Locator Bar ────────────────────
        PortalLocatorManager plm = GameManager.getInstance().getPortalLocatorManager();
        if (plm != null) plm.updatePortal(world, portalCfg);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Palier " + currentStageIndex
                        + " — zone=" + stage.halfWidth() + "×" + stage.halfDepth() + " blocs"
                        + " | centre=(" + stage.centerX() + "," + stage.centerZ() + ")"
                        + " | limit=[" + (stage.centerX() - stage.halfWidth()) + "→" + (stage.centerX() + stage.halfWidth()) + "]"
                        + " x [" + (stage.centerZ() - stage.halfDepth()) + "→" + (stage.centerZ() + stage.halfDepth()) + "]"
                        + " | spawn=(" + sp.x() + "," + sp.y() + "," + sp.z() + ")"
                        + " | portail=(" + portalCfg.minX() + "," + portalCfg.minY() + "," + portalCfg.minZ()
                        + ")→(" + portalCfg.maxX() + "," + portalCfg.maxY() + "," + portalCfg.maxZ() + ")"
                        + " | locator mis à jour");

        if (!force) MiubyLib.callEvent(new VillageZoneStageUpEvent(oldIndex, currentStageIndex, stage));
    }


    /**
     * Applique le dernier palier configuré comme état par défaut (sans démarrer le timer).
     * Utilisé au boot quand la zone n est pas encore démarrée, et lors d un {@link #stop()}.
     * Sans effet si {@code config.stages()} est vide.
     */
    private void applyLastStageAsDefault() {
        if (config.stages().isEmpty()) return;

        int lastIdx = config.stages().size() - 1;
        VillageZoneConfig.VillageZoneStage stage = config.stages().get(lastIdx);

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (villageName == null) return;
        World world = Bukkit.getWorld(villageName);
        if (world == null) return;

        MLWorld mlWorld = WorldRegistry.get(EWorld.VILLAGE);
        if (mlWorld != null && defaultWorldLimit != null) {
            mlWorld.setLimit(new Rect(
                    stage.centerX() + stage.halfWidth(),  stage.centerX() - stage.halfWidth(),
                    defaultWorldLimit.yMax(),              defaultWorldLimit.yMin(),
                    stage.centerZ() + stage.halfDepth(),  stage.centerZ() - stage.halfDepth()
            ));
        }

        VillageZoneConfig.VillageZoneSpawn sp = stage.spawn();
        world.setSpawnLocation(new Location(world, sp.x(), sp.y(), sp.z(), sp.yaw(), sp.pitch()));

        VillageZoneConfig.VillageZonePortal portalCfg = stage.portal();
        Location min = new Location(world, portalCfg.minX(), portalCfg.minY(), portalCfg.minZ());
        Location max = new Location(world, portalCfg.maxX(), portalCfg.maxY(), portalCfg.maxZ());
        GameManager.getInstance().getWorldPortalManager().updateVillagePortal(villageName, min, max);

        PortalLocatorManager plm = GameManager.getInstance().getPortalLocatorManager();
        if (plm != null) plm.updatePortal(world, portalCfg);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[VillageZoneManager] Palier par défaut (dernier palier " + lastIdx + ")"
                        + " — zone=" + stage.halfWidth() + "x" + stage.halfDepth() + " blocs"
                        + " | spawn=(" + sp.x() + "," + sp.y() + "," + sp.z() + ")"
                        + " | portail=(" + portalCfg.minX() + "," + portalCfg.minY() + "," + portalCfg.minZ()
                        + ")->(" + portalCfg.maxX() + "," + portalCfg.maxY() + "," + portalCfg.maxZ() + ")");
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

    private void deleteStartTimestampFromDB() {
        GameManager.getInstance().getDatabase().system().deleteServerData(DB_KEY_START);
    }
}