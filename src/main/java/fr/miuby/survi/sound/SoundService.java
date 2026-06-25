package fr.miuby.survi.sound;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Service statique pour jouer des sons aux joueurs.
 *
 * <p>Les sons sont toujours joués <em>pour</em> le joueur (mode 2D, non positionnel) —
 * ils ne sont pas émis dans le monde et ne dépendent pas de la position du joueur.
 *
 * <p>Toutes les clés et valeurs par défaut sont définies dans {@link ESound}.
 *
 * <pre>{@code
 * // Son individuel
 * SoundService.play(player, ESound.QUEST_COMPLETE);
 *
 * // Son individuel avec hauteur personnalisée
 * SoundService.play(player, ESound.CLICK, 1.5f);
 *
 * // Broadcast à tous les joueurs en ligne
 * SoundService.broadcast(ESound.JOB_LEVEL_UP);
 *
 * // Stopper un son
 * SoundService.stop(player, ESound.QUEST_COMPLETE);
 * }</pre>
 */
public final class SoundService {

    private SoundService() {}

    // ─── Joueur individuel ────────────────────────────────────────────────────

    /** Joue un son au joueur avec le volume et la hauteur définis dans {@link ESound}. */
    public static void play(Player player, ESound sound) {
        player.playSound(sound.getSound());
    }

    /** Joue un son au joueur avec une hauteur personnalisée. */
    public static void play(Player player, ESound sound, float pitch) {
        Sound s = sound.getSound();
        player.playSound(Sound.sound(s.name(), s.source(), s.volume(), pitch));
    }

    /** Joue un son au joueur avec un volume et une hauteur personnalisés. */
    public static void play(Player player, ESound sound, float volume, float pitch) {
        Sound s = sound.getSound();
        player.playSound(Sound.sound(s.name(), s.source(), volume, pitch));
    }

    // ─── Broadcast ───────────────────────────────────────────────────────────

    /** Joue un son à tous les joueurs en ligne. */
    public static void broadcast(ESound sound) {
        Sound s = sound.getSound();
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(s);
    }

    /** Joue un son à tous les joueurs en ligne avec une hauteur personnalisée. */
    public static void broadcast(ESound sound, float pitch) {
        Sound s = sound.getSound();
        Sound custom = Sound.sound(s.name(), s.source(), s.volume(), pitch);
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(custom);
    }

    /** Joue {@code sound} à tous les joueurs en ligne sauf {@code excluded}. */
    public static void broadcastExcept(Player excluded, ESound sound) {
        Sound s = sound.getSound();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(excluded.getUniqueId())) p.playSound(s);
        }
    }

    // ─── Stop ────────────────────────────────────────────────────────────────

    /** Stoppe un son spécifique pour le joueur. */
    public static void stop(Player player, ESound sound) {
        player.stopSound(SoundStop.named(sound.getSound().name()));
    }

    /** Stoppe tous les sons pour le joueur. */
    public static void stopAll(Player player) {
        player.stopAllSounds();
    }
}