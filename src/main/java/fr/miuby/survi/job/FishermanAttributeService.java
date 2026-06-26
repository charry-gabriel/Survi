package fr.miuby.survi.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Gère les attributs persistants du métier {@link EJob#FISHERMAN} :
 * <ul>
 *   <li>{@link Attribute#WATER_MOVEMENT_EFFICIENCY}  — vitesse de nage sous l'eau, calibrée par niveau.</li>
 *   <li>{@link Attribute#OXYGEN_BONUS}               — bonus de temps de respiration en ticks, calibré par niveau.</li>
 *   <li>{@link Attribute#SUBMERGED_MINING_SPEED}     — vitesse de minage sous l'eau, calibrée par niveau.</li>
 * </ul>
 *
 * <p>Ces trois bonus nécessitent désormais que le joueur porte un pantalon
 * ({@code GROWTH_FISHERMAN_LEGGINGS}) ayant débloqué l'ability {@link GrowthItems#ABILITY_UNDERWATER_KIT}
 * (palier de croissance — voir {@code growth_items/growth_fisherman_leggings.yml}). La <b>magnitude</b>
 * de chaque bonus reste pilotée par le niveau du job dans {@link JobsConfig} : l'item débloque la
 * capacité, le job en augmente la portée — même principe que le tree feller du Bûcheron.</p>
 *
 * <p>Ces modificateurs sont <em>transients</em> : ils disparaissent à la déconnexion et sont rétablis
 * à la reconnexion via {@link fr.miuby.survi.player.AlphaPlayerFactory#onPlayerJoin},
 * mis à jour à chaque montée de niveau via {@link fr.miuby.survi.listener.JobLevelUpListener},
 * à chaque changement de jambières via {@link fr.miuby.survi.listener.job.FishermanListener},
 * et resynchronisés périodiquement (toutes les 3 secondes) par
 * {@link fr.miuby.survi.job.task.FishermanEffectsTask} (couvre le cas d'un item qui débloque
 * l'ability en cours de partie alors qu'il est déjà équipé).</p>
 *
 * <p>Les anciens effets de potion (DOLPHINS_GRACE, WATER_BREATHING, HASTE) sont entièrement
 * remplacés par ces attributs — voir {@code jobs/fisherman.yml}.</p>
 */
public final class FishermanAttributeService {

    private static final String KEY_SPEED  = "fisherman_water_movement";
    private static final String KEY_OXYGEN = "fisherman_oxygen_bonus";
    private static final String KEY_MINING = "fisherman_submerged_mining";

    /**
     * Applique (ou met à jour) les trois modificateurs en fonction du niveau FISHERMAN actuel —
     * seulement si {@code GROWTH_FISHERMAN_LEGGINGS} est porté et a débloqué
     * {@link GrowthItems#ABILITY_UNDERWATER_KIT}. Sinon, retire les modificateurs existants
     * (équivalent à {@link #removeAttributes}).
     *
     * <p>Si la valeur configurée pour un attribut donné est 0, le modificateur existant est retiré
     * sans en ajouter un nouveau (même quand l'ability est débloquée).</p>
     */
    public void applyAttributes(AlphaPlayer ap) {
        if (ap.getPlayer() == null) return;

        if (!GrowthItems.hasAbilityEquipped(ap.getPlayer(), GrowthItems.ABILITY_UNDERWATER_KIT, EquipmentSlot.LEGS)) {
            removeAttributes(ap);
            return;
        }

        int level = ap.getJobLevel(EJob.FISHERMAN);
        JobsConfig.FishermanCfg cfg = JobsConfig.getInstance().getFisherman();

        applyModifier(ap, Attribute.WATER_MOVEMENT_EFFICIENCY, KEY_SPEED, cfg.getUnderwaterSpeedModifier()[level]);
        applyModifier(ap, Attribute.OXYGEN_BONUS, KEY_OXYGEN, cfg.getOxygenBonus()[level]);
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