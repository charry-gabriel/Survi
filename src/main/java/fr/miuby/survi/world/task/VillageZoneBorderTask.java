package fr.miuby.survi.world.task;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.VillageZoneManager;
import fr.miuby.survi.world.WorldInitializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class VillageZoneBorderTask extends BukkitRunnable {

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
        VillageZoneManager vzm = GameManager.getInstance().getVillageZoneManager();
        if (!vzm.isStarted()) return;

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (villageName == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(villageName)) continue;
            renderForPlayer(player, vzm);
        }
    }

    private void renderForPlayer(Player player, VillageZoneManager vzm) {
        Location loc = player.getLocation();
        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();
        World world = loc.getWorld();

        double borderW = vzm.getCurrentCenterX() - vzm.getCurrentHalfWidth();
        double borderE = vzm.getCurrentCenterX() + vzm.getCurrentHalfWidth();
        double borderN = vzm.getCurrentCenterZ() - vzm.getCurrentHalfDepth();
        double borderS = vzm.getCurrentCenterZ() + vzm.getCurrentHalfDepth();

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
        String key = isClose ? "boundary.close" : "boundary.approaching";
        player.sendActionBar(GameManager.getInstance().getLangService().text(player, key, meters));
    }

    /** @param isEW true = bord E/W (axe variable Z), false = bord N/S (axe variable X) */
    private void spawnWall(World world, Player player, double fixed, double playerY, double playerAlong, double segMin, double segMax, boolean isEW, Particle.DustOptions dust) {
        double from = Math.max(segMin, playerAlong - LATERAL_RANGE);
        double to   = Math.min(segMax, playerAlong + LATERAL_RANGE);

        for (double along = from; along <= to; along += SPACING) {
            for (double h = playerY - HEIGHT_BELOW; h <= playerY + HEIGHT_ABOVE; h += SPACING) {
                player.spawnParticle(Particle.DUST, isEW ? fixed : along, h, isEW ? along : fixed, 1, 0.0, 0.0, 0.0, 0.0, dust);
            }
        }
    }
}