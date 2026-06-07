package fr.miuby.survi.job.task;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche répétée (toutes les 20 ticks / 1 seconde) gérant les effets passifs du métier PÊCHEUR.
 *
 * <ul>
 *   <li>Niv. 0-9 — Dégâts de pression sous l'eau (1HP si profondeur > seuil du niveau).</li>
 *   <li>Niv. 3+  — Vitesse de nage (DOLPHINS_GRACE).</li>
 *   <li>Niv. 5+  — Hâte sous l'eau (HASTE, réduit la pénalité de minage).</li>
 *   <li>Niv. 7+  — Respiration sous l'eau (WATER_BREATHING).</li>
 *   <li>Niv. 10  — Aucun dégât de pression (profondeur illimitée).</li>
 * </ul>
 *
 * <p>Enregistrement dans Survi.java :</p>
 * <pre>{@code new PecheurEffectsTask().runTaskTimer(this, 0L, PecheurEffectsTask.PERIOD_TICKS);}</pre>
 */
public class PecheurEffectsTask extends BukkitRunnable {

    /** Fréquence d'exécution (20 ticks = 1 seconde). */
    public static final long PERIOD_TICKS = 20L;

    /**
     * Profondeur max SANS dégâts (blocs d'eau au-dessus du joueur), par niveau.
     * niv.0 = 0 → dès 1 bloc d'eau.
     * niv.3 = 9 → sûr jusqu'à 9 blocs, 1HP à partir de 10.
     */
    private static final int[] PRESSURE_SAFE_DEPTH = {
            0,            // niv. 0
            1,            // niv. 1
            5,            // niv. 2
            9,            // niv. 3
            20,            // niv. 4
            35,            // niv. 5
            55,            // niv. 6
            80,            // niv. 7
            110,            // niv. 8
            150,            // niv. 9
            Integer.MAX_VALUE      // niv. 10 — aucun dégât
    };

    /** Durée des effets de potion (légèrement > période pour éviter les clignotements d'icône). */
    private static final int EFFECT_DURATION = 25;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
            if (alpha == null) continue;
            int level = alpha.getJobLevel(EJob.PECHEUR);

            if (!player.isInWater()) continue;

            applySwimSpeed(player, level);
            applyUnderwaterHaste(player, level);
            applyWaterBreathing(player, level);
            applyPressureDamage(player, level);
        }
    }

    private static void applySwimSpeed(Player player, int level) {
        if (level < 3) return;
        int amp = level >= 6 ? 1 : 0;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE, EFFECT_DURATION, amp, false, false, false));
    }

    private static void applyUnderwaterHaste(Player player, int level) {
        if (level < 5) return;
        int amp = level >= 9 ? 2 : level >= 7 ? 1 : 0;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE, EFFECT_DURATION, amp, false, false, false));
    }

    private static void applyWaterBreathing(Player player, int level) {
        if (level < 7) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WATER_BREATHING, EFFECT_DURATION, 0, false, false, false));
    }

    private static void applyPressureDamage(Player player, int level) {
        if (level >= 10) return;
        if (player.getLocation().getBlockY() < 63 - PRESSURE_SAFE_DEPTH[level]) {
            player.damage(1.0); // 1HP = 0.5 cœur
        }
    }
}