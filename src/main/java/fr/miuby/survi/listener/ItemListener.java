package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.ArmorTierService;
import fr.miuby.survi.item.BackpackMenuHolder;
import fr.miuby.survi.item.BackpackService;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.role.Role;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.villagerlevel.VillagerTributeHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ItemListener implements Listener {

    /**
     * Remplacements de tier inférieur en attente, indexés par UUID joueur et slot.
     * Présent dès le {@link PlayerItemBreakEvent} ; retiré soit par le {@code runLater} si le joueur survit,
     * soit par {@link #onPlayerRespawn} si le joueur est mort ce même tick.
     */
    private final Map<UUID, Map<EquipmentSlot, ItemStack>> pendingDowngrades = new HashMap<>();

    /**
     * UUID des joueurs morts avec au moins un downgrade en attente.
     * Ajouté dans {@link #onPlayerDeathDowngrade}, retiré dans {@link #onPlayerRespawn} / {@link #onPlayerQuitDowngrade}.
     */
    private final Set<UUID> pendingDeaths = new HashSet<>();

    // ─── Craft / recettes ────────────────────────────────────────────────────────

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        if (GameManager.getInstance().getLockedItemsFactory().isLocked(result.getType().getKey())) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        CustomRecipe customRecipe = CustomRecipe.getCustomRecipe(result);
        if (customRecipe == null) return;

        if (event.getViewers().isEmpty() || !(event.getViewers().getFirst() instanceof Player viewer)) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        AlphaPlayer alpha = AlphaPlayer.get(viewer.getUniqueId());
        if (alpha == null) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        List<String> roles = customRecipe.getRoles();
        if (roles != null && !roles.isEmpty()) {
            boolean hasPermission = false;
            for (Role r : alpha.getSubRoles()) {
                if (roles.contains(r.type().name())) {
                    hasPermission = true;
                    break;
                }
            }
            if (!hasPermission) event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getResult().getType().getKey())
                || event.getResult().getType() == Material.COPPER_BOOTS
                || event.getResult().getType() == Material.COPPER_CHESTPLATE
                || event.getResult().getType() == Material.COPPER_LEGGINGS
                || event.getResult().getType() == Material.COPPER_HELMET
                || event.getResult().getType() == Material.IRON_BOOTS
                || event.getResult().getType() == Material.IRON_CHESTPLATE
                || event.getResult().getType() == Material.IRON_LEGGINGS
                || event.getResult().getType() == Material.IRON_HELMET
                || event.getResult().getType() == Material.GOLDEN_BOOTS
                || event.getResult().getType() == Material.GOLDEN_HELMET
                || event.getResult().getType() == Material.GOLDEN_CHESTPLATE
                || event.getResult().getType() == Material.GOLDEN_LEGGINGS
                || event.getResult().getType() == Material.DIAMOND_BOOTS
                || event.getResult().getType() == Material.DIAMOND_CHESTPLATE
                || event.getResult().getType() == Material.DIAMOND_HELMET
                || event.getResult().getType() == Material.DIAMOND_LEGGINGS) {
            event.setResult(new ItemStack(Material.AIR));
        }
    }

    // ─── Downgrade d'armure ───────────────────────────────────────────────────────

    /**
     * Quand une armure casse, on stocke le remplacement et on planifie un {@code runLater}.
     * <p>
     * Si le joueur survit au tick courant : le {@code runLater} pose l'item directement (comportement nominal).
     * Si le joueur meurt ce tick : {@link #onPlayerDeathDowngrade} marquera l'UUID et le {@code runLater}
     * ne posera pas l'item — c'est {@link #onPlayerRespawn} qui le donnera.
     */
    @EventHandler
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        Material broken = event.getBrokenItem().getType();
        EquipmentSlot slot = ArmorTierService.slotFor(broken);
        if (slot == null) return;

        ItemStack replacement = ArmorTierService.getLowerTierReplacement(broken);
        if (replacement == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        pendingDowngrades.computeIfAbsent(uuid, k -> new EnumMap<>(EquipmentSlot.class)).put(slot, replacement);

        MiubyLib.runLater(() -> {
            if (pendingDeaths.contains(uuid)) {
                // Le joueur est mort ce même tick — le downgrade sera remis au respawn
                MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                        "[Item] " + player.getName() + " — Broke " + broken.name() + " (mort) → downgrade différé au respawn");
                return;
            }
            // Joueur vivant — downgrade immédiat (comportement nominal)
            Map<EquipmentSlot, ItemStack> pending = pendingDowngrades.get(uuid);
            if (pending == null) return;
            ItemStack item = pending.remove(slot);
            if (pending.isEmpty()) pendingDowngrades.remove(uuid);
            if (item == null) return;
            player.getEquipment().setItem(slot, item);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[Item] " + player.getName() + " — Broke " + broken.name() + " → downgrade " + item.getType().name());
        }, 1L);

        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[Item] " + player.getName() + " — Broke " + broken.name());
    }

    /**
     * Si le joueur meurt avec un downgrade en attente, on l'inscrit dans {@code pendingDeaths}
     * pour que le {@code runLater} d'onPlayerItemBreak ne pose pas l'item sur un joueur mort.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathDowngrade(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingDowngrades.containsKey(uuid)) return;
        pendingDeaths.add(uuid);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[Item] " + event.getPlayer().getName() + " — mort avec downgrade(s) en attente → remise au respawn");
    }

    /**
     * Au respawn, donne les downgrades en attente dans le slot approprié.
     * Si le slot est déjà occupé (edge case), l'item est ajouté à l'inventaire.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingDeaths.remove(uuid);
        Map<EquipmentSlot, ItemStack> pending = pendingDowngrades.remove(uuid);
        if (pending == null || pending.isEmpty()) return;

        Player player = event.getPlayer();
        MiubyLib.runLater(() -> {
            for (Map.Entry<EquipmentSlot, ItemStack> entry : pending.entrySet()) {
                EquipmentSlot s = entry.getKey();
                ItemStack item = entry.getValue();
                ItemStack current = player.getEquipment().getItem(s);
                if (current == null || current.getType() == Material.AIR) {
                    player.getEquipment().setItem(s, item);
                    MLLogManager.getInstance().log(Level.INFO, ELogTag.PLAYER,
                            "[Item] " + player.getName() + " — downgrade au respawn : " + item.getType().name() + " slot=" + s.name());
                } else {
                    player.getInventory().addItem(item);
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,
                            "[Item] " + player.getName() + " — downgrade respawn slot=" + s.name() + " occupé → ajout inventaire : " + item.getType().name());
                }
            }
        }, 1L);
    }

    /** Nettoyage si le joueur se déconnecte avant le respawn. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitDowngrade(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingDeaths.remove(uuid);
        pendingDowngrades.remove(uuid);
    }

    // ─── Inventaires ─────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() == InventoryType.MERCHANT
                || event.getInventory().getType() == InventoryType.MERCHANT) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (event.getClickedInventory().getHolder() instanceof Player
                && event.getInventory().getHolder() instanceof VillagerTributeHolder holder) {
            if (item != null && item.getType() != Material.AIR)
                holder.getVillagerLevel().giveItems(event.getInventory(), item, player);
            event.setCancelled(true);
        } else if (event.getClickedInventory().getHolder() instanceof VillagerTributeHolder) {
            event.setCancelled(true);
        }
    }

    // ─── Sac à dos (BackpackMenuHolder) ──────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!BackpackService.isBackpack(item)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        long backpackCount = Arrays.stream(player.getInventory().getContents())
                .filter(BackpackService::isBackpack)
                .count();
        if (backpackCount > 1) {
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "backpack.too_many"));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[Backpack] " + player.getName() + " bloqué à l'ouverture — " + backpackCount + " sacs en inventaire.");
            return;
        }

        BackpackService.open(player, item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBackpackInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackMenuHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean movingIntoBackpack =
                (event.getClickedInventory() != null
                        && event.getClickedInventory().getHolder() instanceof BackpackMenuHolder
                        && BackpackService.isBackpack(event.getCursor()))
                        || (event.isShiftClick()
                        && event.getClickedInventory() != null
                        && event.getClickedInventory().getHolder() instanceof Player
                        && BackpackService.isBackpack(event.getCurrentItem()));

        if (movingIntoBackpack) {
            event.setCancelled(true);
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "backpack.no_self_insert"));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[Backpack] " + player.getName() + " — tentative bloquée de mise d'un sac à dos dans un sac à dos.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBackpackInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackMenuHolder)) return;
        if (!BackpackService.isBackpack(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        boolean targetsBackpack = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (targetsBackpack) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(GameManager.getInstance().getLangService().text(player, "backpack.no_self_insert"));
            }
        }
    }

    @EventHandler
    public void onBackpackInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackMenuHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        BackpackService.save(player, holder);
    }
}