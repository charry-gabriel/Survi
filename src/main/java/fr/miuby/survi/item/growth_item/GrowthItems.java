package fr.miuby.survi.item.growth_item;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.AddEnchantmentItemEffect;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class GrowthItems {

    // ─── Clés PDC communes ────────────────────────────────────────────────────

    public static final NamespacedKey ID_KEY     = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_id");
    public static final NamespacedKey USES_KEY   = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_uses");
    public static final NamespacedKey TIER_KEY   = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_tier");

    /**
     * Version de config au moment de la dernière réapplication des effets sur cet item.
     * Comparée à {@link GrowthItemLoader#getConfigVersion()} pour détecter les items stale.
     * Valeur absente ↔ 0 (jamais mis à jour depuis un reload).
     */
    public static final NamespacedKey RELOAD_VERSION_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_reload_version");

    // ─── Clés PDC spécifiques aux items à progression unique ─────────────────

    public static final NamespacedKey VISITED_BIOMES_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_visited_biomes");

    public static final NamespacedKey KILLED_MOB_TYPES_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_killed_mob_types");

    /** Secondes de feu infligées aux ennemis frappés — stockées dans le PDC de l'item par {@link fr.miuby.survi.item.growth_item.effect.FireEnemiesItemEffect}. */
    public static final NamespacedKey FIRE_SECONDS_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_fire_seconds");

    private GrowthItems() {}

    public static void init() {
        GrowthItemLoader.initConfigVersion();
        GrowthItemLoader.loadAll();
    }

    // ─── IncrementUses — progression par utilisation, un ou plusieurs slots ──

    /**
     * Incrémente les uses du growth item équipé dans un des {@code slots} donnés, si sa
     * config est déclenchée par {@code event}.
     *
     * <p>Les slots sont essayés dans l'ordre fourni ; le premier non-vide est utilisé — les
     * autres sont ignorés. Exemples :</p>
     * <pre>
     *   IncrementUses(player, "BlockBreakEvent", HAND, OFF_HAND);  // outil tenu en main
     *   IncrementUses(player, "OreBreakEvent",   HEAD);            // casque
     *   IncrementUses(player, "OreBreakEvent",   LEGS);            // jambières
     * </pre>
     */
    public static void IncrementUses(Player player, String event, EquipmentSlot... slots) {
        PlayerInventory inv = player.getInventory();

        ItemStack item = null;
        EquipmentSlot foundSlot = null;
        for (EquipmentSlot slot : slots) {
            ItemStack candidate = getEquipped(inv, slot);
            if (candidate != null && !candidate.getType().isAir()) {
                item = candidate;
                foundSlot = slot;
                break;
            }
        }
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(USES_KEY, PersistentDataType.INTEGER)) return;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        // Correction silencieuse si le YAML a été rechargé depuis la dernière utilisation
        if (checkAndReapplyIfStale(item, player)) {
            meta = item.getItemMeta();
            pdc  = meta.getPersistentDataContainer();
        }

        doIncrement(item, meta, pdc, player, config);
        setEquipped(inv, foundSlot, item); // certains getters (armure) retournent une copie
    }

    /** Récupère l'item actuellement dans {@code slot}. Retourne {@code null} pour un slot non géré. */
    @Nullable
    private static ItemStack getEquipped(PlayerInventory inv, EquipmentSlot slot) {
        return switch (slot) {
            case HAND     -> inv.getItemInMainHand();
            case OFF_HAND -> inv.getItemInOffHand();
            case HEAD     -> inv.getHelmet();
            case CHEST    -> inv.getChestplate();
            case LEGS     -> inv.getLeggings();
            case FEET     -> inv.getBoots();
            default       -> null;
        };
    }

    /** Réécrit {@code item} dans {@code slot} (no-op pour un slot non géré). */
    private static void setEquipped(PlayerInventory inv, EquipmentSlot slot, ItemStack item) {
        switch (slot) {
            case HAND     -> inv.setItemInMainHand(item);
            case OFF_HAND -> inv.setItemInOffHand(item);
            case HEAD     -> inv.setHelmet(item);
            case CHEST    -> inv.setChestplate(item);
            case LEGS     -> inv.setLeggings(item);
            case FEET     -> inv.setBoots(item);
            default       -> {}
        }
    }

    // ─── IncrementUsesOnNewBiome — boussole ───────────────────────────────────

    public static void IncrementUsesOnNewBiome(Player player, String biomeKey) {
        ItemStack compass = findGrowthItemInHands(player, "GROWTH_BOUSSOLE_EXPLORER");
        if (compass == null) return;

        ItemMeta meta = compass.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        GrowthConfig config = GrowthItemRegistry.get("GROWTH_BOUSSOLE_EXPLORER");
        if (config == null) return;

        Set<String> visited = parseSet(pdc.getOrDefault(VISITED_BIOMES_KEY, PersistentDataType.STRING, ""));
        if (visited.contains(biomeKey)) return;

        visited.add(biomeKey);
        pdc.set(VISITED_BIOMES_KEY, PersistentDataType.STRING, String.join(",", visited));
        compass.setItemMeta(meta);

        doIncrement(compass, meta, pdc, player, config);
    }

    // ─── IncrementUsesOnNewMobType — épée shiny ──────────────────────────────

    public static void IncrementUsesOnNewMobType(Player player, String entityTypeName) {
        ItemStack sword = player.getInventory().getItemInMainHand();
        if (sword.getType().isAir()) return;

        ItemMeta meta = sword.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(USES_KEY, PersistentDataType.INTEGER)) return;

        if (!"GROWTH_EPEE_SHINY".equals(pdc.get(ID_KEY, PersistentDataType.STRING))) return;

        GrowthConfig config = GrowthItemRegistry.get("GROWTH_EPEE_SHINY");
        if (config == null) return;

        Set<String> killed = parseSet(pdc.getOrDefault(KILLED_MOB_TYPES_KEY, PersistentDataType.STRING, ""));
        if (killed.contains(entityTypeName)) return;

        killed.add(entityTypeName);
        pdc.set(KILLED_MOB_TYPES_KEY, PersistentDataType.STRING, String.join(",", killed));
        sword.setItemMeta(meta);

        doIncrement(sword, meta, pdc, player, config);
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    @Nullable
    public static ItemStack findGrowthItemInHands(Player player, String growthId) {
        for (ItemStack hand : new ItemStack[]{
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand()}) {
            if (hand.getType().isAir()) continue;
            ItemMeta meta = hand.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(ID_KEY, PersistentDataType.STRING)) continue;
            if (growthId.equals(pdc.get(ID_KEY, PersistentDataType.STRING))) return hand;
        }
        return null;
    }

    // ─── Logique d'incrément partagée ────────────────────────────────────────

    private static void doIncrement(ItemStack item, ItemMeta meta, PersistentDataContainer pdc,
                                    Player player, GrowthConfig config) {
        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);

        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            item.setItemMeta(meta);
            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(item, AlphaPlayer.get(player.getUniqueId()));
        }

        final AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(pe -> {
            if (uses % pe.everyUses() == 0)
                pe.effects().forEach(e -> e.apply(item, alpha));
        });
    }

    // ─── API publique pour les commandes admin ────────────────────────────────

    @Nullable
    public static String getGrowthId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
    }

    public static boolean forceLvlUp(ItemStack item, AlphaPlayer alpha) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ID_KEY, PersistentDataType.STRING)) return false;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null) return false;

        int currentTier = pdc.getOrDefault(TIER_KEY, PersistentDataType.INTEGER, 0);
        if (currentTier >= config.tiers().size()) return false;

        GrowthTier tier = config.tiers().get(currentTier);
        pdc.set(TIER_KEY, PersistentDataType.INTEGER, currentTier + 1);
        pdc.set(USES_KEY, PersistentDataType.INTEGER, tier.requiredUses());
        item.setItemMeta(meta);

        for (ItemEffect effect : tier.effects())
            effect.apply(item, alpha);

        return true;
    }

    // ─── Détection de stale et réapplication paresseuse ──────────────────────

    /**
     * Vérifie si la clé PDC {@link #RELOAD_VERSION_KEY} de l'item est inférieure à
     * {@link GrowthItemLoader#getConfigVersion()} — ce qui signifie que le YAML a été
     * rechargé depuis la dernière application des effets sur cet item.
     *
     * <p>Si c'est le cas, appelle {@link #reapplyAll} et retourne {@code true}.
     * Sinon (cas nominal), retourne {@code false} après un seul accès PDC O(1).</p>
     *
     * <p>L'appelant doit remettre l'item dans l'inventaire si le getter utilisé
     * retourne une copie (ex. {@code getHelmet()}, {@code getItem(slot)}).</p>
     */
    public static boolean checkAndReapplyIfStale(ItemStack item, Player player) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ID_KEY, PersistentDataType.STRING)) return false;

        int itemVersion = pdc.getOrDefault(RELOAD_VERSION_KEY, PersistentDataType.INTEGER, 0);
        if (itemVersion >= GrowthItemLoader.getConfigVersion()) return false;

        try {
            reapplyAll(item, AlphaPlayer.get(player.getUniqueId()));
        } catch (AlphaPlayerNotFoundException ignored) {
            return false;
        }
        return true;
    }

    /**
     * Vérifie et réapplique si nécessaire les effets growth de tout ce que {@code player}
     * tient ou porte actuellement (main, offhand, armure complète).
     *
     * <p>Point d'entrée générique de détection de staleness, indépendant de l'event métier
     * ({@code eventType}) associé à l'item — couvre la connexion, le swap main/offhand et
     * tout équipement via clic d'inventaire (armure, offhand, shift-click).</p>
     */
    public static void checkAndReapplyHeldAndEquipped(Player player) {
        PlayerInventory inv = player.getInventory();

        ItemStack mainHand = inv.getItemInMainHand();
        if (getGrowthId(mainHand) != null && checkAndReapplyIfStale(mainHand, player)) {
            inv.setItemInMainHand(mainHand);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[GrowthItems] Reapply (main) " + player.getName() + " : " + getGrowthId(mainHand));
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (getGrowthId(offHand) != null && checkAndReapplyIfStale(offHand, player)) {
            inv.setItemInOffHand(offHand);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[GrowthItems] Reapply (offhand) " + player.getName() + " : " + getGrowthId(offHand));
        }

        ItemStack[] armor = inv.getArmorContents();
        boolean anyUpdated = false;
        for (ItemStack piece : armor) {
            if (getGrowthId(piece) != null && checkAndReapplyIfStale(piece, player)) {
                anyUpdated = true;
                MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                        "[GrowthItems] Reapply (armure) " + player.getName() + " : " + getGrowthId(piece));
            }
        }
        if (anyUpdated) inv.setArmorContents(armor);
    }

    // ─── Réapplication complète depuis la config courante ────────────────────

    /**
     * Réapplique les effets persistants (name, set_attribute, add_enchantment) de tous les paliers
     * atteints sur {@code item}, en se basant sur la config courante du registre.
     *
     * <p>Séquence :</p>
     * <ol>
     *   <li>Défait la contribution growth sur les enchantements.</li>
     *   <li>Réapplique les effets non-transitoires de chaque palier atteint.</li>
     *   <li>Avance silencieusement les paliers que les uses actuels débloquent déjà
     *       (ex. nouveau palier ajouté avec un seuil déjà franchi).</li>
     *   <li>Estampille {@link #RELOAD_VERSION_KEY} avec la version de config courante.</li>
     * </ol>
     */
    public static void reapplyAll(ItemStack item, AlphaPlayer alpha) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ID_KEY, PersistentDataType.STRING)) return;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null) return;

        int currentTier = pdc.getOrDefault(TIER_KEY, PersistentDataType.INTEGER, 0);
        int uses        = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);

        // 1. Défaire la contribution growth sur les enchantements
        resetGrowthEnchantments(item, currentTier, config);

        // 2. Réappliquer les effets de base (stats initiales configurables via YAML)
        for (ItemEffect effect : config.baseEffects()) {
            if (!effect.isTransient()) effect.apply(item, alpha);
        }

        // 3. Réappliquer les effets persistants de tous les paliers déjà atteints
        int newTier = Math.min(currentTier, config.tiers().size());
        for (int i = 0; i < newTier; i++) {
            for (ItemEffect effect : config.tiers().get(i).effects()) {
                if (!effect.isTransient()) effect.apply(item, alpha);
            }
        }

        // 4. Avancer silencieusement si les uses débloquent déjà de nouveaux paliers
        while (newTier < config.tiers().size() && uses >= config.tiers().get(newTier).requiredUses()) {
            for (ItemEffect effect : config.tiers().get(newTier).effects()) {
                if (!effect.isTransient()) effect.apply(item, alpha);
            }
            newTier++;
        }

        // 5. Mettre à jour le tier et la version de config en PDC
        meta = item.getItemMeta();
        pdc  = meta.getPersistentDataContainer();
        if (newTier != currentTier)
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, newTier);
        pdc.set(RELOAD_VERSION_KEY, PersistentDataType.INTEGER, GrowthItemLoader.getConfigVersion());
        item.setItemMeta(meta);
    }

    // ─── Reset enchantements growth ───────────────────────────────────────────

    /**
     * Défait la contribution growth sur les enchantements avant une réapplication complète.
     *
     * <ul>
     *   <li><b>Tracking PDC</b> (clés {@code growth_ench_*}) : soustraction exacte.</li>
     *   <li><b>Inférence legacy</b> (items sans tracking, ex. créés avant cette version) :
     *       on somme les {@code amount} des {@link AddEnchantmentItemEffect} des paliers atteints
     *       depuis la config courante et on soustrait ce total.</li>
     * </ul>
     */
    @SuppressWarnings("deprecation")
    private static void resetGrowthEnchantments(ItemStack item, int currentTier, GrowthConfig config) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String namespace = GameManager.getInstance().getPlugin().getName().toLowerCase();

        List<NamespacedKey> trackKeys = new ArrayList<>();
        for (NamespacedKey k : pdc.getKeys()) {
            if (k.getNamespace().equals(namespace) && k.getKey().startsWith("growth_ench_"))
                trackKeys.add(k);
        }

        if (!trackKeys.isEmpty()) {
            // Tracking précis
            for (NamespacedKey key : trackKeys) {
                Integer amount = pdc.get(key, PersistentDataType.INTEGER);
                pdc.remove(key);
                if (amount == null || amount == 0) continue;
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(key.getKey().substring("growth_ench_".length())));
                if (ench == null || !meta.hasEnchant(ench)) continue;
                int newLevel = Math.max(0, meta.getEnchantLevel(ench) - amount);
                if (newLevel == 0) meta.removeEnchant(ench);
                else meta.addEnchant(ench, newLevel, true);
            }
        } else {
            // Legacy : inférer depuis la config courante (base + paliers atteints)
            Map<Enchantment, Integer> inferred = new HashMap<>();
            // Effets de base (toujours présents, quel que soit le tier)
            for (ItemEffect effect : config.baseEffects()) {
                if (effect instanceof AddEnchantmentItemEffect(Enchantment enchantment, int amount))
                    inferred.merge(enchantment, amount, Integer::sum);
            }
            // Effets des paliers atteints
            for (int i = 0; i < Math.min(currentTier, config.tiers().size()); i++) {
                for (ItemEffect effect : config.tiers().get(i).effects()) {
                    if (effect instanceof AddEnchantmentItemEffect(Enchantment enchantment, int amount))
                        inferred.merge(enchantment, amount, Integer::sum);
                }
            }
            for (Map.Entry<Enchantment, Integer> entry : inferred.entrySet()) {
                if (!meta.hasEnchant(entry.getKey())) continue;
                int newLevel = Math.max(0, meta.getEnchantLevel(entry.getKey()) - entry.getValue());
                if (newLevel == 0) meta.removeEnchant(entry.getKey());
                else meta.addEnchant(entry.getKey(), newLevel, true);
            }
        }

        item.setItemMeta(meta);
    }

    // ─── Utilitaire interne ───────────────────────────────────────────────────

    private static Set<String> parseSet(String csv) {
        if (csv.isBlank()) return new HashSet<>();
        Set<String> set = new HashSet<>(Arrays.asList(csv.split(",")));
        set.remove("");
        return set;
    }
}