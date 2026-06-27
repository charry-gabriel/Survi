package fr.miuby.survi.job.alchemic;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.LangService;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suit l'état actif des potions inédites à suivi programmatique :
 *
 * <ul>
 *   <li><b>Bouclier</b>  — absorbe 1 coup, puis expire automatiquement après 60 s.</li>
 *   <li><b>Symbiose</b>  — horodatage d'expiration (30 s), bloque les attaques sortantes.</li>
 *   <li><b>Fissure</b>   — applique la fatigue (1 min) après 8 s de haste extrême.</li>
 * </ul>
 *
 * <p>Toutes les messages joueur passent par {@link LangService}.</p>
 */
public final class CustomPotionManager {

    // ─── Bouclier ─────────────────────────────────────────────────────────────

    /** {@code true} si le joueur a un Bouclier actif. */
    private final Map<UUID, Boolean>    shieldActive = new HashMap<>();
    /** Tâches d'expiration automatique du Bouclier (60 s sans coup reçu). */
    private final Map<UUID, BukkitTask> shieldTasks  = new HashMap<>();

    // ─── Symbiose ─────────────────────────────────────────────────────────────

    /** Timestamp d'expiration de la Symbiose (millis système). */
    private final Map<UUID, Long>       symbiosisExpiry = new HashMap<>();
    /** Tâches de fin de Symbiose (message d'expiration). */
    private final Map<UUID, BukkitTask> symbiosisTasks  = new HashMap<>();

    // ─── Fissure ──────────────────────────────────────────────────────────────

    /** Tâches de recoil (applique Fatigue IV après 8 s de Haste XV). */
    private final Map<UUID, BukkitTask> fissureTasks = new HashMap<>();

    // ─── BOUCLIER ─────────────────────────────────────────────────────────────

    /** Active le Bouclier Éphémère : absorbe le prochain coup, expire après 60 s. */
    public void applyBouclier(Player player) {
        UUID uuid = player.getUniqueId();
        cancelAndRemove(shieldTasks, uuid);
        shieldActive.put(uuid, true);

        LangService ls = GameManager.getInstance().getLangService();
        player.sendMessage(ls.text(player, "job.fisherman.potion.bouclier.active"));

        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            shieldTasks.remove(uuid);
            if (shieldActive.remove(uuid) != null && player.isOnline())
                player.sendMessage(ls.text(player, "job.fisherman.potion.bouclier.broken"));
        }, 1200L); // 60 s
        shieldTasks.put(uuid, task);
    }

    /**
     * Tente d'absorber un coup avec le Bouclier.
     * @return {@code true} si le coup a été absorbé.
     */
    public boolean consumeShieldHit(Player player) {
        UUID uuid = player.getUniqueId();
        if (!shieldActive.containsKey(uuid)) return false;

        shieldActive.remove(uuid);
        cancelAndRemove(shieldTasks, uuid);

        LangService ls = GameManager.getInstance().getLangService();
        player.sendMessage(ls.text(player, "job.fisherman.potion.bouclier.hit"));
        return true;
    }

    // ─── SYMBIOSE ─────────────────────────────────────────────────────────────

    /** Active la Symbiose : Régénération I 30 s + blocage des attaques sortantes. */
    public void applySymbiose(Player player) {
        UUID uuid = player.getUniqueId();
        cancelAndRemove(symbiosisTasks, uuid);

        symbiosisExpiry.put(uuid, System.currentTimeMillis() + 30_000L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0, false, true));

        LangService ls = GameManager.getInstance().getLangService();
        player.sendMessage(ls.text(player, "job.fisherman.potion.symbiose.active"));

        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            symbiosisTasks.remove(uuid);
            symbiosisExpiry.remove(uuid);
            if (player.isOnline())
                player.sendMessage(ls.text(player, "job.fisherman.potion.symbiose.expired"));
        }, 600L);
        symbiosisTasks.put(uuid, task);
    }

    /** @return {@code true} si le joueur est actuellement sous l'effet Symbiose. */
    public boolean isSymbiosisActive(UUID uuid) {
        Long expiry = symbiosisExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            symbiosisExpiry.remove(uuid);
            return false;
        }
        return true;
    }

    // ─── FISSURE ──────────────────────────────────────────────────────────────

    /**
     * Active la Fissure : Haste XV pendant 8 s,
     * puis Fatigue IV pendant <b>1 minute</b> (recoil planifié).
     */
    public void applyFissure(Player player) {
        UUID uuid = player.getUniqueId();
        cancelAndRemove(fissureTasks, uuid);

        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 160, 14, false, true)); // Haste XV, 8 s

        LangService ls = GameManager.getInstance().getLangService();
        player.sendMessage(ls.text(player, "job.fisherman.potion.fissure.active"));

        var plugin = GameManager.getInstance().getPlugin();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            fissureTasks.remove(uuid);
            if (!player.isOnline()) return;
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 1200, 3, false, true)); // Fatigue IV, 1 min
            player.sendMessage(ls.text(player, "job.fisherman.potion.fissure.recoil"));
        }, 160L); // déclenché après 8 s
        fissureTasks.put(uuid, task);
    }

    // ─── Nettoyage ────────────────────────────────────────────────────────────

    /** Libère toutes les ressources d'un joueur (déconnexion, etc.). */
    public void cleanup(UUID uuid) {
        shieldActive.remove(uuid);
        cancelAndRemove(shieldTasks, uuid);
        symbiosisExpiry.remove(uuid);
        cancelAndRemove(symbiosisTasks, uuid);
        cancelAndRemove(fissureTasks, uuid);
    }

    private static void cancelAndRemove(Map<UUID, BukkitTask> map, UUID uuid) {
        BukkitTask t = map.remove(uuid);
        if (t != null) t.cancel();
    }
}
