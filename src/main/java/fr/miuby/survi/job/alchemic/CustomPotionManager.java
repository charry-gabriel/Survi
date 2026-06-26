package fr.miuby.survi.job.alchemic;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suit l'état actif des potions inédites qui nécessitent un suivi programmatique :
 * <ul>
 *   <li><b>Bouclier</b>  — compteur de coups restants + tâche d'expiration.</li>
 *   <li><b>Symbiose</b>  — horodatage d'expiration (30 s).</li>
 *   <li><b>Fissure</b>   — tâche planifiée pour l'application du recoil (Fatigue).</li>
 * </ul>
 *
 * <p>Initialisé dans {@link fr.miuby.survi.GameManager#initAfterWorldsLoad()}.
 * Nettoyé à la déconnexion via {@link #cleanup(UUID)}.</p>
 */
public final class CustomPotionManager {

    /** Hits restants du Bouclier par joueur. */
    private final Map<UUID, Integer>    shieldHits  = new HashMap<>();
    /** Tâches d'expiration du Bouclier (60 s sans hit). */
    private final Map<UUID, BukkitTask> shieldTasks = new HashMap<>();

    /** Timestamp d'expiration de la Symbiose (millis). */
    private final Map<UUID, Long>       symbiosisExpiry = new HashMap<>();
    /** Tâches de fin de Symbiose (pour envoyer le message). */
    private final Map<UUID, BukkitTask> symbiosisTasks  = new HashMap<>();

    /** Tâches de recoil de la Fissure (applique Fatigue après 8 s). */
    private final Map<UUID, BukkitTask> fissureTasks = new HashMap<>();

    // ─── BOUCLIER ────────────────────────────────────────────────────────────────

    /**
     * Applique le Bouclier : 3 coups absorbés, expire automatiquement après 60 s.
     * Si un Bouclier est déjà actif, il est réinitialisé.
     */
    public void applyBouclier(Player player) {
        UUID uuid = player.getUniqueId();

        // Annuler l'ancienne tâche si déjà actif
        BukkitTask old = shieldTasks.remove(uuid);
        if (old != null) old.cancel();

        shieldHits.put(uuid, 3);

        player.sendMessage(Component.text(
                "✦ Bouclier Éphémère activé ! Vos 3 prochains coups seront absorbés.",
                NamedTextColor.BLUE));

        // Expiration automatique après 60 s sans hit
        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            shieldTasks.remove(uuid);
            if (shieldHits.remove(uuid) != null && player.isOnline())
                player.sendMessage(Component.text("✦ Votre Bouclier Éphémère s'est dissipé.", NamedTextColor.GRAY));
        }, 1200L); // 60 s
        shieldTasks.put(uuid, task);
    }

    /**
     * Tente d'absorber un coup avec le Bouclier.
     *
     * @return {@code true} si le coup a été absorbé (bouclier actif), {@code false} sinon.
     */
    public boolean consumeShieldHit(Player player) {
        UUID uuid = player.getUniqueId();
        Integer remaining = shieldHits.get(uuid);
        if (remaining == null) return false;

        int next = remaining - 1;
        if (next <= 0) {
            shieldHits.remove(uuid);
            BukkitTask t = shieldTasks.remove(uuid);
            if (t != null) t.cancel();
            player.sendMessage(Component.text("✦ Votre Bouclier Éphémère s'est brisé !", NamedTextColor.GRAY));
        } else {
            shieldHits.put(uuid, next);
            player.sendActionBar(Component.text(
                    "✦ Bouclier absorbé — " + next + " coup(s) restant(s)", NamedTextColor.BLUE));
        }
        return true;
    }

    // ─── SYMBIOSE ────────────────────────────────────────────────────────────────

    /**
     * Applique la Symbiose : régénération I pendant 30 s + blocage des attaques sortantes.
     */
    public void applySymbiose(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask old = symbiosisTasks.remove(uuid);
        if (old != null) old.cancel();

        long expiry = System.currentTimeMillis() + 30_000L;
        symbiosisExpiry.put(uuid, expiry);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0, false, true));
        player.sendMessage(Component.text(
                "✦ Symbiose active 30 s — régénération, mais vous ne pouvez plus attaquer !",
                NamedTextColor.LIGHT_PURPLE));

        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            symbiosisTasks.remove(uuid);
            symbiosisExpiry.remove(uuid);
            if (player.isOnline())
                player.sendMessage(Component.text("✦ La Symbiose s'est terminée.", NamedTextColor.GRAY));
        }, 600L); // 30 s
        symbiosisTasks.put(uuid, task);
    }

    /** Retourne {@code true} si le joueur est actuellement sous Symbiose. */
    public boolean isSymbiosisActive(UUID uuid) {
        Long expiry = symbiosisExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            symbiosisExpiry.remove(uuid);
            return false;
        }
        return true;
    }

    // ─── FISSURE ─────────────────────────────────────────────────────────────────

    /**
     * Applique la Fissure : Haste XV pendant 8 s,
     * puis Fatigue IV pendant 2 minutes (recoil planifié).
     */
    public void applyFissure(Player player) {
        UUID uuid = player.getUniqueId();

        // Annuler un recoil en attente si la potion est reprise avant la fin
        BukkitTask old = fissureTasks.remove(uuid);
        if (old != null) old.cancel();

        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 160, 14, false, true)); // Haste XV, 8 s
        player.sendMessage(Component.text(
                "✦ Fissure ! Vos mains déchirent la roche… mais à quel prix !",
                NamedTextColor.GOLD));

        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            fissureTasks.remove(uuid);
            if (!player.isOnline()) return;
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 2400, 3, false, true)); // Fatigue IV, 2 min
            player.sendMessage(Component.text(
                    "✦ La Fissure vous épuise ! Vous ne pouvez plus miner pendant 2 minutes.",
                    NamedTextColor.DARK_RED));
        }, 160L); // 8 s
        fissureTasks.put(uuid, task);
    }

    // ─── Nettoyage ───────────────────────────────────────────────────────────────

    /**
     * Libère toutes les ressources d'un joueur (déconnexion, mort…).
     * Annule les tâches planifiées et supprime les entrées des maps.
     */
    public void cleanup(UUID uuid) {
        shieldHits.remove(uuid);
        cancelAndRemove(shieldTasks, uuid);
        symbiosisExpiry.remove(uuid);
        cancelAndRemove(symbiosisTasks, uuid);
        cancelAndRemove(fissureTasks, uuid);
    }

    private static void cancelAndRemove(Map<UUID, BukkitTask> map, UUID uuid) {
        BukkitTask task = map.remove(uuid);
        if (task != null) task.cancel();
    }
}
