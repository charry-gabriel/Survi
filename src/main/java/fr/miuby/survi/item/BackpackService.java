package fr.miuby.survi.item;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Logique métier du sac à dos ({@link ECustomItem#BACKPACK}).
 *
 * <p>Le contenu est sérialisé directement dans le PDC de l'ItemStack tenu par le joueur (comme une shulker box) —
 * aucune table SQL n'est nécessaire. L'identifiant {@code backpack_id} est assigné au premier clic-droit et permet
 * de retrouver l'exact ItemStack du joueur à la fermeture du menu, même s'il a changé de slot pendant que le GUI
 * était ouvert (échange de hotbar).</p>
 *
 * <p>Le nombre de slots dépend du niveau {@link EJob#LUMBERJACK} du joueur, configuré dans
 * {@code jobs/lumberjack.yml} ({@code backpack-slots}, via {@link JobsConfig.LumberjackCfg#getBackpackSlots()}).</p>
 */
public final class BackpackService {

    /** Marqueur de type posé une fois pour toutes par {@link ECustomItem#BACKPACK} (identique sur chaque copie). */
    public static final NamespacedKey BACKPACK_MARKER_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "backpack_marker");

    /** Identifiant unique de l'instance physique — assigné au premier clic-droit. */
    private static final NamespacedKey ID_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "backpack_id");

    /** Contenu sérialisé (Base64) de l'inventaire du sac. */
    private static final NamespacedKey CONTENTS_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "backpack_contents");

    private BackpackService() {}

    // ─── API publique ────────────────────────────────────────────────────────────

    public static boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BACKPACK_MARKER_KEY, PersistentDataType.BOOLEAN);
    }

    /** Nombre de slots pour un niveau de Bûcheron donné, lu depuis {@code jobs/lumberjack.yml}. */
    public static int computeSlots(int lumberjackLevel) {
        int[] slots = JobsConfig.getInstance().getLumberjack().getBackpackSlots();
        int idx = Math.clamp(lumberjackLevel, 0, slots.length - 1);
        return slots[idx];
    }

    /**
     * Ouvre le sac à dos {@code heldItem} pour {@code player}.
     * Assigne un {@code backpack_id} si absent (première ouverture), recalcule la capacité depuis le niveau
     * Bûcheron actuel, et renvoie dans l'inventaire du joueur (ou lâche au sol si plein) les objets en surplus
     * si la capacité a diminué depuis la dernière sauvegarde.
     */
    public static void open(Player player, ItemStack heldItem) {
        ItemMeta meta = heldItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        UUID backpackId;
        if (pdc.has(ID_KEY, PersistentDataType.STRING)) {
            backpackId = UUID.fromString(pdc.get(ID_KEY, PersistentDataType.STRING));
        } else {
            backpackId = UUID.randomUUID();
            pdc.set(ID_KEY, PersistentDataType.STRING, backpackId.toString());
            heldItem.setItemMeta(meta);
            player.getInventory().setItemInMainHand(heldItem);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[Backpack] Nouvel identifiant assigné : " + backpackId + " (" + player.getName() + ")");
        }

        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        int level = alphaPlayer.getJobLevel(EJob.LUMBERJACK);
        int slots = computeSlots(level);

        ItemStack[] saved = loadContents(meta);
        ItemStack[] toDisplay = new ItemStack[slots];
        int kept = Math.min(saved.length, slots);
        System.arraycopy(saved, 0, toDisplay, 0, kept);
        if (saved.length > slots) returnOverflow(player, saved, slots);

        BackpackMenuHolder holder = new BackpackMenuHolder(backpackId, player.getUniqueId());
        LangService ls = GameManager.getInstance().getLangService();
        Inventory inv = Bukkit.createInventory(holder, slots, ls.text(player, "backpack.title", level));
        holder.setInventory(inv);
        inv.setContents(toDisplay);

        player.openInventory(inv);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                "[Backpack] " + player.getName() + " ouvre son sac (niveau bûcheron " + level + ", " + slots + " slots, sac " + backpackId + ")");
    }

    /** Sauvegarde le contenu du GUI dans le PDC de l'ItemStack correspondant retrouvé dans l'inventaire du joueur. */
    public static void save(Player player, BackpackMenuHolder holder) {
        var inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!isBackpack(stack)) continue;

            ItemMeta stackMeta = stack.getItemMeta();
            String idStr = stackMeta.getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
            if (!holder.getBackpackId().toString().equals(idStr)) continue;

            stackMeta.getPersistentDataContainer().set(CONTENTS_KEY, PersistentDataType.STRING, serialize(holder.getInventory().getContents()));
            stack.setItemMeta(stackMeta);
            inventory.setItem(i, stack);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[Backpack] Contenu sauvegardé pour " + player.getName() + " (sac " + holder.getBackpackId() + ")");
            return;
        }

        MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                "[Backpack] Sac " + holder.getBackpackId() + " introuvable dans l'inventaire de "
                        + player.getName() + " à la fermeture — contenu non sauvegardé.");
    }

    // ─── Internes ────────────────────────────────────────────────────────────────

    private static void returnOverflow(Player player, ItemStack[] saved, int newSlotCount) {
        int returned = 0;
        int dropped = 0;
        for (int i = newSlotCount; i < saved.length; i++) {
            ItemStack extra = saved[i];
            if (extra == null || extra.getType().isAir()) continue;

            var leftover = player.getInventory().addItem(extra);
            if (leftover.isEmpty()) {
                returned++;
            } else {
                for (ItemStack rest : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rest);
                }
                dropped++;
            }
        }

        LangService ls = GameManager.getInstance().getLangService();
        if (returned > 0) player.sendMessage(ls.text(player, "backpack.overflow_returned", returned));
        if (dropped > 0) player.sendMessage(ls.text(player, "backpack.overflow_dropped", dropped));
        if (returned > 0 || dropped > 0) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                    "[Backpack] " + player.getName() + " — capacité réduite : " + returned
                            + " objet(s) replacés dans l'inventaire, " + dropped + " lâché(s) au sol.");
        }
    }

    private static ItemStack[] loadContents(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(CONTENTS_KEY, PersistentDataType.STRING)) return new ItemStack[0];

        String encoded = pdc.get(CONTENTS_KEY, PersistentDataType.STRING);
        try {
            return ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded));
        } catch (RuntimeException e) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.ITEM,
                    "[Backpack] Échec de désérialisation du contenu — contenu réinitialisé.", e);
            return new ItemStack[0];
        }
    }

    private static String serialize(ItemStack[] contents) {
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(contents));
    }
}