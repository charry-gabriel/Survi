package fr.miuby.survi.job.task;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche répétée (toutes les 20 ticks / 1 seconde) gérant les effets passifs du métier FISHERMAN.
 *
 * <ul>
 *   <li>Niv. 0-9 — Dégâts de pression sous l'eau (si profondeur > seuil du niveau).</li>
 *   <li>Niv. 3+  — Vitesse de nage (DOLPHINS_GRACE, amplificateur configurable).</li>
 *   <li>Niv. 5+  — Hâte sous l'eau (HASTE, amplificateur configurable).</li>
 *   <li>Niv. 7+  — Respiration sous l'eau (WATER_BREATHING).</li>
 *   <li>Niv. 10  — Aucun dégât de pression (pressure-safe-depth = -1).</li>
 * </ul>
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig} ({@code jobs.yml}).</p>
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

            applySwimSpeed(player, level, cfg);
            applyUnderwaterHaste(player, level, cfg);
            applyWaterBreathing(player, level, cfg);
            applyPressureDamage(player, level, cfg);
        }
    }

    private static void applySwimSpeed(Player player, int level, JobsConfig.FishermanCfg cfg) {
        if (level < 3) return;
        int amp = cfg.getSwimSpeedAmplifier()[level];
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE, cfg.getEffectDurationTicks(), amp, false, false, false));
    }

    private static void applyUnderwaterHaste(Player player, int level, JobsConfig.FishermanCfg cfg) {
        if (level < 5) return;
        int amp = cfg.getUnderwaterHasteAmplifier()[level];
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE, cfg.getEffectDurationTicks(), amp, false, false, false));
    }

    private static void applyWaterBreathing(Player player, int level, JobsConfig.FishermanCfg cfg) {
        if (level < 7) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WATER_BREATHING, cfg.getEffectDurationTicks(), 0, false, false, false));
    }

    private static void applyPressureDamage(Player player, int level, JobsConfig.FishermanCfg cfg) {
        int safeDepth = cfg.getPressureSafeDepth()[level];
        if (safeDepth < 0) return; // -1 = illimité, aucun dégât
        if (player.getLocation().getBlockY() < 63 - safeDepth) {
            player.damage(cfg.getPressureDamage());
        }
    }
}
