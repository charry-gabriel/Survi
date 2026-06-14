package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;

public class WorldResetManager {

    public WorldResetManager() {}

    // -------------------------------------------------------------------------
    // Vérification quotidienne
    // -------------------------------------------------------------------------

    public void checkAndPerformResets() {
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Vérification du reset des mondes...");

        int frequency = getResetFrequency();
        if (frequency <= 0) return;

        LocalDate lastReset = getLastResetDate();
        LocalDate today     = LocalDate.now();

        if (!lastReset.plusDays(frequency).isAfter(today)) {
            performReset();
        }
    }

    // -------------------------------------------------------------------------
    // Reset principal : Wilderness + Nether + End
    // -------------------------------------------------------------------------

    public void performReset() {
        String currentWildName   = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);
        String currentNetherName = WorldInitializer.getWorlds().get(EWorld.NETHER);
        String currentEndName    = WorldInitializer.getWorlds().get(EWorld.END);

        String newWildName   = WorldInitializer.generateNextWildernessName();
        String newNetherName = newWildName + "_nether";
        String newEndName    = newWildName + "_the_end";

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "Reset : " + currentWildName + " -> " + newWildName + " | " + currentNetherName + " -> " + newNetherName + " | " + currentEndName + " -> " + newEndName);

        World village      = WorldRegistry.get(EWorld.VILLAGE).getWorld();
        Location safeSpawn = village.getSpawnLocation();

        // 1. Téléporter tous les joueurs des 3 mondes vers Village
        teleportPlayersToVillage(currentWildName,   safeSpawn);
        teleportPlayersToVillage(currentNetherName, safeSpawn);
        teleportPlayersToVillage(currentEndName,    safeSpawn);

        // 2. Nettoyer les tombes des 3 mondes avant leur déchargement
        clearGravesForWorld(currentWildName);
        clearGravesForWorld(currentNetherName);
        clearGravesForWorld(currentEndName);

        // 3. Désenregistrer le portail Wilderness avant unload
        GameManager.getInstance().getWorldPortalManager().unregisterWildernessPortal(currentWildName);

        // 4. Unload sans sauvegarde
        unloadWorld(currentWildName);
        unloadWorld(currentNetherName);
        unloadWorld(currentEndName);

        // 5. Créer les 3 nouveaux mondes
        WorldInitializer.loadOrCreate(newWildName,   World.Environment.NORMAL);
        WorldInitializer.loadOrCreate(newNetherName, World.Environment.NETHER);
        WorldInitializer.loadOrCreate(newEndName,    World.Environment.THE_END);

        // 6. Mettre à jour WorldInitializer (mémoire + DB + WorldRegistry)
        WorldInitializer.updateWorldNames(newWildName);

        // 7. Post-init après génération des spawn chunks (5 s)
        Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
            setLastResetDate(LocalDate.now());
            buildWorldPortal(newWildName);

            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "Reset terminé. Mondes actifs : " + newWildName + ", " + newNetherName + ", " + newEndName);
        }, 20L * 5);
    }

    void buildWorldPortal(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        // Portail fixe en 0,0 — facile à retrouver
        int bx = 0;
        int bz = 0;
        int by = world.getHighestBlockYAt(bx, bz);

        // Plateforme
        for (int x = -1; x <= 2; x++)
            for (int z = -1; z <= 1; z++)
                world.getBlockAt(bx + x, by - 1, bz + z).setType(Material.STONE_BRICKS);

        // Cadre + intérieur
        for (int h = 0; h <= 4; h++) {
            for (int w = -1; w <= 2; w++) {
                boolean isBorder = (h == 0 || h == 4 || w == -1 || w == 2);
                Material mat = isBorder ? Material.STONE_BRICKS : Material.MAGENTA_STAINED_GLASS_PANE;
                world.getBlockAt(bx + w, by + h, bz).setType(mat);
            }
        }

        Location min = new Location(world, bx,     by + 1, bz);
        Location max = new Location(world, bx + 1, by + 3, bz);

        // Spawn juste à côté du portail (quelques blocs devant)
        int newZ = bz + 6;
        int newY = world.getHighestBlockYAt(bx, newZ);
        world.setSpawnLocation(new Location(world, bx, newY + 1, newZ));

        GameManager.getInstance().getWorldPortalManager().registerWildernessPortal(worldName, min, max);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Portail Wilderness construit dans " + worldName + " en 0,0 (Y=" + by + ")");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void clearGravesForWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        GameManager.getInstance().getGraveManager().clearGravesInWorld(world.getUID());
    }

    private void teleportPlayersToVillage(String worldName, Location destination) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        var ls = GameManager.getInstance().getLangService();
        for (var player : world.getPlayers()) {
            player.teleport(destination);
            player.sendMessage(ls.text(player, "world.reset_warning"));
        }
    }

    private void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Bukkit.unloadWorld(world, false);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Monde déchargé : " + worldName);
    }

    // -------------------------------------------------------------------------
    // DB
    // -------------------------------------------------------------------------

    public int getResetFrequency() {
        String val = GameManager.getInstance().getDatabase().system().getServerData("reset_freq");
        return val != null ? Integer.parseInt(val) : 0;
    }

    public void setResetFrequency(int days) {
        GameManager.getInstance().getDatabase().system().saveServerData("reset_freq", String.valueOf(days));
    }

    /**
     * Retourne le prochain {@link ZonedDateTime} auquel le reset Wilderness sera déclenché,
     * ou {@code null} si les resets automatiques sont désactivés ({@code reset_freq <= 0}).
     *
     * <p>Le reset a lieu à {@link SurviConfig#getDailyResetHour()} le jour où
     * {@code lastResetDate + frequency <= today}. Si ce moment est déjà passé,
     * la méthode retourne {@code ZonedDateTime.now(tz)} pour indiquer qu'un reset
     * est imminent / en attente.
     */
    public ZonedDateTime getNextWildernessResetTime(ZoneId tz) {
        int frequency = getResetFrequency();
        if (frequency <= 0) return null;

        LocalDate lastReset = getLastResetDate();
        LocalDate nextResetDate = lastReset.equals(LocalDate.MIN) ? LocalDate.now() : lastReset.plusDays(frequency);

        int resetHour = SurviConfig.getInstance().getDailyResetHour();
        ZonedDateTime nextReset = nextResetDate.atTime(resetHour, 0).atZone(tz);
        ZonedDateTime now = ZonedDateTime.now(tz);

        return now.isBefore(nextReset) ? nextReset : now;
    }

    public LocalDate getLastResetDate() {
        String val = GameManager.getInstance().getDatabase().system().getServerData("last_reset_date");
        return val != null ? LocalDate.parse(val) : LocalDate.MIN;
    }

    private void setLastResetDate(LocalDate date) {
        GameManager.getInstance().getDatabase().system().saveServerData("last_reset_date", date.toString());
    }
}