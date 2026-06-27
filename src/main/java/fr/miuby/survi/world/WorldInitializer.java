package fr.miuby.survi.world;

import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WorldInitializer {

    private static final Map<EWorld, String> DISPLAY_NAMES = Map.of(
            EWorld.VILLAGE,    "Village",
            EWorld.WILDERNESS, "Wilderness",
            EWorld.NETHER,     "Nether",
            EWorld.END,        "End"
    );

    @Getter
    private static final Map<EWorld, String> worlds = new HashMap<>();

    private static boolean initialized = false;

    private WorldInitializer() { }

    // -------------------------------------------------------------------------
    // Init principal appelé par GameManager
    // -------------------------------------------------------------------------

    public static void initializeIfNeeded() {
        if (initialized) return;

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Chargement des mondes...");

        // Village : nom fixe, ne change jamais
        worlds.put(EWorld.VILLAGE, "Village");

        // Wilderness : seul monde dont on stocke le nom en DB.
        // Nether et End sont toujours dérivés de ce nom.
        String wildName = getSavedWorldName(EWorld.WILDERNESS);
        if (wildName == null) {
            wildName = "Wilderness_1";
            saveWorldName(EWorld.WILDERNESS, wildName);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Nouveau monde créé : " + wildName);
        } else {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Monde existant chargé : " + wildName);
        }
        worlds.put(EWorld.WILDERNESS, wildName);

        // POINT 3 : Nether et End sont dérivés du nom Wilderness.
        // Convention Paper/Bukkit : <worldName>_nether et <worldName>_the_end.
        // Cela suffit pour que les portails vanilla les connectent automatiquement.
        worlds.put(EWorld.NETHER, wildName + "_nether");
        worlds.put(EWorld.END,    wildName + "_the_end");

        initializeWorlds();
        initialized = true;

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Tous les mondes sont prêts.");
        GameManager.getInstance().initAfterWorldsLoad();
    }

    // -------------------------------------------------------------------------
    // Chargement / création des worlds Bukkit + enregistrement MiubyLib
    // -------------------------------------------------------------------------

    private static void initializeWorlds() {
        // --- Village (NORMAL, permanent) ---
        World village = loadOrCreate("Village", World.Environment.NORMAL);
        MLWorld mvVillage = new MLWorld(village, "Village", NamedTextColor.AQUA, EWorld.VILLAGE);
        mvVillage.setLimit(new Rect(1024,0,512,50,1024,0));
        mvVillage.getWorld().setSpawnLocation(new Location(mvVillage.getWorld(), -24, 158, -30));
        WorldRegistry.register(mvVillage);
        village.setGameRule(GameRules.ADVANCE_TIME, false);
        village.setGameRule(GameRules.MOB_GRIEFING, false);
        village.setGameRule(GameRules.KEEP_INVENTORY, true);
        village.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, true);
        village.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        village.setClearWeatherDuration(Integer.MAX_VALUE);
        village.setDifficulty(Difficulty.EASY);
        village.setViewDistance(32);

        // --- Wilderness (NORMAL, réinitialisable) ---
        String wildName = worlds.get(EWorld.WILDERNESS);
        World wilderness = loadOrCreate(wildName, World.Environment.NORMAL);
        MLWorld mvWild = new MLWorld(wilderness, "Wilderness", NamedTextColor.GOLD, EWorld.WILDERNESS);
        WorldRegistry.register(mvWild);
        wilderness.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        wilderness.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 45);
        wilderness.setViewDistance(8);
        wilderness.setDifficulty(Difficulty.HARD);

        // --- Nether (NETHER, réinitialisable, verrouillé par défaut) ---
        String netherName = worlds.get(EWorld.NETHER);
        World nether = loadOrCreate(netherName, World.Environment.NETHER);
        MLWorld mvNether = new MLWorld(nether, "Nether", NamedTextColor.RED, EWorld.NETHER);
        mvNether.setLocked(true);
        WorldRegistry.register(mvNether);
        nether.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        nether.setDifficulty(Difficulty.HARD);

        // --- End (THE_END, réinitialisable, verrouillé par défaut) ---
        String endName = worlds.get(EWorld.END);
        World end = loadOrCreate(endName, World.Environment.THE_END);
        MLWorld mvEnd = new MLWorld(end, "End", NamedTextColor.YELLOW, EWorld.END);
        mvEnd.setLocked(true);
        WorldRegistry.register(mvEnd);
        end.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        end.setDifficulty(Difficulty.HARD);
    }

    // -------------------------------------------------------------------------
    // Appelé par WorldResetManager après création des nouveaux mondes.
    // Met à jour la map en mémoire + la DB (WILDERNESS seulement) + WorldRegistry.
    // Pour WILDERNESS, dérive automatiquement les nouveaux noms Nether et End.
    // -------------------------------------------------------------------------

    public static void updateWorldNames(String newWildName) {
        String newNetherName = newWildName + "_nether";
        String newEndName    = newWildName + "_the_end";

        String oldWildName   = worlds.get(EWorld.WILDERNESS);
        String oldNetherName = worlds.get(EWorld.NETHER);
        String oldEndName    = worlds.get(EWorld.END);

        worlds.put(EWorld.WILDERNESS, newWildName);
        worlds.put(EWorld.NETHER,     newNetherName);
        worlds.put(EWorld.END,        newEndName);

        // Seul le nom Wilderness est stocké en DB (Nether/End en sont dérivés).
        saveWorldName(EWorld.WILDERNESS, newWildName);

        registerUpdatedWorld(EWorld.WILDERNESS, newWildName, NamedTextColor.GOLD,   false);
        registerUpdatedWorld(EWorld.NETHER,     newNetherName, NamedTextColor.RED,  true);
        registerUpdatedWorld(EWorld.END,         newEndName, NamedTextColor.YELLOW, true);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "WorldRegistry mis à jour : " + oldWildName + " → " + newWildName
                        + ", " + oldNetherName + " → " + newNetherName
                        + ", " + oldEndName   + " → " + newEndName);
    }

    private static void registerUpdatedWorld(EWorld eWorld, String worldName, NamedTextColor color, boolean locked) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD,
                    "updateWorldNames : monde introuvable après reset : " + worldName);
            // On ne masque pas l'erreur, le jeu doit crasher proprement ici.
            throw new IllegalStateException("Monde introuvable après reset : " + worldName);
        }
        MLWorld mlWorld = new MLWorld(world, DISPLAY_NAMES.get(eWorld), color, eWorld);
        if (locked) mlWorld.setLocked(true);
        WorldRegistry.register(mlWorld);
    }

    // -------------------------------------------------------------------------
    // Génère le prochain nom de Wilderness (Wilderness_1 → Wilderness_2, etc.)
    // Les noms Nether et End sont automatiquement dérivés.
    // -------------------------------------------------------------------------

    public static String generateNextWildernessName() {
        String current = worlds.get(EWorld.WILDERNESS);
        String base = "Wilderness_";
        int counter = 1;
        if (current != null && current.startsWith(base)) {
            try {
                counter = Integer.parseInt(current.substring(base.length())) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return base + counter;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Charge un monde existant ou le crée via WorldCreator.
     * Lève une IllegalStateException si la création échoue (jamais null retourné).
     */
    public static World loadOrCreate(String name, World.Environment env) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "Création du monde : " + name + " (" + env + ")");
        World created = new WorldCreator(name).environment(env).createWorld();
        if (created == null) {
            // On ne cache pas l'erreur avec un if : on la lève explicitement.
            throw new IllegalStateException("Échec de la création du monde : " + name + " (" + env + ")");
        }
        created.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        created.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 45);
        created.setDifficulty(Difficulty.HARD);
        created.setViewDistance(8);
        return created;
    }

    // -------------------------------------------------------------------------
    // DB accessors (Wilderness uniquement)
    // -------------------------------------------------------------------------

    private static String getSavedWorldName(EWorld eWorld) {
        return GameManager.getInstance().getDatabase().system()
                .getServerData("world_current_name_" + eWorld.name().toLowerCase());
    }

    private static void saveWorldName(EWorld eWorld, String name) {
        GameManager.getInstance().getDatabase().system()
                .saveServerData("world_current_name_" + eWorld.name().toLowerCase(), name);
    }

    // -------------------------------------------------------------------------
    // Accesseurs publics
    // -------------------------------------------------------------------------

    public static World getDefaultWorld() {
        return Bukkit.getWorld(worlds.get(EWorld.VILLAGE));
    }
}