package fr.miuby.survi.job.task;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche répétée appliquant les dégâts de pluie acide.
 *
 * <p>Actif seulement quand {@code RainManager.isAcidRainActive()} est {@code true}.
 * Les joueurs exposés à la pluie ({@code player.isInRain()}) dont le niveau Pêcheur
 * est inférieur au seuil ({@code jobs/fisherman.yml → acid-rain-fisherman-level-threshold})
 * subissent {@code acid-rain-damage} dégâts toutes les
 * {@code config.yml → rain.acid.damage-interval-seconds} secondes.
 *
 * <p>Enregistrement dans Survi.java :
 * <pre>{@code new AcidRainDamageTask().runTaskTimer(this, 0L, AcidRainDamageTask.periodTicks());}</pre>
 */
public class AcidRainDamageTask extends BukkitRunnable {

    public static long periodTicks() {
        return (long) SurviConfig.getInstance().getAcidRainDamageIntervalSeconds() * 20L;
    }

    @Override
    public void run() {
        if (!GameManager.getInstance().getRainManager().isAcidRainActive()) return;

        JobsConfig.FishermanCfg fisherman = JobsConfig.getInstance().getFisherman();
        int    threshold = fisherman.getAcidRainFishermanLevelThreshold();
        double damage    = fisherman.getAcidRainDamage();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isInRain()) continue;
            AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
            if (alpha == null) continue;
            if (alpha.getJobLevel(EJob.FISHERMAN) >= threshold) continue;

            player.damage(damage);
            player.sendActionBar(Component.text("☠ Pluie acide", NamedTextColor.DARK_GREEN));
        }
    }
}
