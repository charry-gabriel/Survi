package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;

/**
 * Réagit à {@link AlphaPlayerJobLevelUpEvent} pour produire les effets de passage de niveau :
 *
 * <ul>
 *   <li><b>Effets visuels</b> — title + subtitle affiché au joueur concerné</li>
 *   <li><b>Son</b>           — joué pour tous les joueurs en ligne</li>
 *   <li><b>Annonce</b>       — broadcast global en chat</li>
 *   <li><b>Attributs</b>     — mise à jour des attributs persistants du métier :
 *     <ul>
 *       <li>FISHERMAN → {@link fr.miuby.survi.job.FishermanAttributeService}
 *           ({@code PLAYER_UNDERWATER_MOVEMENT}, {@code PLAYER_OXYGEN_BONUS})</li>
 *       <li>EXPLORER  → {@link fr.miuby.survi.job.ExplorerAttributeService}
 *           ({@code SAFE_FALL_DISTANCE})</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class JobLevelUpListener implements Listener {

    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofMillis(2500),
            Duration.ofMillis(500)
    );

    @EventHandler
    public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        AlphaPlayer alphaPlayer = event.getAlphaPlayer();
        Player player = alphaPlayer.getPlayer();
        EJob job = event.getJob();
        int newLevel = event.getNewLevel();

        // ── Effets visuels — title affiché uniquement au joueur concerné ─────────
        if (player != null && player.isOnline()) {
            player.showTitle(Title.title(
                    Component.text("⚒ Niveau supérieur !", NamedTextColor.GOLD),
                    Component.text(job.getDisplayName(), job.getColor())
                            .append(Component.text(" · niv." + newLevel, NamedTextColor.YELLOW)),
                    TITLE_TIMES
            ));
        }

        // ── Annonce + son — tous les joueurs en ligne ─────────────────────────────
        Component broadcast = Component.text("⚒ ", NamedTextColor.GOLD)
                .append(Component.text(alphaPlayer.getPseudo(), NamedTextColor.WHITE))
                .append(Component.text(" a atteint le niveau ", NamedTextColor.GOLD))
                .append(Component.text("niv." + newLevel, NamedTextColor.YELLOW))
                .append(Component.text(" en ", NamedTextColor.GOLD))
                .append(job.toComponent())
                .append(Component.text(" !", NamedTextColor.GOLD));

        SoundService.broadcast(ESound.JOB_LEVEL_UP);
        Bukkit.broadcast(broadcast);

        // ── Mise à jour des attributs persistants par métier ─────────────────────
        if (player == null || !player.isOnline()) return;

        switch (job) {
            case FISHERMAN -> GameManager.getInstance().getAlphaPlayerFactory()
                    .getFishermanAttributeService().applyAttributes(alphaPlayer);
            case EXPLORER  -> GameManager.getInstance().getAlphaPlayerFactory()
                    .getExplorerAttributeService().applyAttributes(alphaPlayer);
            default        -> { /* pas d'attribut persistant pour ce métier */ }
        }
    }
}