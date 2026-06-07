package fr.miuby.survi.sound;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Registre centralisé de tous les sons du plugin.
 *
 * <p>Chaque entrée définit la clé Minecraft, la source audio, le volume et la hauteur par défaut.
 * Pour jouer un son, utiliser {@link SoundService}.
 *
 * <pre>{@code
 * SoundService.play(player, ESound.QUEST_COMPLETE);
 * SoundService.broadcast(ESound.JOB_LEVEL_UP);
 * SoundService.play(player, ESound.CLICK, 1.5f); // pitch custom
 * }</pre>
 */
public enum ESound {

    // ─── Quêtes ──────────────────────────────────────────────────────────────
    QUEST_COMPLETE(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f),
    GLOBAL_QUEST_START(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 0.8f),
    GLOBAL_QUEST_COMPLETE(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.2f),

    // ─── Niveaux ─────────────────────────────────────────────────────────────
    JOB_LEVEL_UP(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f),
    WORLD_LEVEL_UP(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 0.8f),
    VILLAGER_LEVEL_UP(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1.1f),

    // ─── Combat ──────────────────────────────────────────────────────────────
    FEE_DAMAGE(Key.key("entity.slime.attack"), Sound.Source.MASTER, 1f, 1.1f),

    // ─── Interface ───────────────────────────────────────────────────────────
    CLICK(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.0f),
    ERROR(Key.key("entity.villager.no"), Sound.Source.MASTER, 1.0f, 1.0f),
    COIN(Key.key("entity.experience_orb.pickup"), Sound.Source.MASTER, 1.0f, 1.2f),
    UNLOCK(Key.key("block.note_block.chime"), Sound.Source.MASTER, 0.8f, 1.5f),
    ;

    @Getter private final Sound sound;

    ESound(Key key, Sound.Source source, float volume, float pitch) {
        this.sound = Sound.sound(key, source, volume, pitch);
    }
}