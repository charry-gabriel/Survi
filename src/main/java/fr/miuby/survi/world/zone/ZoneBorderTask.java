package fr.miuby.survi.world.zone;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.world.WorldType;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Level;

/**
 * Tâche périodique affichant les particules de bordure et l'actionbar de distance
 * pour tous les mondes avec une zone active.
 *
 * <ul>
 *   <li><b>Village</b> — zone globale, dimensions depuis {@link VillageZoneManager#getCurrentBounds()}.
 *       Clés lang : {@code boundary.village.approaching} / {@code boundary.village.close}.</li>
 *   <li><b>Wilderness / Nether</b> — zone par joueur, rayon selon niveau Explorateur.
 *       Clés lang : {@code boundary.wild.approaching} / {@code boundary.wild.close}.</li>
 * </ul>
 */
public class ZoneBorderTask extends BukkitRunnable {

    public static final long PERIOD_TICKS = 10L;

    private static final int    TRIGGER_DIST  = 25;
    private static final int    CLOSE_DIST    = 10;
    private static final int    LATERAL_RANGE = 12;
    private static final int    HEIGHT_ABOVE  = 3;
    private static final int    HEIGHT_BELOW  = 2;
    private static final double SPACING       = 1.5;

    private static final Particle.DustOptions DUST_ORANGE = new Particle.DustOptions(Color.fromRGB(255, 145, 0), 1.2f);
    private static final Particle.DustOptions DUST_RED    = new Particle.DustOptions(Color.fromRGB(210, 25, 25),  1.5f);

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                processPlayer(player);
            } catch (Exception e) {
                // Une erreur sur un joueur ne doit jamais annuler la task pour tout le monde
                // (Bukkit annule définitivement une repeating task sur exception non interceptée).
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                        "[ZoneBorderTask] Erreur de rendu pour " + player.getName() + " — joueur ignoré ce tick : " + e);
            }
        }
    }

    private void processPlayer(Player player) {
        MLWorld mlWorld = WorldRegistry.get(player.getWorld().getUID());
        if (mlWorld == null) return;

        WorldType type = mlWorld.getType();
        ZoneBounds bounds;
        String langPrefix;

        if (type == EWorld.VILLAGE) {
            VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
            if (!vzm.isStarted()) return;
            bounds = vzm.getCurrentBounds();
            langPrefix = "boundary.village";
        } else if (type == EWorld.WILDERNESS) {
            bounds = getExploreBounds(player, false);
            langPrefix = "boundary.wild";
        } else if (type == EWorld.NETHER) {
            bounds = getExploreBounds(player, true);
            langPrefix = "boundary.wild";
        } else {
            return;
        }

        if (bounds == null) return;
        renderForPlayer(player, bounds, langPrefix);
    }

    // ─── Bounds par joueur (Wilderness / Nether) ─────────────────────────────────

    /**
     * Calcule les bornes d'exploration du joueur selon son niveau Explorateur.
     *
     * @param player   le joueur cible
     * @param isNether {@code true} pour le Nether (rayon divisé par 8, convention Minecraft)
     * @return {@link ZoneBounds} centrée en 0,0 avec le rayon approprié, ou {@code null} si le joueur est inconnu
     */
    private ZoneBounds getExploreBounds(Player player, boolean isNether) {
        AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
        if (ap == null) return null;

        List<Integer> radii = SurviConfig.getInstance().getExploreWildernessRadius();
        int idx    = Math.min(ap.getJobLevel(EJob.EXPLORER), radii.size() - 1);
        int radius = radii.get(idx);
        if (isNether) radius = radius / 8;

        return new ZoneBounds(0, 0, radius, radius);
    }

    // ─── Rendu particules + actionbar ─────────────────────────────────────────────

    private void renderForPlayer(Player player, ZoneBounds bounds, String langPrefix) {
        Location loc = player.getLocation();
        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();
        World world = loc.getWorld();

        double borderW = bounds.minX();
        double borderE = bounds.maxX();
        double borderN = bounds.minZ();
        double borderS = bounds.maxZ();

        double distW = px - borderW;
        double distE = borderE - px;
        double distN = pz - borderN;
        double distS = borderS - pz;

        double nearest = Math.min(Math.min(distW, distE), Math.min(distN, distS));
        if (nearest > TRIGGER_DIST) return;

        boolean isClose = nearest <= CLOSE_DIST;
        Particle.DustOptions dust = isClose ? DUST_RED : DUST_ORANGE;

        if (distW <= TRIGGER_DIST) spawnWall(world, player, borderW, py, pz, borderN, borderS, true,  dust);
        if (distE <= TRIGGER_DIST) spawnWall(world, player, borderE, py, pz, borderN, borderS, true,  dust);
        if (distN <= TRIGGER_DIST) spawnWall(world, player, borderN, py, px, borderW, borderE, false, dust);
        if (distS <= TRIGGER_DIST) spawnWall(world, player, borderS, py, px, borderW, borderE, false, dust);

        int meters = Math.max(1, (int) Math.ceil(nearest));
        String key  = isClose ? langPrefix + ".close" : langPrefix + ".approaching";
        player.sendActionBar(GameManager.getInstance().getLangService().text(player, key, meters));
    }

    /**
     * Spawn un mur de particules le long d'un bord, centré sur la position du joueur.
     *
     * @param isEW {@code true} = bord Est/Ouest (axe variable Z), {@code false} = bord Nord/Sud (axe variable X)
     */
    private void spawnWall(World world, Player player, double fixed, double playerY,
                           double playerAlong, double segMin, double segMax,
                           boolean isEW, Particle.DustOptions dust) {
        double from = Math.max(segMin, playerAlong - LATERAL_RANGE);
        double to   = Math.min(segMax, playerAlong + LATERAL_RANGE);

        for (double along = from; along <= to; along += SPACING) {
            for (double h = playerY - HEIGHT_BELOW; h <= playerY + HEIGHT_ABOVE; h += SPACING) {
                player.spawnParticle(Particle.DUST,
                        isEW ? fixed : along, h, isEW ? along : fixed,
                        1, 0.0, 0.0, 0.0, 0.0, dust);
            }
        }
    }
}