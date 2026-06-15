package fr.miuby.survi.world;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.logging.Level;

/**
 * Gère le cycle de pluie configurable et l'état de la pluie acide.
 *
 * <p>Le cycle pluie remplace la météo vanilla en deux phases :
 * <ol>
 *   <li>Pluie : durée fixe ({@code rain.duration-seconds}), contrôlée par ce manager.</li>
 *   <li>Accalmie : délai aléatoire dans [{@code cooldown-min-seconds}, {@code cooldown-max-seconds}].</li>
 * </ol>
 * {@code setWeatherDuration(MAX_VALUE)} est appliqué sur tous les mondes gérés après chaque
 * transition pour bloquer les changements vanilla.
 */
public class RainManager {

    private final Random random = new Random();

    @Getter private boolean acidRainActive = false;
    @Getter private boolean currentlyRaining = false;

    private BukkitTask stopRainTask;
    private BukkitTask nextRainTask;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    public void init() {
        applyWeatherToAllWorlds(false);
        scheduleNextRain();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[RainManager] Initialisé.");
    }

    public void stop() {
        if (stopRainTask != null) { stopRainTask.cancel(); stopRainTask = null; }
        if (nextRainTask != null) { nextRainTask.cancel(); nextRainTask = null; }
    }

    // ─── Pluie acide ─────────────────────────────────────────────────────────────

    public void setAcidRainActive(boolean active) {
        this.acidRainActive = active;
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[RainManager] Pluie acide " + (active ? "activée" : "désactivée") + ".");
    }

    // ─── Cycle pluie ─────────────────────────────────────────────────────────────

    private void scheduleNextRain() {
        SurviConfig cfg = SurviConfig.getInstance();
        long minTicks = (long) cfg.getRainCooldownMinSeconds() * 20L;
        long maxTicks = (long) cfg.getRainCooldownMaxSeconds() * 20L;
        long delay    = minTicks + (long) (random.nextDouble() * (maxTicks - minTicks));

        nextRainTask = GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), this::startRain, delay);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[RainManager] Prochaine pluie dans " + (delay / 20L) + "s.");
    }

    private void startRain() {
        SurviConfig cfg = SurviConfig.getInstance();
        long durationTicks = (long) cfg.getRainDurationSeconds() * 20L;

        applyWeatherToAllWorlds(true);
        currentlyRaining = true;
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[RainManager] Pluie démarrée (" + cfg.getRainDurationSeconds() + "s).");

        stopRainTask = GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), this::stopRain, durationTicks);
    }

    private void stopRain() {
        applyWeatherToAllWorlds(false);
        currentlyRaining = false;
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[RainManager] Pluie terminée.");
        scheduleNextRain();
    }

    /**
     * Réapplique l'état météo courant à tous les mondes gérés.
     * À appeler après un reset de monde pour que le nouveau monde soit sous contrôle.
     */
    public void applyCurrentStateToManagedWorlds() {
        applyWeatherToAllWorlds(currentlyRaining);
    }

    /**
     * Applique l'état météo à tous les mondes configurés dans {@code rain.worlds}.
     * {@code setWeatherDuration(MAX_VALUE)} bloque les transitions vanilla ultérieures.
     */
    private void applyWeatherToAllWorlds(boolean storm) {
        for (EWorld eWorld : SurviConfig.getInstance().getRainWorlds()) {
            var mlWorld = WorldRegistry.get(eWorld);
            if (mlWorld == null) continue;
            World world = mlWorld.getWorld();
            world.setStorm(storm);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setThunderDuration(Integer.MAX_VALUE);
        }
    }
}
