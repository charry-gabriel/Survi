package fr.miuby.survi.job.task;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tâche répétée (toutes les 60 ticks / 3 secondes) gérant les effets passifs de grotte du métier MINER.
 *
 * <h3>Comportement</h3>
 * <p>Deux blocs indépendants, chacun sur son propre seuil Y :</p>
 * <ul>
 *   <li><b>VD (view-distance)</b> — activation dès {@code playerY < cave-view-distance-threshold-y[level]},
 *       restauration uniquement quand {@code playerY >= seuil + cave-view-distance-hysteresis}.
 *       La marge d'hystérésis évite les changements répétés quand le joueur oscille autour du seuil.</li>
 *   <li><b>DARKNESS</b> — binaire sur {@code cave-darkness-threshold-y[level]}, sans marge :
 *       appliqué dès que {@code playerY < seuil}, retiré dès que {@code playerY >= seuil}.
 *       Envoyé via paquet NMS pour masquer l'icône et le minuteur côté client.</li>
 * </ul>
 * <p>Sur chaque bloc, {@code threshold = -1} immunise indépendamment (niv. 10 par défaut).</p>
 *
 * <p>Enregistrement dans Survi.java :</p>
 * <pre>{@code new MinerEffectsTask().runTaskTimer(this, 20L, MinerEffectsTask.PERIOD_TICKS);}</pre>
 */
public class MinerEffectsTask extends BukkitRunnable {

    /** Fréquence d'exécution (60 ticks = 3 secondes). */
    public static final long PERIOD_TICKS = 60L;

    /** View-distance (en chunks) imposée sous le seuil VD. */
    private static final int REDUCED_VIEW_DISTANCE = 2;

    /**
     * Durée de l'effet DARKNESS envoyé à chaque tick.
     * Supérieure à {@link #PERIOD_TICKS} pour éviter le scintillement entre deux envois.
     */
    private static final int DARKNESS_DURATION_TICKS = (int) PERIOD_TICKS + 40;

    /** Joueurs dont la view-distance est actuellement réduite — valeur = view-distance d'origine à restaurer. */
    private final Map<UUID, Integer> reducedViewDistance = new HashMap<>();
    /** Joueurs ayant actuellement le paquet DARKNESS envoyé. */
    private final Set<UUID> playersWithDarkness = new HashSet<>();

    @Override
    public void run() {
        JobsConfig.MinerCfg miner = JobsConfig.getInstance().getMiner();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // Hors monde normal (Nether, End) : restaurer immédiatement
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                clearAllEffects(player, uuid);
                continue;
            }

            AlphaPlayer alpha = AlphaPlayer.get(uuid);
            if (alpha == null) {
                clearAllEffects(player, uuid);
                continue;
            }

            int level    = alpha.getJobLevel(EJob.MINER);
            int playerY  = player.getLocation().getBlockY();

            updateViewDistance(player, uuid, playerY,
                    miner.getCaveViewDistanceThresholdY()[level], miner.getCaveViewDistanceHysteresis());
            updateDarkness(player, uuid, playerY, miner.getCaveDarknessThresholdY()[level]);
        }
    }

    private void clearAllEffects(Player player, UUID uuid) {
        restoreViewDistance(player, uuid);
        clearDarkness(player, uuid);
    }

    // ─── VD (view-distance) ────────────────────────────────────────────────────────

    private void updateViewDistance(Player player, UUID uuid, int playerY, int thresholdVD, int hysteresis) {
        if (thresholdVD == -1) { // immunisé
            restoreViewDistance(player, uuid);
            return;
        }

        if (!reducedViewDistance.containsKey(uuid)) {
            if (playerY < thresholdVD) {
                reducedViewDistance.put(uuid, player.getViewDistance());
                player.setViewDistance(REDUCED_VIEW_DISTANCE);
            }
        } else if (playerY >= thresholdVD + hysteresis) {
            restoreViewDistance(player, uuid);
        }
    }

    private void restoreViewDistance(Player player, UUID uuid) {
        Integer original = reducedViewDistance.remove(uuid);
        if (original != null) player.setViewDistance(original);
    }

    // ─── Darkness (NMS) ────────────────────────────────────────────────────────────

    private void updateDarkness(Player player, UUID uuid, int playerY, int thresholdDarkness) {
        if (thresholdDarkness == -1 || playerY >= thresholdDarkness) { // immunisé ou au-dessus du seuil
            clearDarkness(player, uuid);
            return;
        }
        playersWithDarkness.add(uuid);
        sendDarkness(player); // renouveler à chaque tick (durée > période)
    }

    private void clearDarkness(Player player, UUID uuid) {
        if (playersWithDarkness.remove(uuid)) removeDarkness(player);
    }

    // ─── NMS ───────────────────────────────────────────────────────────────────────

    private static void sendDarkness(Player player) {
        ServerPlayer nms = ((CraftPlayer) player).getHandle();
        nms.connection.send(new ClientboundUpdateMobEffectPacket(
                nms.getId(),
                new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DURATION_TICKS, 0, true, false, false),
                true
        ));
    }

    private static void removeDarkness(Player player) {
        ServerPlayer nms = ((CraftPlayer) player).getHandle();
        nms.connection.send(new ClientboundRemoveMobEffectPacket(nms.getId(), MobEffects.DARKNESS));
    }
}