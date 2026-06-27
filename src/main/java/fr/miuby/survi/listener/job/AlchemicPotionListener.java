package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.alchemic.CustomPotionManager;
import fr.miuby.survi.job.alchemic.ECustomPotion;
import fr.miuby.survi.system.lang.LangService;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * Interception des 5 potions inédites du Pêcheur.
 *
 * <ul>
 *   <li>{@link PlayerItemConsumeEvent}       — BOUCLIER, SYMBIOSE, FISSURE.</li>
 *   <li>{@link PotionSplashEvent}             — DEFLAGRATION (dégâts AOE), MIASME (nuage).</li>
 *   <li>{@link EntityDamageByEntityEvent}     — absorption Bouclier + blocage attaque Symbiose.</li>
 *   <li>{@link PlayerQuitEvent}               — nettoyage {@link CustomPotionManager}.</li>
 * </ul>
 *
 * <p>Tous les messages passent par {@link LangService}.</p>
 */
public final class AlchemicPotionListener implements Listener {

    // ─── Potions buvables ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrinkCustomPotion(PlayerItemConsumeEvent event) {
        ECustomPotion potion = ECustomPotion.fromItem(event.getItem());
        if (potion == null) return;

        Player player = event.getPlayer();
        CustomPotionManager mgr = GameManager.getInstance().getCustomPotionManager();

        switch (potion) {
            case BOUCLIER     -> mgr.applyBouclier(player);
            case SYMBIOSE     -> mgr.applySymbiose(player);
            case FISSURE      -> mgr.applyFissure(player);
            case DEFLAGRATION -> applyDeflagration(player.getLocation(), player.getWorld(), player);
            default           -> { /* MIASME est splash */ }
        }
    }

    // ─── Splash ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        ThrownPotion thrown = event.getPotion();
        ECustomPotion potion = ECustomPotion.fromItem(thrown.getItem());
        if (potion == null) return;

        // Bloquer les effets vanilla (la potion n'en a aucun, précaution)
        event.getAffectedEntities().forEach(e -> event.setIntensity(e, 0));

        Location loc = thrown.getLocation();
        World world   = thrown.getWorld();

        switch (potion) {
            case MIASME -> applyMiasme(loc, world, thrown.getShooter());
            default     -> { }
        }
    }

    // ─── DÉFLAGRATION — dégâts AOE directs ───────────────────────────────────

    /**
     * 8 dégâts directs (4 cœurs) sur tous les mobs dans un rayon de 6 blocs autour du joueur.
     * Déclenché quand le joueur boit la potion — pas de foudre, pas de transformation de mob.
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

        // Visuel d'impact sans dégâts de blocs
        world.createExplosion(loc, 0f, false, false);

        if (shooter instanceof Player player) {
            LangService ls = GameManager.getInstance().getLangService();
            player.sendMessage(ls.text(player, "job.fisherman.potion.deflagration.strike", count));
        }
    }

    // ─── MIASME — dégâts et ralentissement programmés ────────────────────────

    /**
     * Crée un nuage visuel pendant 15 s.
     * Une tâche périodique (toutes les 20 ticks = 1 s) gère manuellement :
     * <ul>
     *   <li><b>2.0 dégâts</b> directs via {@code entity.damage()} — valeur exacte, indépendante de l'armure.</li>
     *   <li><b>Lenteur I</b> appliquée 2 s, rafraîchie chaque seconde tant que l'entité reste dans la zone.</li>
     * </ul>
     * Aucun effet de potion vanilla dans l'AEC — contrôle total sur les valeurs.
     */
    private static void applyMiasme(Location loc, World world, Object shooter) {
        final double DAMAGE        = 2.0;   // dégâts par seconde (1 cœur)
        final double RADIUS        = 3.0;   // blocs
        final int    SLOW_AMP      = 0;     // Lenteur I (0 = I, 1 = II…)
        final int    SLOW_DURATION = 40;    // 2 s — se rafraîchit chaque tick de tâche
        final int    DURATION_SEC  = 15;    // durée totale du nuage en secondes

        // Nuage visuel uniquement (couleur verte, pas d'effets vanilla)
        AreaEffectCloud cloud = world.spawn(loc, AreaEffectCloud.class);
        cloud.setDuration(DURATION_SEC * 20);
        cloud.setRadius((float) RADIUS);
        cloud.setRadiusOnUse(0f);
        cloud.setRadiusPerTick(0f);
        cloud.setWaitTime(0);
        cloud.setColor(Color.fromRGB(0x2E8B00));

        // Tâche : 1 fois par seconde, s'annule quand le nuage expire ou est retiré
        var plugin = GameManager.getInstance().getPlugin();
        new BukkitRunnable() {
            int remaining = DURATION_SEC;

            @Override
            public void run() {
                if (remaining-- <= 0 || !cloud.isValid()) {
                    this.cancel();
                    return;
                }
                for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
                    if (!(entity instanceof LivingEntity le) || entity instanceof Player) continue;
                    le.damage(DAMAGE);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_DURATION, SLOW_AMP, false, false));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        if (shooter instanceof Player player) {
            LangService ls = GameManager.getInstance().getLangService();
            player.sendMessage(ls.text(player, "job.fisherman.potion.miasme.active"));
        }
    }

    // ─── Bouclier — absorption d'un coup ─────────────────────────────────────

    /**
     * Absorbe le prochain coup reçu si le Bouclier est actif.
     * HIGH : tourne après les handlers de combat standards (NORMAL) pour avoir le dernier mot.
     * setDamage(0) + setCancelled(true) : double filet en Paper 26.x où certains recalculs
     * de dégâts peuvent ignorer l'état cancelled et appliquer quand même les HP perdus.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShieldAbsorb(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!GameManager.getInstance().getCustomPotionManager().consumeShieldHit(player)) return;
        event.setDamage(0);
        event.setCancelled(true);
    }

    // ─── Symbiose — blocage des attaques sortantes ────────────────────────────

    /** Annule toute attaque sortante du joueur pendant la Symbiose. Priorité HIGHEST. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSymbiosisAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        CustomPotionManager mgr = GameManager.getInstance().getCustomPotionManager();
        if (!mgr.isSymbiosisActive(player.getUniqueId())) return;

        event.setCancelled(true);
        LangService ls = GameManager.getInstance().getLangService();
        player.sendActionBar(ls.text(player, "job.fisherman.potion.symbiose.blocked"));
    }

    // ─── Nettoyage déconnexion ────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameManager.getInstance().getCustomPotionManager().cleanup(event.getPlayer().getUniqueId());
    }
}