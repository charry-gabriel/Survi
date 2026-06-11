package fr.miuby.survi.grave;

import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.database.repository.GraveRepository;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.lang.LangKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

public class GraveManager {
    private static final Material GRAVE_MATERIAL = Material.CHEST;

    private final Map<Location, GraveData> graveLocations = new HashMap<>();

    public GraveManager() {
        loadFromDB();
    }

    // -------------------------------------------------------------------------
    // Chargement au démarrage
    // -------------------------------------------------------------------------

    private void loadFromDB() {
        GraveRepository repo = GameManager.getInstance().getDatabase().graves();
        List<GraveRepository.RawGrave> rows = repo.loadAll();

        int loaded = 0;
        for (GraveRepository.RawGrave row : rows) {
            World world = Bukkit.getWorld(row.worldUid());

            if (world == null) {
                // Monde introuvable — tombe orpheline (ex : ancien monde Wilderness supprimé)
                repo.remove(row.id());
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE, "Tombe orpheline supprimée (monde inexistant) : " + row.id());
                continue;
            }

            Location loc = new Location(world, row.x(), row.y(), row.z());
            if (loc.getBlock().getType() != GRAVE_MATERIAL) {
                // Le bloc n'est plus un coffre : entrée corrompue
                repo.remove(row.id());
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE, "Tombe corrompue supprimée (bloc absent) : " + row.id());
                continue;
            }

            graveLocations.put(loc, new GraveData(row.id(), row.ownerId(), loc));
            loaded++;
        }

        if (loaded > 0)
            MLLogManager.getInstance().log(Level.INFO, ELogTag.GRAVE, loaded + " tombe(s) chargée(s) depuis la base de données.");
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
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE, "Impossible de placer la tombe de " + player.getName() + " : emplacement obstrué.");
            return false;
        }

        List<ItemStack> items = collectStorageItems(player);
        if (items.isEmpty()) return false;

        UUID graveId = UUID.randomUUID();
        GraveData grave = new GraveData(graveId, player.getUniqueId(), loc);

        GameManager.getInstance().getDatabase().graves().save(
                graveId,
                player.getUniqueId(),
                loc.getWorld().getUID(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                items
        );

        graveLocations.put(loc, grave);
        loc.getBlock().setType(GRAVE_MATERIAL);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                "[CreateGrave] " + player.getName() + " → " + graveId + " en " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        // Vide uniquement les 36 slots de l'inventaire, pas l'armure
        player.getInventory().setStorageContents(new ItemStack[36]);

        player.sendMessage(GameManager.getInstance().getLangService().text(player, LangKey.GRAVE_CREATED,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
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
            player.sendMessage(GameManager.getInstance().getLangService().text(player, LangKey.GRAVE_NOT_YOURS));
            return true;
        }

        List<ItemStack> items = GameManager.getInstance().getDatabase().graves().loadItems(grave.id());
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                loc.getWorld().dropItemNaturally(loc, drop);
            }
        }

        removeGrave(grave);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE, "[CollectGrave] " + player.getName() + " a récupéré la tombe " + grave.id());
        player.sendMessage(GameManager.getInstance().getLangService().text(player, LangKey.GRAVE_RECOVERED));
        return true;
    }

    // -------------------------------------------------------------------------
    // Protection du bloc
    // -------------------------------------------------------------------------

    public boolean isGrave(Location loc) {
        return graveLocations.containsKey(loc);
    }

    // -------------------------------------------------------------------------
    // Nettoyage lors d'un reset de monde
    // -------------------------------------------------------------------------

    /**
     * Supprime de la mémoire et de la base toutes les tombes appartenant au monde donné.
     * Doit être appelé AVANT le déchargement du monde, sur le thread principal.
     */
    public void clearGravesInWorld(UUID worldUid) {
        int before = graveLocations.size();
        graveLocations.entrySet().removeIf(e -> {
            World w = e.getKey().getWorld();
            return w != null && w.getUID().equals(worldUid);
        });
        int removed = before - graveLocations.size();

        GameManager.getInstance().getDatabase().graves().removeByWorldUid(worldUid);

        if (removed > 0)
            MLLogManager.getInstance().log(Level.INFO, ELogTag.GRAVE, removed + " tombe(s) supprimée(s) suite au reset du monde " + worldUid + ".");
    }

    // -------------------------------------------------------------------------
    // Helpers internes
    // -------------------------------------------------------------------------

    private void removeGrave(GraveData grave) {
        graveLocations.remove(grave.location());
        if (grave.location().getBlock().getType() == GRAVE_MATERIAL)
            grave.location().getBlock().setType(Material.AIR);
        GameManager.getInstance().getDatabase().graves().remove(grave.id());
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }
}