package fr.miuby.survi.job.task;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Tâche répétée (toutes les 60 ticks / 3 secondes) gérant les effets passifs de grotte du métier MINER.
 *
 * <h3>Comportement — monde NORMAL</h3>
 * <p>Deux blocs indépendants, chacun sur son propre seuil Y, tous deux avec hystérésis (l'entrée en zone
 * se fait pile sur le seuil, la sortie exige une marge supplémentaire — voir {@code cave-*-hysteresis} dans
 * {@code jobs/miner.yml} — pour éviter le clignotement quand le joueur oscille pile sur une limite) :</p>
 * <ul>
 *   <li><b>NIGHT_VISION</b> — toujours retiré au-dessus de Y=63 (extérieur, seuil fixe, sans hystérésis).
 *       En dessous, appliqué tant que {@code playerY >= cave-night-vision-threshold-y[level]} (avec
 *       hystérésis {@code cave-night-vision-hysteresis}, ou toujours si seuil = -1), que le joueur n'est
 *       pas en zone DARKNESS (priorité à DARKNESS), et qu'il est en situation de "grotte" —
 *       voir {@link #isInCave(Player, UUID, int)} (hystérésis {@code cave-light-hysteresis}).
 *       Durée appliquée volontairement {@code > 200 ticks} (voir {@link #NIGHT_VISION_DURATION_TICKS}) :
 *       en dessous de ce seuil le client déclenche l'animation de clignotement de fin d'effet vanilla.</li>
 *   <li><b>DARKNESS</b> — sur {@code cave-darkness-threshold-y[level]} (hystérésis {@code cave-darkness-hysteresis}) :
 *       appliqué dès que {@code playerY < seuil}, retiré seulement quand {@code playerY >= seuil + hystérésis}.
 *       Envoyé via paquet NMS pour masquer l'icône et le minuteur côté client.
 *       À l'entrée en zone DARKNESS, un message explique que le niveau de Mineur est trop bas pour cette profondeur.</li>
 * </ul>
 * <p>Sur chaque bloc, {@code threshold = -1} immunise indépendamment (niv. 10 par défaut).</p>
 *
 * <h3>Comportement — Nether</h3>
 * <p>Jamais de NIGHT_VISION. DARKNESS sur {@code nether-darkness-threshold-y[level]}, même mécanisme
 * (paquet NMS, hystérésis {@code cave-darkness-hysteresis}, message à l'entrée en zone) que la variante NORMAL.</p>
 *
 * <h3>Comportement — autres mondes (The End…)</h3>
 * <p>Tous les effets sont retirés immédiatement et tout état d'hystérésis est réinitialisé.</p>
 *
 * <h3>Observabilité</h3>
 * <p>Chaque entrée/sortie d'effet (DARKNESS ou NIGHT_VISION) est journalisée en {@code FINE/JOB}
 * (edge-triggered, pas à chaque tick, pour éviter le bruit). {@link PerfTimer} mesure le coût du tick complet
 * ({@code "MinerEffectsTask.run"}) et celui, plus suspect, de la détection de grotte par lumière du ciel
 * ({@code "MinerEffectsTask.isInCave"}, qui peut déclencher un accès chunk/lighting).</p>
 *
 * <p>Enregistrement dans Survi.java :</p>
 * <pre>{@code new MinerEffectsTask().runTaskTimer(this, 20L, MinerEffectsTask.PERIOD_TICKS);}</pre>
 */
public class MinerEffectsTask extends BukkitRunnable {

    /** Fréquence d'exécution (60 ticks = 3 secondes). */
    public static final long PERIOD_TICKS = 60L;

    /** Coordonnée Y de la surface (limite haute fixe de la night vision — au-dessus, on est à l'extérieur). */
    private static final int SEA_LEVEL_Y = 63;

    /**
     * Durée de l'effet DARKNESS envoyé à chaque tick.
     * Supérieure à {@link #PERIOD_TICKS} pour éviter le scintillement entre deux envois.
     */
    private static final int DARKNESS_DURATION_TICKS = (int) PERIOD_TICKS + 40;

    /**
     * Durée de l'effet NIGHT_VISION appliqué à chaque tick.
     * Le client Minecraft déclenche une animation de clignotement (fade vanilla) lorsque la durée
     * restante de NIGHT_VISION descend à 200 ticks ou moins — il faut donc rester strictement
     * au-dessus de 200 ticks même après soustraction d'une période complète sans rafraîchissement.
     */
    private static final int NIGHT_VISION_DURATION_TICKS = (int) PERIOD_TICKS + 240;

    /** Joueurs ayant actuellement le paquet DARKNESS envoyé (grotte ou Nether). */
    private final Set<UUID> playersWithDarkness = new HashSet<>();
    /** Joueurs ayant actuellement l'effet NIGHT_VISION appliqué. */
    private final Set<UUID> playersWithNightVision = new HashSet<>();
    /** État d'hystérésis du seuil Y de NIGHT_VISION (indépendant de l'effet final, voir {@link #isAboveNightVisionThreshold}). */
    private final Set<UUID> playersAboveNightVisionY = new HashSet<>();
    /** État d'hystérésis de la détection "grotte" par lumière du ciel (voir {@link #isInCave}). */
    private final Set<UUID> playersInCaveLight = new HashSet<>();

    @Override
    public void run() {
        try (var t = PerfTimer.start("MinerEffectsTask.run")) {
            JobsConfig.MinerCfg miner = JobsConfig.getInstance().getMiner();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                World.Environment env = player.getWorld().getEnvironment();

                AlphaPlayer alpha = AlphaPlayer.get(uuid);
                if (alpha == null) {
                    clearAllEffects(player, uuid);
                    continue;
                }

                int level   = alpha.getJobLevel(EJob.MINER);
                int playerY = player.getLocation().getBlockY();

                if (env == World.Environment.NETHER) {
                    clearNightVision(player, uuid); // jamais de night vision au Nether
                    playersAboveNightVisionY.remove(uuid);
                    playersInCaveLight.remove(uuid);
                    updateDarkness(player, uuid, playerY,
                            miner.getNetherDarknessThresholdY()[level], miner.getCaveDarknessHysteresis(), "nether");
                    continue;
                }

                if (env != World.Environment.NORMAL) { // The End, etc. : restaurer immédiatement
                    clearAllEffects(player, uuid);
                    continue;
                }

                int darknessThreshold    = miner.getCaveDarknessThresholdY()[level];
                int nightVisionThreshold = miner.getCaveNightVisionThresholdY()[level];

                updateDarkness(player, uuid, playerY, darknessThreshold, miner.getCaveDarknessHysteresis(), "grotte");
                updateNightVision(player, uuid, playerY, nightVisionThreshold,
                        miner.getCaveNightVisionHysteresis(), miner.getCaveLightHysteresis());
            }
        }
    }

    private void clearAllEffects(Player player, UUID uuid) {
        clearDarkness(player, uuid);
        clearNightVision(player, uuid);
        playersAboveNightVisionY.remove(uuid);
        playersInCaveLight.remove(uuid);
    }

    // ─── Night vision ────────────────────────────────────────────────────────────

    private void updateNightVision(Player player, UUID uuid, int playerY, int nightVisionThreshold,
                                   int nightVisionHysteresis, int lightHysteresis) {
        boolean aboveThreshold = isAboveNightVisionThreshold(uuid, playerY, nightVisionThreshold, nightVisionHysteresis);
        boolean inCave         = isInCave(player, uuid, lightHysteresis);
        boolean inDarknessZone = playersWithDarkness.contains(uuid); // déjà mis à jour ce tick par updateDarkness

        if (playerY > SEA_LEVEL_Y || inDarknessZone || !aboveThreshold || !inCave) {
            clearNightVision(player, uuid);
            return;
        }
        if (playersWithNightVision.add(uuid)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[MinerNightVision] " + player.getName() + " entre en NIGHT_VISION — y=" + playerY
                            + " seuil=" + nightVisionThreshold);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, false));
    }

    /**
     * Hystérésis sur le seuil Y de NIGHT_VISION : l'entrée (passage à "au-dessus du seuil") se fait pile
     * sur {@code threshold}, mais il faut redescendre de {@code hysteresis} blocs de plus pour en sortir.
     */
    private boolean isAboveNightVisionThreshold(UUID uuid, int playerY, int threshold, int hysteresis) {
        if (threshold == -1) return true; // illimité
        boolean wasAbove = playersAboveNightVisionY.contains(uuid);
        boolean nowAbove = wasAbove ? playerY >= threshold - hysteresis : playerY >= threshold;
        if (nowAbove) playersAboveNightVisionY.add(uuid); else playersAboveNightVisionY.remove(uuid);
        return nowAbove;
    }

    /**
     * Détecte une situation de "grotte" : pas de lumière du ciel à hauteur des yeux (donc un plafond,
     * quel que soit le nombre de blocs au-dessus — plus fiable qu'un simple bloc juste au-dessus de la tête,
     * qui échouerait sous un plafond haut) et pas sous l'eau (pas de night vision en nageant/plongeant).
     * Hystérésis : l'entrée exige une lumière strictement nulle, la sortie tolère jusqu'à {@code hysteresis}
     * (évite le clignotement pile à l'entrée d'une grotte, où la valeur peut osciller entre 0 et 1).
     *
     * <p>Instrumenté via {@link PerfTimer} : {@code getLightFromSky()} peut déclencher un accès
     * chunk/lighting si la colonne n'est pas déjà en cache.</p>
     */
    private boolean isInCave(Player player, UUID uuid, int hysteresis) {
        try (var t = PerfTimer.start("MinerEffectsTask.isInCave")) {
            Block eyeBlock = player.getEyeLocation().getBlock();
            int sky = eyeBlock.getLightFromSky();
            boolean wasInCave = playersInCaveLight.contains(uuid);
            boolean nowInCave = wasInCave ? sky <= hysteresis : sky == 0;
            if (nowInCave) playersInCaveLight.add(uuid); else playersInCaveLight.remove(uuid);
            return nowInCave;
        }
    }

    private void clearNightVision(Player player, UUID uuid) {
        if (playersWithNightVision.remove(uuid)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[MinerNightVision] " + player.getName() + " sort de NIGHT_VISION — y=" + player.getLocation().getBlockY());
        }
    }

    // ─── Darkness (NMS) ────────────────────────────────────────────────────────────

    /**
     * Hystérésis sur le seuil Y de DARKNESS : l'entrée se fait pile sur {@code thresholdDarkness},
     * mais il faut remonter de {@code hysteresis} blocs de plus pour en sortir.
     *
     * @param context "grotte" ou "nether", uniquement pour le log d'entrée en zone.
     */
    private void updateDarkness(Player player, UUID uuid, int playerY, int thresholdDarkness, int hysteresis, String context) {
        if (thresholdDarkness == -1) { // immunisé
            clearDarkness(player, uuid);
            return;
        }
        boolean wasDark = playersWithDarkness.contains(uuid);
        boolean nowDark = wasDark ? playerY < thresholdDarkness + hysteresis : playerY < thresholdDarkness;

        if (!nowDark) {
            clearDarkness(player, uuid);
            return;
        }
        if (playersWithDarkness.add(uuid)) {
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "job.miner.depth_too_deep"));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[MinerDarkness] " + player.getName() + " entre en DARKNESS — contexte=" + context
                            + " y=" + playerY + " seuil=" + thresholdDarkness);
        }
        sendDarkness(player); // renouveler à chaque tick (durée > période)
    }

    private void clearDarkness(Player player, UUID uuid) {
        if (playersWithDarkness.remove(uuid)) {
            removeDarkness(player);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[MinerDarkness] " + player.getName() + " sort de DARKNESS — y=" + player.getLocation().getBlockY());
        }
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