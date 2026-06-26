package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.alchemic.CustomPotionManager;
import fr.miuby.survi.job.alchemic.ECustomPotion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * Interception des potions inédites du Pêcheur :
 *
 * <ul>
 *   <li>{@link PlayerItemConsumeEvent} — BOUCLIER, SYMBIOSE, FISSURE (potions buvables).</li>
 *   <li>{@link PotionSplashEvent} — DEFLAGRATION, MIASME (splash).</li>
 *   <li>{@link EntityDamageByEntityEvent} — absorption Bouclier + blocage attaque Symbiose.</li>
 *   <li>{@link PlayerQuitEvent} — nettoyage du {@link CustomPotionManager}.</li>
 * </ul>
 */
public final class AlchemicPotionListener implements Listener {

    // ─── Potions buvables ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrinkCustomPotion(PlayerItemConsumeEvent event) {
        ECustomPotion potion = ECustomPotion.fromItem(event.getItem());
        if (potion == null) return;

        Player player = event.getPlayer();
        CustomPotionManager mgr = GameManager.getInstance().getCustomPotionManager();

        // Annuler la consommation vanilla (la potion n'a pas d'effet vanilla)
        // On laisse quand même l'animation se jouer — setCancelled(false) = normal

        switch (potion) {
            case BOUCLIER  -> mgr.applyBouclier(player);
            case SYMBIOSE  -> mgr.applySymbiose(player);
            case FISSURE   -> mgr.applyFissure(player);
            // DEFLAGRATION et MIASME sont des splash, gérés dans onSplash
            default        -> { /* rien */ }
        }
    }

    // ─── Splash ───────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        ThrownPotion thrown = event.getPotion();
        ECustomPotion potion = ECustomPotion.fromItem(thrown.getItem());
        if (potion == null) return;

        // Bloquer les effets vanilla (la potion n'en a pas, mais par sécurité)
        event.getAffectedEntities().forEach(e -> event.setIntensity(e, 0));

        Location loc = thrown.getLocation();
        World world   = thrown.getWorld();

        switch (potion) {
            case DEFLAGRATION -> applyDeflagration(loc, world, thrown.getShooter());
            case MIASME       -> applyMiasme(loc, world);
            default           -> { /* rien */ }
        }
    }

    // ─── DÉFLAGRATION ─────────────────────────────────────────────────────────────

    /**
     * Inflige 8 dégâts (4 cœurs) à tous les mobs dans un rayon de 6 blocs autour du point d'impact.
     * Utilise {@code entity.damage()} directement — pas de foudre, pas de transformation de creeper.
     * Un effet de particules et de son simule l'impact.
     */
    private static void applyDeflagration(Location loc, World world, Object shooter) {
        final double DAMAGE = 8.0;
        final double RADIUS = 6.0;

        Collection<Entity> nearby = world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS,
                e -> e instanceof LivingEntity && !(e instanceof Player));

        int count = 0;
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity le) {
                le.damage(DAMAGE);
                count++;
            }
        }

        // Effet visuel : explosion de particules sans dégâts de bloc
        world.createExplosion(loc, 0f, false, false); // power=0 → visuel pur, aucun dégât de bloc
        world.spawnParticle(org.bukkit.Particle.EXPLOSION, loc, 8, 1.5, 1.5, 1.5, 0.1);

        if (shooter instanceof Player player) {
            player.sendMessage(Component.text(
                    "✦ Déflagration ! " + count + " créature(s) touchée(s).",
                    NamedTextColor.GOLD));
        }
    }

    // ─── MIASME ───────────────────────────────────────────────────────────────────

    private static void applyMiasme(Location loc, World world) {
        AreaEffectCloud cloud = world.spawn(loc, AreaEffectCloud.class);
        cloud.setDuration(300);                      // 15 s
        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(0f);
        cloud.setRadiusPerTick(0f);                  // rayon constant
        cloud.setWaitTime(0);
        cloud.setColor(Color.fromRGB(0x2E8B00));     // vert sombre
        cloud.addCustomEffect(
                new PotionEffect(PotionEffectType.POISON, 100, 2, false, false), // Poison III
                true
        );
    }

    // ─── Bouclier & Symbiose (dégâts) ────────────────────────────────────────────

    /**
     * Bouclier : absorbe le prochain coup reçu.
     * Priorité LOW → on annule avant le calcul des dégâts normaux.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShieldAbsorb(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CustomPotionManager mgr = GameManager.getInstance().getCustomPotionManager();
        if (mgr.consumeShieldHit(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Symbiose : bloque toutes les attaques sortantes du joueur pendant la durée.
     * Priorité HIGHEST → s'assure que les dégâts restent à 0.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSymbiosisAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        CustomPotionManager mgr = GameManager.getInstance().getCustomPotionManager();
        if (!mgr.isSymbiosisActive(player.getUniqueId())) return;

        event.setCancelled(true);
        player.sendActionBar(Component.text(
                "✦ La Symbiose vous empêche d'attaquer.", NamedTextColor.LIGHT_PURPLE));
    }

    // ─── Nettoyage déconnexion ────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameManager.getInstance().getCustomPotionManager().cleanup(event.getPlayer().getUniqueId());
    }
}
