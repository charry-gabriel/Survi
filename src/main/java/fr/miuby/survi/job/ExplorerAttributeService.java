package fr.miuby.survi.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

/**
 * Gère l'attribut persistant du métier {@link EJob#EXPLORER} :
 * <ul>
 *   <li>{@link Attribute#SAFE_FALL_DISTANCE} — distance de chute sécurisée, calibrée par niveau.</li>
 * </ul>
 *
 * <p>Le modificateur ADD_NUMBER appliqué vaut {@code safeFallDistance[level] − 3.0}
 * (la base vanilla est 3 blocs). Les dégâts de chute et Feather Falling sont
 * entièrement délégués au moteur vanilla.</p>
 *
 * <p>Le modificateur est <em>transient</em> : il disparaît à la déconnexion et est rétabli
 * à la reconnexion via {@link fr.miuby.survi.player.AlphaPlayerFactory#onPlayerJoin},
 * puis mis à jour à chaque montée de niveau via
 * {@link fr.miuby.survi.listener.JobLevelUpListener}.</p>
 */
public final class ExplorerAttributeService {

    private static final double VANILLA_SAFE_FALL = 3.0;
    private static final String KEY = "explorer_safe_fall_distance";

    /**
     * Applique (ou met à jour) le modificateur {@code SAFE_FALL_DISTANCE}
     * en fonction du niveau EXPLORER actuel du joueur.
     */
    public void applyAttributes(AlphaPlayer ap) {
        if (ap.getPlayer() == null) return;
        int level = ap.getJobLevel(EJob.EXPLORER);
        double target = JobsConfig.getInstance().getExplorer().getSafeFallDistance()[level];
        applyModifier(ap, target - VANILLA_SAFE_FALL);
    }

    /** Retire le modificateur (ex : reset admin). */
    public void removeAttributes(AlphaPlayer ap) {
        if (ap.getPlayer() == null) return;
        AttributeInstance attr = ap.getPlayer().getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (attr == null) return;
        NamespacedKey key = new NamespacedKey(GameManager.getInstance().getPlugin(), KEY);
        AttributeModifier mod = attr.getModifier(key);
        if (mod != null) attr.removeModifier(mod);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private static void applyModifier(AlphaPlayer ap, double delta) {
        AttributeInstance attr = ap.getPlayer().getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (attr == null) return;
        NamespacedKey key = new NamespacedKey(GameManager.getInstance().getPlugin(), KEY);
        AttributeModifier existing = attr.getModifier(key);
        if (existing != null) attr.removeModifier(existing);
        attr.addTransientModifier(new AttributeModifier(key, delta, AttributeModifier.Operation.ADD_NUMBER));
    }
}