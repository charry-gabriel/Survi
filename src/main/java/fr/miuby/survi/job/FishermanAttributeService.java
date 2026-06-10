package fr.miuby.survi.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

/**
 * Gère les attributs persistants du métier {@link EJob#FISHERMAN} :
 * <ul>
 *   <li>{@link Attribute#WATER_MOVEMENT_EFFICIENCY}  — vitesse de nage sous l'eau, calibrée par niveau.</li>
 *   <li>{@link Attribute#OXYGEN_BONUS}               — bonus de temps de respiration en ticks, calibré par niveau.</li>
 *   <li>{@link Attribute#SUBMERGED_MINING_SPEED}     — vitesse de minage sous l'eau, calibrée par niveau.</li>
 * </ul>
 *
 * <p>Ces modificateurs sont <em>transients</em> : ils disparaissent à la déconnexion et sont rétablis
 * à la reconnexion via {@link fr.miuby.survi.player.AlphaPlayerFactory#onPlayerJoin},
 * puis mis à jour à chaque montée de niveau via
 * {@link fr.miuby.survi.listener.JobLevelUpListener}.</p>
 *
 * <p>Les anciens effets de potion (DOLPHINS_GRACE, WATER_BREATHING, HASTE) sont entièrement
 * remplacés par ces attributs — voir {@code jobs/fisherman.yml}.</p>
 */
public final class FishermanAttributeService {

    private static final String KEY_SPEED  = "fisherman_water_movement";
    private static final String KEY_OXYGEN = "fisherman_oxygen_bonus";
    private static final String KEY_MINING = "fisherman_submerged_mining";

    /**
     * Applique (ou met à jour) les trois modificateurs en fonction du niveau FISHERMAN actuel.
     * Si la valeur configurée est 0, le modificateur existant est retiré sans en ajouter un nouveau.
     */
    public void applyAttributes(AlphaPlayer ap) {
        if (ap.getPlayer() == null) return;
        int level = ap.getJobLevel(EJob.FISHERMAN);
        JobsConfig.FishermanCfg cfg = JobsConfig.getInstance().getFisherman();

        applyModifier(ap, Attribute.WATER_MOVEMENT_EFFICIENCY, KEY_SPEED, cfg.getUnderwaterSpeedModifier()[level]);
        applyModifier(ap, Attribute.OXYGEN_BONUS, KEY_OXYGEN, cfg.getOxygenBonusTicks()[level]);
        applyModifier(ap, Attribute.SUBMERGED_MINING_SPEED, KEY_MINING, cfg.getSubmergedMiningSpeedModifier()[level]);
    }

    /** Retire les trois modificateurs (ex : reset admin). */
    public void removeAttributes(AlphaPlayer ap) {
        if (ap.getPlayer() == null) return;
        removeModifier(ap, Attribute.WATER_MOVEMENT_EFFICIENCY, KEY_SPEED);
        removeModifier(ap, Attribute.OXYGEN_BONUS, KEY_OXYGEN);
        removeModifier(ap, Attribute.SUBMERGED_MINING_SPEED, KEY_MINING);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static void applyModifier(AlphaPlayer ap, Attribute attribute, String keyName, double value) {
        AttributeInstance instance = ap.getPlayer().getAttribute(attribute);
        if (instance == null) return;
        NamespacedKey key = new NamespacedKey(GameManager.getInstance().getPlugin(), keyName);
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) instance.removeModifier(existing);
        if (value != 0.0) instance.addTransientModifier(new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER));
    }

    private static void removeModifier(AlphaPlayer ap, Attribute attribute, String keyName) {
        AttributeInstance instance = ap.getPlayer().getAttribute(attribute);
        if (instance == null) return;
        NamespacedKey key = new NamespacedKey(GameManager.getInstance().getPlugin(), keyName);
        AttributeModifier mod = instance.getModifier(key);
        if (mod != null) instance.removeModifier(mod);
    }
}