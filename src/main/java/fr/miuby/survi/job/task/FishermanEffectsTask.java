package fr.miuby.survi.job.task;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Tâche répétée (toutes les 60 ticks / 3 secondes) gérant les effets passifs du métier FISHERMAN.
 *
 * <ul>
 *   <li>Niv. 0-9 — Dégâts de pression sous l'eau (si profondeur > seuil du niveau).</li>
 *   <li>Niv. 10  — Aucun dégât de pression (pressure-safe-depth = -1).</li>
 * </ul>
 *
 * <p>La vitesse de nage ({@code WATER_MOVEMENT_EFFICIENCY}), la capacité respiratoire
 * ({@code OXYGEN_BONUS}) et la vitesse de minage sous l'eau ({@code SUBMERGED_MINING_SPEED})
 * sont des attributs persistants gérés par
 * {@link fr.miuby.survi.job.FishermanAttributeService} — aucun effet de potion n'est appliqué ici.</p>
 *
 * <h3>Observabilité</h3>
 * <p>Chaque entrée/sortie en dégâts de pression ou de pluie acide est journalisée en {@code FINE/JOB}
 * (edge-triggered, pas à chaque tick, pour éviter le bruit malgré les dégâts répétés tant que le
 * joueur reste en zone). {@link PerfTimer} mesure le coût du tick complet ({@code "FishermanEffectsTask.run"}).</p>
 *
 * <p>Enregistrement dans Survi.java :</p>
 * <pre>{@code new FishermanEffectsTask().runTaskTimer(this, 0L, FishermanEffectsTask.PERIOD_TICKS);}</pre>
 */
public class FishermanEffectsTask extends BukkitRunnable {

    /** Fréquence d'exécution (60 ticks = 3 secondes). */
    public static final long PERIOD_TICKS = 60L;

    /** Joueurs subissant actuellement les dégâts de pression sous l'eau. */
    private final Set<UUID> playersTakingPressureDamage = new HashSet<>();
    /** Joueurs subissant actuellement les dégâts de pluie acide. */
    private final Set<UUID> playersTakingAcidRainDamage = new HashSet<>();

    @Override
    public void run() {
        try (var t = PerfTimer.start("FishermanEffectsTask.run")) {
            JobsConfig.FishermanCfg fisherman = JobsConfig.getInstance().getFisherman();
            boolean acidRainActive = GameManager.getInstance().getRainManager().isAcidRainActive();
            int acidRainThreshold = acidRainActive ? fisherman.getAcidRainFishermanLevelThreshold() : 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                AlphaPlayer alpha = AlphaPlayer.get(uuid);
                if (alpha == null) {
                    clearPressureDamage(player, uuid);
                    clearAcidRainDamage(player, uuid);
                    continue;
                }

                int level = alpha.getJobLevel(EJob.FISHERMAN);

                updatePressureDamage(player, uuid, level, alpha, fisherman);

                boolean acidRainHit = acidRainActive && player.isInRain() && level < acidRainThreshold;
                updateAcidRainDamage(player, uuid, acidRainHit, fisherman);
            }
        }
    }

    // ─── Pression sous l'eau ───────────────────────────────────────────────────────

    private void updatePressureDamage(Player player, UUID uuid, int level, AlphaPlayer alpha, JobsConfig.FishermanCfg fisherman) {
        int safeDepth = fisherman.getPressureSafeDepth()[level];
        boolean tooDeep = safeDepth != -1 && player.isInWater() && player.getLocation().getBlockY() < 63 - safeDepth;

        if (!tooDeep) {
            clearPressureDamage(player, uuid);
            return;
        }
        if (playersTakingPressureDamage.add(uuid)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[FishermanPressure] " + player.getName() + " entre en dégâts de pression — y="
                            + player.getLocation().getBlockY() + " safeDepth=" + safeDepth + " niveau=" + level);
        }
        alpha.setLastJobDamageCause(EJob.FISHERMAN);
        player.damage(fisherman.getPressureDamage());
    }

    private void clearPressureDamage(Player player, UUID uuid) {
        if (playersTakingPressureDamage.remove(uuid)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[FishermanPressure] " + player.getName() + " sort des dégâts de pression — y="
                            + player.getLocation().getBlockY());
        }
    }

    // ─── Pluie acide ────────────────────────────────────────────────────────────────

    private void updateAcidRainDamage(Player player, UUID uuid, boolean acidRainHit, JobsConfig.FishermanCfg fisherman) {
        if (!acidRainHit) {
            clearAcidRainDamage(player, uuid);
            return;
        }
        if (playersTakingAcidRainDamage.add(uuid)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[FishermanAcidRain] " + player.getName() + " entre en dégâts de pluie acide");
        }
        player.damage(fisherman.getAcidRainDamage());
        player.sendActionBar(Component.text("☠ Pluie acide", NamedTextColor.DARK_GREEN));
    }

    private void clearAcidRainDamage(Player player, UUID uuid) {
        if (playersTakingAcidRainDamage.remove(uuid)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[FishermanAcidRain] " + player.getName() + " sort des dégâts de pluie acide");
        }
    }
}