package fr.miuby.survi.job.task;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche répétée (toutes les 20 ticks / 1 seconde) gérant les effets passifs du métier FISHERMAN.
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
 * <p>Enregistrement dans Survi.java :</p>
 * <pre>{@code new FishermanEffectsTask().runTaskTimer(this, 0L, FishermanEffectsTask.PERIOD_TICKS);}</pre>
 */
public class FishermanEffectsTask extends BukkitRunnable {

    /** Fréquence d'exécution (20 ticks = 1 seconde). */
    public static final long PERIOD_TICKS = 20L;

    @Override
    public void run() {
        JobsConfig.FishermanCfg cfg = JobsConfig.getInstance().getFisherman();

        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
            if (alpha == null) continue;
            int level = alpha.getJobLevel(EJob.FISHERMAN);

            if (!player.isInWater()) continue;

            applyPressureDamage(player, level, alpha, cfg);
        }
    }

    private static void applyPressureDamage(Player player, int level, AlphaPlayer alpha, JobsConfig.FishermanCfg cfg) {
        int safeDepth = cfg.getPressureSafeDepth()[level];
        if (safeDepth == -1) return; // -1 = illimité, aucun dégât
        if (player.getLocation().getBlockY() < 63 - safeDepth) {
            alpha.setLastJobDamageCause(EJob.FISHERMAN);
            player.damage(cfg.getPressureDamage());
        }
    }
}