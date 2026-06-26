package fr.miuby.survi.grave;

import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.database.repository.GraveRepository;
import fr.miuby.survi.system.log.ELogTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
                repo.remove(row.id());
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE, "Tombe orpheline supprimée (monde inexistant) : " + row.id());
                continue;
            }

            Location loc = new Location(world, row.x(), row.y(), row.z());
            if (loc.getBlock().getType() != GRAVE_MATERIAL) {
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
    // Création de la tombe à la mort
    // -------------------------------------------------------------------------

    /**
     * Crée une tombe contenant le contenu de l'inventaire (36 slots) et l'item en main secondaire.
     * L'armure reste sur le joueur, gérée séparément par PlayerListener.
     * La tombe est placée sur le premier bloc solide disponible, même si le joueur est mort dans le vide.
     *
     * @return true si la tombe a été créée (les drops doivent alors être effacés).
     */
    public boolean createGrave(Player player) {
        Location deathLoc = player.getLocation().getBlock().getLocation();
        Location loc = findAvailableLocation(deathLoc);
        if (loc == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE,
                    "[CreateGrave] Impossible de placer la tombe de " + player.getName() + " autour de "
                            + deathLoc.getWorld().getName() + " " + deathLoc.getBlockX() + "," + deathLoc.getBlockY() + "," + deathLoc.getBlockZ());

            MLWorld deathWorld = WorldRegistry.get(deathLoc.getWorld().getUID());
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "grave.not_created",
                    Placeholder.unparsed("x", Integer.toString(deathLoc.getBlockX())),
                    Placeholder.unparsed("y", Integer.toString(deathLoc.getBlockY())),
                    Placeholder.unparsed("z", Integer.toString(deathLoc.getBlockZ())),
                    Placeholder.component("world", Component.text(deathWorld.getName(), deathWorld.getColor()))));
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
                "[CreateGrave] " + player.getName() + " → " + graveId
                        + " en " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                        + " (mort en " + deathLoc.getBlockX() + "," + deathLoc.getBlockY() + "," + deathLoc.getBlockZ() + ")");

        player.getInventory().setStorageContents(new ItemStack[36]);
        player.getInventory().setItemInOffHand(null);
        MLWorld w = WorldRegistry.get(loc.getWorld().getUID());

        player.sendMessage(GameManager.getInstance().getLangService().text(player, "grave.created",
                Placeholder.unparsed("x", Integer.toString(loc.getBlockX())),
                Placeholder.unparsed("y", Integer.toString(loc.getBlockY())),
                Placeholder.unparsed("z", Integer.toString(loc.getBlockZ())),
                Placeholder.component("world", Component.text(w.getName(), w.getColor()))));
        return true;
    }

    // -------------------------------------------------------------------------
    // Recherche d'emplacement — gestion du vide + scan spiral
    // -------------------------------------------------------------------------

    /**
     * Trouve le meilleur emplacement libre pour poser la tombe.
     * 1. Si le joueur est mort dans le vide, cherche latéralement (en spirale)
     *    une colonne X,Z voisine qui possède du sol, puis se place dessus.
     * 2. Sinon, essaie la position de mort puis 1 bloc au-dessus.
     * 3. Si bloqué, scan en spirale (rayon 1-4) autour de la position de mort.
     */
    private Location findAvailableLocation(Location base) {
        World world = base.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        // Étape 1 : mort dans le vide → chercher une surface solide à côté
        if (isInVoid(base, minY)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[FindLoc] Mort dans le vide en " + base.getBlockX() + "," + base.getBlockY() + "," + base.getBlockZ() + " — recherche surface voisine");
            Location surface = findSurfaceNearby(base, minY, maxY);
            if (surface != null) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                        "[FindLoc] Surface voisine trouvée en " + surface.getBlockX() + "," + surface.getBlockY() + "," + surface.getBlockZ());
            } else {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE,
                        "[FindLoc] Aucune surface trouvée dans un rayon de 16 blocs");
            }
            return surface;
        }

        // Étape 2 : position directe et 1 bloc au-dessus
        for (int dy = 0; dy <= 1; dy++) {
            int y = base.getBlockY() + dy;
            if (y < minY || y > maxY) continue;
            Location candidate = new Location(world, base.getBlockX(), y, base.getBlockZ());
            if (canPlaceAt(candidate)) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                        "[FindLoc] Emplacement direct en " + candidate.getBlockX() + "," + y + "," + candidate.getBlockZ() + " (dy=" + dy + ")");
                return candidate;
            }
        }

        // Étape 3 : scan en spirale autour de la position de mort
        Location nearby = scanNearby(base, minY, maxY);
        if (nearby != null) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[FindLoc] Emplacement spiral en " + nearby.getBlockX() + "," + nearby.getBlockY() + "," + nearby.getBlockZ());
        }
        return nearby;
    }

    /**
     * Retourne true si le joueur est mort dans le vide :
     * soit en dessous de minY, soit dans une colonne entièrement vide jusqu'à minY.
     */
    private boolean isInVoid(Location loc, int minY) {
        if (loc.getBlockY() < minY) return true;
        World world = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        for (int y = loc.getBlockY(); y >= minY; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) return false;
        }
        return true;
    }

    /**
     * Cherche en spirale (rayon 0→16) autour de la position de mort une colonne X,Z
     * qui possède un bloc solide en surface, puis retourne la position juste au-dessus.
     * Utilise getHighestBlockYAt pour éviter de scanner colonne par colonne.
     */
    private Location findSurfaceNearby(Location base, int minY, int maxY) {
        World world = base.getWorld();
        int cx = base.getBlockX(), cz = base.getBlockZ();

        for (int r = 0; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue; // périmètre uniquement
                    int x = cx + dx, z = cz + dz;
                    int surfaceY = world.getHighestBlockYAt(x, z);
                    if (surfaceY < minY) continue; // colonne vide
                    if (world.getBlockAt(x, surfaceY, z).getType().isAir()) continue; // sécurité
                    Location top = new Location(world, x, surfaceY + 1, z);
                    if (top.getBlockY() <= maxY && canPlaceAt(top)) return top;
                }
            }
        }
        return null;
    }

    /**
     * Scan en spirale (périmètre carré) autour de center, rayon 1 à 4.
     * Essaie la position et 1 bloc au-dessus à chaque candidat.
     */
    private Location scanNearby(Location center, int minY, int maxY) {
        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        for (int r = 1; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // périmètre uniquement
                    for (int dy = 0; dy <= 1; dy++) {
                        int y = cy + dy;
                        if (y < minY || y > maxY) continue;
                        Location candidate = new Location(world, cx + dx, y, cz + dz);
                        if (canPlaceAt(candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean canPlaceAt(Location loc) {
        return loc.getBlock().getType().isAir() || loc.getBlock().isLiquid();
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
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "grave.not_yours"));
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
        player.sendMessage(GameManager.getInstance().getLangService().text(player, "grave.recovered"));
        return true;
    }

    /**
     * Transfère tous les items de la tombe appartenant à {@code ownerId} dans l'inventaire
     * de {@code recipient}. Les items excédentaires droppent à la position de recipient.
     * Utilisé par la commande /grave recover pour les cas où le joueur ne peut pas
     * atteindre physiquement sa tombe (mort dans le vide, emplacement inaccessible…).
     *
     * @return true si une tombe appartenant à ownerId a été trouvée et récupérée.
     */
    public boolean collectGraveByOwner(Player recipient, UUID ownerId) {
        // Collecter toutes les tombes du joueur en une seule passe (un joueur peut avoir
        // plusieurs tombes s'il est mort plusieurs fois sans les récupérer)
        List<GraveData> graves = new ArrayList<>();
        for (GraveData data : graveLocations.values()) {
            if (data.ownerId().equals(ownerId)) graves.add(data);
        }
        if (graves.isEmpty()) return false;

        for (GraveData grave : graves) {
            List<ItemStack> items = GameManager.getInstance().getDatabase().graves().loadItems(grave.id());
            for (ItemStack item : items) {
                Map<Integer, ItemStack> leftover = recipient.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    recipient.getWorld().dropItemNaturally(recipient.getLocation(), drop);
                }
            }
            removeGrave(grave);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[CollectGraveByOwner] tombe " + grave.id() + " de " + ownerId
                            + " → " + recipient.getName()
                            + " (était en " + grave.location().getWorld().getName() + " " + grave.location().getBlockX() + "," + grave.location().getBlockY() + "," + grave.location().getBlockZ() + ")");
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.GRAVE,
                "[CollectGraveByOwner] " + graves.size() + " tombe(s) de " + ownerId + " récupérée(s) pour " + recipient.getName());
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

    private List<ItemStack> collectStorageItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isValidItem(item)) items.add(item);
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isValidItem(offHand)) items.add(offHand);

        return items;
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }
}