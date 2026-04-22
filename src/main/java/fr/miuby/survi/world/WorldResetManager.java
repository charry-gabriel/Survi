package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.time.LocalDate;
import java.util.logging.Level;

public class WorldResetManager {

    private static WorldResetManager instance;

    private WorldResetManager() {}

    public static WorldResetManager getInstance() {
        if (instance == null) instance = new WorldResetManager();
        return instance;
    }

    // -------------------------------------------------------------------------
    // Vérification quotidienne
    // -------------------------------------------------------------------------

    public void checkAndPerformResets() {
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Vérification du reset des mondes...");

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

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                "Reset : " + currentWildName + " -> " + newWildName + " | " + currentNetherName + " -> " + newNetherName + " | " + currentEndName    + " -> " + newEndName);

        World village      = WorldRegistry.get(EWorld.VILLAGE).getWorld();
        Location safeSpawn = village.getSpawnLocation();

        // 1. Téléporter tous les joueurs des 3 mondes vers Village
        teleportPlayersToVillage(currentWildName,   safeSpawn);
        teleportPlayersToVillage(currentNetherName, safeSpawn);
        teleportPlayersToVillage(currentEndName,    safeSpawn);

        // 2. Désenregistrer le portail Wilderness avant unload
        WorldPortalManager.getInstance().unregisterWildernessPortal(currentWildName);

        // 3. Unload sans sauvegarde
        unloadWorld(currentWildName);
        unloadWorld(currentNetherName);
        unloadWorld(currentEndName);

        // 4. Créer les 3 nouveaux mondes
        WorldInitializer.loadOrCreate(newWildName,   World.Environment.NORMAL);
        WorldInitializer.loadOrCreate(newNetherName, World.Environment.NETHER);
        WorldInitializer.loadOrCreate(newEndName,    World.Environment.THE_END);

        // 5. Mettre à jour WorldInitializer (mémoire + DB + WorldRegistry)
        WorldInitializer.updateWorldNames(newWildName);

        // 6. Post-init après génération des spawn chunks (5 s)
        Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
            setLastResetDate(LocalDate.now());
            buildWorldPortal(newWildName);

            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                    "Reset terminé. Mondes actifs : " + newWildName + ", " + newNetherName + ", " + newEndName);
        }, 20L * 5);
    }

    // -------------------------------------------------------------------------
    // Portail Wilderness — cadre obsidienne 4L×5H, intérieur 2×3 en verre
    //
    // Vue de face (Z fixe, axe X = largeur — on entre par Nord/Sud) :
    //
    //   O O O O   h=4  toit
    //   O G G O   h=3  intérieur
    //   O G G O   h=2  intérieur
    //   O G G O   h=1  intérieur  ← min = (bx, by+1, bz), max = (bx+1, by+3, bz)
    //   O O O O   h=0  sol
    //   w: -1 0 1 2
    //
    // G = GLASS (verre, affiché en NETHER_PORTAL côté client par WorldPortalManager)
    // O = OBSIDIAN
    //
    // Axe du faux portail : Axis.X (min.Z == max.Z, détecté automatiquement).
    // -------------------------------------------------------------------------

    void buildWorldPortal(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location spawn = world.getSpawnLocation();
        int bx     = spawn.getBlockX() + 4;
        int bz     = spawn.getBlockZ() + 4;
        int groundY = world.getHighestBlockYAt(bx, bz);
        int by      = groundY; // h=0 (sol du cadre) est à groundY, intérieur commence à groundY+1

        // Plateforme obsidienne sous le portail
        for (int x = -1; x <= 2; x++)
            for (int z = -1; z <= 1; z++)
                world.getBlockAt(bx + x, by - 1, bz + z).setType(Material.STONE_BRICKS);

        // Cadre + intérieur
        // w va de -1 à 2 (largeur 4 blocs), intérieur à w=0 et w=1
        // h va de 0 à 4 (hauteur 5 blocs), intérieur à h=1, h=2, h=3
        for (int h = 0; h <= 4; h++) {
            for (int w = -1; w <= 2; w++) {
                boolean isBorder = (h == 0 || h == 4 || w == -1 || w == 2);
                Material mat = isBorder ? Material.STONE_BRICKS : Material.GLASS;
                world.getBlockAt(bx + w, by + h, bz).setType(mat);
            }
        }

        // min = coin bas-gauche de l'intérieur (h=1, w=0)
        // max = coin haut-droit de l'intérieur (h=3, w=1)
        // Z identique → detectAxis() retournera Axis.X
        Location min = new Location(world, bx,     by + 1, bz);
        Location max = new Location(world, bx + 1, by + 3, bz);

        WorldPortalManager.getInstance().registerWildernessPortal(worldName, min, max);

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Portail Wilderness construit dans " + worldName + " (Y=" + by + ")");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void teleportPlayersToVillage(String worldName, Location destination) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        for (var player : world.getPlayers()) {
            player.teleport(destination);
            player.sendMessage("§6[Monde] §eLe monde est en cours de réinitialisation. Retour au Village.");
        }
    }

    private void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Bukkit.unloadWorld(world, false);
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Monde déchargé : " + worldName);
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

    private LocalDate getLastResetDate() {
        String val = GameManager.getInstance().getDatabase().system().getServerData("last_reset_date");
        return val != null ? LocalDate.parse(val) : LocalDate.MIN;
    }

    private void setLastResetDate(LocalDate date) {
        GameManager.getInstance().getDatabase().system().saveServerData("last_reset_date", date.toString());
    }
}