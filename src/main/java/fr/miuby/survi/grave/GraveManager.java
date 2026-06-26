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
     * 1. Si le joueur est mort dans le vide (aucun sol sous ses pieds), remonte
     *    jusqu'au premier bloc solide en scannant vers le haut.
     * 2. Essaie la position ajustée puis 1 bloc au-dessus.
     * 3. Si bloqué, scan en spirale jusqu'au rayon 4.
     *
     * @return un emplacement libre, ou null si aucun n'est trouvé.
     */
    private Location findAvailableLocation(Location base) {
        World world = base.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        // Étape 1 : corriger une position dans le vide
        Location adjusted = avoidVoid(base, minY, maxY);

        // Étape 2 : position ajustée et 1 bloc au-dessus
        for (int dy = 0; dy <= 1; dy++) {
            int y = adjusted.getBlockY() + dy;
            if (y > maxY) break;
            Location candidate = new Location(world, adjusted.getBlockX(), y, adjusted.getBlockZ());
            if (canPlaceAt(candidate)) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                        "[FindLoc] Emplacement trouvé en " + candidate.getBlockX() + "," + y + "," + candidate.getBlockZ() + " (dy=" + dy + ")");
                return candidate;
            }
        }

        // Étape 3 : scan en spirale autour de la position ajustée
        Location nearby = scanNearby(adjusted, minY, maxY);
        if (nearby != null) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[FindLoc] Emplacement spiral trouvé en " + nearby.getBlockX() + "," + nearby.getBlockY() + "," + nearby.getBlockZ());
        }
        return nearby;
    }

    /**
     * Si la position de mort est dans le vide (en dessous de minY ou sans aucun bloc
     * non-air en dessous jusqu'à minY), remonte en scannant vers le haut pour trouver
     * le dessus du premier bloc solide.
     */
    private Location avoidVoid(Location loc, int minY, int maxY) {
        int y = loc.getBlockY();

        if (y < minY) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[FindLoc] Mort sous minY (" + y + " < " + minY + ") — scan ascendant depuis " + minY);
            return scanUpForSolidTop(new Location(loc.getWorld(), loc.getBlockX(), minY, loc.getBlockZ()), maxY);
        }

        if (!hasSolidGroundBelow(loc, minY)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[FindLoc] Aucun sol détecté sous Y=" + y + " — scan ascendant (vide)");
            return scanUpForSolidTop(loc, maxY);
        }

        return loc;
    }

    /**
     * Retourne true si au moins un bloc non-air existe entre la position et minY (exclu).
     * Les blocs liquides (eau, lave) comptent comme sol.
     */
    private boolean hasSolidGroundBelow(Location loc, int minY) {
        World world = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        for (int y = loc.getBlockY() - 1; y >= minY; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) return true;
        }
        return false;
    }

    /**
     * Remonte depuis start et retourne la position juste au-dessus du premier bloc
     * non-air trouvé. Si aucun bloc solide n'est trouvé avant maxY, retourne start.
     */
    private Location scanUpForSolidTop(Location start, int maxY) {
        World world = start.getWorld();
        int x = start.getBlockX(), z = start.getBlockZ();
        for (int y = start.getBlockY(); y <= maxY; y++) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                        "[FindLoc] Premier bloc solide trouvé en Y=" + y + " → tombe en Y=" + (y + 1));
                return new Location(world, x, y + 1, z);
            }
        }
        MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE,
                "[FindLoc] Aucun bloc solide trouvé en remontant depuis Y=" + start.getBlockY() + " — position de mort conservée");
        return start.clone();
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