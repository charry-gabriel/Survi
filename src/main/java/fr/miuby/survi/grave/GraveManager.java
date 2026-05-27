package fr.miuby.survi.grave;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class GraveManager {
    private static final Material GRAVE_MATERIAL = Material.CHEST;

    private final File gravesFolder;
    private final Map<Location, GraveData> graveLocations = new HashMap<>();

    public GraveManager() {
        this.gravesFolder = new File(GameManager.getInstance().getPlugin().getDataFolder(), "graves");

        if (!gravesFolder.exists() && !gravesFolder.mkdirs())
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.GRAVE, "Impossible de créer le dossier de tombes.");

        loadFromDisk();
    }

    // -------------------------------------------------------------------------
    // Chargement au démarrage
    // -------------------------------------------------------------------------

    private void loadFromDisk() {
        File[] files = gravesFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            try {
                UUID graveId = UUID.fromString(file.getName().replace(".yml", ""));
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                Location loc = cfg.getLocation("location");
                UUID ownerId = UUID.fromString(cfg.getString("owner-uuid"));

                if (loc != null && loc.isWorldLoaded() && loc.getBlock().getType() == GRAVE_MATERIAL) {
                    graveLocations.put(loc, new GraveData(graveId, ownerId, loc));
                    loaded++;
                } else {
                    if (!file.delete())
                        LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.GRAVE, "Impossible de supprimer le fichier corrompu : " + file.getName());
                }
            } catch (Exception e) {
                LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.GRAVE, "Fichier corrompu ignoré : " + file.getName(), e);
            }
        }

        if (loaded > 0)
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.GRAVE, loaded + " tombe(s) chargée(s) depuis le disque.");
    }

    // -------------------------------------------------------------------------
    // Création de la tombe à la mort (inventaire uniquement, pas l'armure)
    // -------------------------------------------------------------------------

    /**
     * Crée une tombe contenant uniquement le contenu de l'inventaire (36 slots).
     * L'armure reste sur le joueur, gérée séparément par PlayerListener.
     *
     * @return true si la tombe a été créée (les drops doivent alors être effacés).
     */
    public boolean createGrave(Player player) {
        Location loc = findAvailableLocation(player.getLocation().getBlock().getLocation());
        if (loc == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.GRAVE, "Impossible de placer la tombe de " + player.getName() + " : emplacement obstrué.");
            return false;
        }

        List<ItemStack> items = collectStorageItems(player);
        if (items.isEmpty()) return false;

        UUID graveId = UUID.randomUUID();
        GraveData grave = new GraveData(graveId, player.getUniqueId(), loc);
        saveGraveFile(grave, items);
        graveLocations.put(loc, grave);
        loc.getBlock().setType(GRAVE_MATERIAL);
        LogManager.getInstance().log(Level.FINE, LogManager.ETagLog.GRAVE,
                "[CreateGrave] " + player.getName() + " → " + graveId + " en " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        // Vide uniquement les 36 slots de l'inventaire, pas l'armure
        player.getInventory().setStorageContents(new ItemStack[36]);

        player.sendMessage(Component.text("☠ Votre tombe a été créée en " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + " [" + loc.getWorld().getName() + "]").color(NamedTextColor.GREEN));
        return true;
    }

    private Location findAvailableLocation(Location base) {
        if (base.getBlock().getType().isAir() || base.getBlock().isLiquid())
            return base;

        Location above = base.clone().add(0, 1, 0);
        if (above.getBlock().getType().isAir())
            return above;

        return null;
    }

    private List<ItemStack> collectStorageItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isValidItem(item)) items.add(item);
        }
        return items;
    }

    // -------------------------------------------------------------------------
    // Récupération de la tombe (clic droit = transfert direct)
    // -------------------------------------------------------------------------

    /**
     * Transfère tous les items de la tombe dans l'inventaire du joueur.
     * Les items qui ne rentrent pas droppent au sol.
     *
     * @return true si l'emplacement correspond à une tombe.
     */
    public boolean collectGrave(Player player, Location loc) {
        GraveData grave = graveLocations.get(loc);
        if (grave == null) return false;

        if (!grave.ownerId().equals(player.getUniqueId()) && !player.hasPermission("survi.grave.bypass")) {
            player.sendMessage(Component.text("Ce n'est pas votre tombe !").color(NamedTextColor.RED));
            return true;
        }

        List<ItemStack> items = loadItems(grave);
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                loc.getWorld().dropItemNaturally(loc, drop);
            }
        }

        removeGrave(grave);
        LogManager.getInstance().log(Level.FINE, LogManager.ETagLog.GRAVE,
                "[CollectGrave] " + player.getName() + " a récupéré la tombe " + grave.id());
        player.sendMessage(Component.text("Vous avez récupéré votre tombe.").color(NamedTextColor.GREEN));
        return true;
    }

    // -------------------------------------------------------------------------
    // Protection du bloc
    // -------------------------------------------------------------------------

    public boolean isGrave(Location loc) {
        return graveLocations.containsKey(loc);
    }

    // -------------------------------------------------------------------------
    // Helpers internes
    // -------------------------------------------------------------------------

    private List<ItemStack> loadItems(GraveData grave) {
        File file = getGraveFile(grave);
        if (!file.exists()) return new ArrayList<>();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<?> raw = cfg.getList("items", new ArrayList<>());
        List<ItemStack> items = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof ItemStack itemStack)
                items.add(itemStack);
        }
        return items;
    }

    private void saveGraveFile(GraveData grave, List<ItemStack> items) {
        File file = getGraveFile(grave);
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("owner-uuid", grave.ownerId().toString());
        cfg.set("location", grave.location());
        cfg.set("items", items);
        try {
            cfg.save(file);
        } catch (IOException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.GRAVE, "Impossible de sauvegarder " + file.getName(), e);
        }
    }

    private void removeGrave(GraveData grave) {
        graveLocations.remove(grave.location());
        if (grave.location().getBlock().getType() == GRAVE_MATERIAL)
            grave.location().getBlock().setType(Material.AIR);
        if (!getGraveFile(grave).delete())
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.GRAVE, "Impossible de supprimer le fichier de " + grave.id());
    }

    private File getGraveFile(GraveData grave) {
        return new File(gravesFolder, grave.id() + ".yml");
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }
}