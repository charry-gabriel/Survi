package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.mob.EMobStat;
import fr.miuby.survi.mob.MobLevelManager;
import fr.miuby.survi.mob.MobPotionEffectConfig;
import fr.miuby.survi.mob.MobTypeConfig;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Random;

/**
 * Listener principal du système de niveaux de monstres.
 *
 * <h3>Responsabilités</h3>
 * <ul>
 *   <li>À l'apparition d'un mob : tire son niveau et applique toutes ses stats
 *       via {@link MobLevelManager#applyLevel(LivingEntity, int)}.</li>
 *   <li>À chaque attaque au corps-à-corps d'un mob scalé : applique les effets de potion
 *       configurés (poison, wither, cécité…) avec leurs probabilités et durées
 *       calculées depuis le niveau du mob.</li>
 *   <li>À chaque impact de flèche tirée par un mob scalé (Skeleton, Stray, Pillager…) :
 *       écrase les dégâts par la valeur {@code attack-damage} du niveau du tireur,
 *       car {@code Attribute.ATTACK_DAMAGE} ne s'applique pas nativement aux projectiles.</li>
 * </ul>
 *
 * <h3>Enregistrement</h3>
 * Enregistrer cette classe dans votre méthode {@code onEnable()} ou
 * {@code initAfterWorldsLoad()} comme les autres listeners :
 * <pre>
 *   plugin.getServer().getPluginManager()
 *       .registerEvents(new MobSpawnListener(), plugin);
 * </pre>
 */
public class MobSpawnListener implements Listener {

    private final Random random = new Random();

    // ─── Spawn ────────────────────────────────────────────────────────────────

    /**
     * Intercepte le spawn naturel (et forcé par spawn-eggs) pour appliquer
     * le niveau mob. Les spawns via {@code EntityType.CUSTOM} ou les entités
     * non-vivantes sont ignorés automatiquement.
     *
     * <p>Priorité HIGH pour laisser les autres plugins annuler l'event avant
     * qu'on modifie les attributs (économie de CPU).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        MobLevelManager mlm = GameManager.getInstance().getMobLevelManager();
        if (mlm == null) return;

        // On ne scale que les mobs qui ont une config active
        LivingEntity entity = event.getEntity();
        if (!mlm.isManaged(entity.getType())) return;

        // Seuls les spawns "naturels" sont scalés ; exclure les spawns de
        // commande /summon sauf si tu veux les inclure aussi.
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.COMMAND) return;

        int level = mlm.rollMobLevel();
        mlm.applyLevel(entity, level);
    }

    // ─── Attaque + effets de potion ────────────────────────────────────────────

    /**
     * Applique les effets de potion configurés quand un mob scalé attaque
     * une entité vivante (joueur, animal…).
     *
     * <p>Exemple : une araignée de niveau 50 a une chance élevée d'empoisonner
     * et une chance croissante d'infliger Wither.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMobAttack(EntityDamageByEntityEvent event) {
        // L'attaquant doit être un mob scalé
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        int storedLevel = GameManager.getInstance().getMobLevelManager().getStoredLevel(attacker);
        if (storedLevel < 0) return; // mob non scalé

        // La cible doit aussi être une entité vivante
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Récupère la config du type pour les effets de potion
        EntityType type = attacker.getType();
        MobTypeConfig typeConfig = GameManager.getInstance().getMobLevelManager().getConfig(type);
        if (typeConfig == null) return;

        List<MobPotionEffectConfig> potionEffects = typeConfig.getPotionEffects();
        if (potionEffects.isEmpty()) return;

        for (MobPotionEffectConfig pec : potionEffects) {
            double chance    = pec.computeChance(storedLevel);
            int    duration  = pec.computeDuration(storedLevel);
            int    amplifier = pec.computeAmplifier(storedLevel);

            if (duration <= 0) continue;                  // niveau trop bas
            if (random.nextDouble() > chance) continue;   // pas de bol ce coup-ci

            target.addPotionEffect(new PotionEffect(
                    pec.type(),
                    duration,
                    amplifier,
                    false,  // ambient = false (pas d'effet de particule ambient)
                    true,   // particles visibles
                    true    // icône dans l'inventaire
            ));
        }
    }

    // ─── Dégâts de flèches des mobs scalés ────────────────────────────────────

    /**
     * Écrase les dégâts d'une flèche tirée par un mob scalé avec la valeur
     * {@code attack-damage} calculée pour son niveau.
     *
     * <p><b>Pourquoi ?</b> L'attribut Bukkit {@code Attribute.ATTACK_DAMAGE} ne
     * s'applique pas aux projectiles en vanilla — le dégât d'une flèche est fixé
     * par l'IA du mob au moment du tir (base ≈ 2–5, indépendant de l'attribut).
     * Sans cette interception, un Skeleton niveau 30 ferait les mêmes dégâts
     * qu'un Skeleton niveau 1.</p>
     *
     * <p>Couvre tous les tirs avec {@link AbstractArrow} : flèche normale,
     * flèche trempée (Stray, Bogged) et carreau d'arbalète (Pillager).</p>
     *
     * <p>Ne touche pas aux flèches tirées par les joueurs.</p>
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        // Le projectile doit être une flèche (AbstractArrow couvre Arrow + SpectralArrow)
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;

        // Le tireur doit être un mob vivant (pas un joueur)
        if (!(arrow.getShooter() instanceof LivingEntity shooter)) return;
        if (shooter instanceof Player) return;

        MobLevelManager mlm = GameManager.getInstance().getMobLevelManager();
        int level = mlm.getStoredLevel(shooter);
        if (level < 0) return; // tireur non géré par le système

        MobTypeConfig typeConfig = mlm.getConfig(shooter.getType());
        if (typeConfig == null) return;

        double attackDamage = typeConfig.getStatValue(EMobStat.ATTACK_DAMAGE, level);
        if (attackDamage < 0) return; // stat non configurée pour ce mob

        event.setDamage(attackDamage);
    }
}