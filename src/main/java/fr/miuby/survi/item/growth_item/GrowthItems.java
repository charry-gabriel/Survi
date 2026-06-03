package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GrowthItems {

    // ─── Clés PDC communes ────────────────────────────────────────────────────

    public static final NamespacedKey ID_KEY   = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_id");
    public static final NamespacedKey USES_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_uses");
    public static final NamespacedKey TIER_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_tier");

    // ─── Clés PDC spécifiques aux items à progression unique ─────────────────

    /**
     * Liste CSV des biomes déjà visités par le joueur avec la boussole.
     * Ex. {@code "minecraft:plains,minecraft:desert,minecraft:forest"}.
     * Stockée sur l'item lui-même — si l'item est perdu, la progression l'est aussi.
     */
    public static final NamespacedKey VISITED_BIOMES_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_visited_biomes");

    /**
     * Liste CSV des types de mobs (niveau ≥ 30) déjà tués avec l'épée shiny.
     * Ex. {@code "ZOMBIE,SKELETON,CREEPER"}.
     */
    public static final NamespacedKey KILLED_MOB_TYPES_KEY =
            new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_killed_mob_types");

    private GrowthItems() {}

    public static void init() {
        GrowthItemLoader.loadAll();
    }

    // ─── IncrementUses — item en main ─────────────────────────────────────────

    public static void IncrementUses(Player player, String event) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            hand = player.getInventory().getItemInOffHand();
        if (hand.getType().isAir()) return;

        ItemMeta meta = hand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(USES_KEY, PersistentDataType.INTEGER)) return;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        doIncrement(hand, meta, pdc, player, config);
    }

    // ─── IncrementUsesFromHelmet — item en slot HEAD ──────────────────────────

    public static void IncrementUsesFromHelmet(Player player, String event) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().isAir()) return;

        ItemMeta meta = helmet.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(USES_KEY, PersistentDataType.INTEGER)) return;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        doIncrement(helmet, meta, pdc, player, config);
        player.getInventory().setHelmet(helmet); // getHelmet() retourne une copie
    }

    // ─── IncrementUsesOnNewBiome — boussole ───────────────────────────────────

    /**
     * Incrémente les uses de la boussole uniquement si {@code biomeKey} n'a jamais
     * été visité avec cet item.
     *
     * <p>La liste des biomes visités est stockée en CSV dans
     * {@link #VISITED_BIOMES_KEY} sur l'item lui-même.
     *
     * @param player   le joueur qui change de biome
     * @param biomeKey clé namespaced du biome (ex. {@code minecraft:plains})
     */
    public static void IncrementUsesOnNewBiome(Player player, String biomeKey) {
        ItemStack compass = findGrowthItemInHands(player, "GROWTH_BOUSSOLE_AVENTURIER");
        if (compass == null) return;

        ItemMeta meta = compass.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        GrowthConfig config = GrowthItemRegistry.get("GROWTH_BOUSSOLE_AVENTURIER");
        if (config == null) return;

        Set<String> visited = parseSet(pdc.getOrDefault(VISITED_BIOMES_KEY, PersistentDataType.STRING, ""));
        if (visited.contains(biomeKey)) return; // déjà découvert

        visited.add(biomeKey);
        pdc.set(VISITED_BIOMES_KEY, PersistentDataType.STRING, String.join(",", visited));
        compass.setItemMeta(meta); // commit de la liste avant l'incrément

        doIncrement(compass, meta, pdc, player, config);
    }

    // ─── IncrementUsesOnNewMobType — épée shiny ──────────────────────────────

    /**
     * Incrémente les uses de l'épée shiny uniquement si {@code entityTypeName}
     * n'a jamais été tué avec cet item.
     *
     * <p>La liste des types tués est stockée en CSV dans
     * {@link #KILLED_MOB_TYPES_KEY} sur l'item.
     * Seuls les mobs de niveau ≥ 30 comptent (vérifié côté listener).
     *
     * @param player         le joueur
     * @param entityTypeName nom du type Bukkit (ex. {@code ZOMBIE}, {@code CREEPER})
     */
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
        if (killed.contains(entityTypeName)) return; // déjà tué ce type

        killed.add(entityTypeName);
        pdc.set(KILLED_MOB_TYPES_KEY, PersistentDataType.STRING, String.join(",", killed));
        sword.setItemMeta(meta); // commit de la liste avant l'incrément

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

    /**
     * Cœur du système de growth : incrémente USES, déclenche le palier si le seuil
     * est atteint, et applique les effets périodiques.
     *
     * <p>Reçoit le {@code meta} et le {@code pdc} déjà ouverts pour pouvoir
     * s'enchaîner après une modification préalable (ex. ajout d'un biome en CSV).
     * Chaque {@code item.setItemMeta(meta)} committera l'état accumulé.
     */
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

    /** Désérialise un CSV en {@link Set}, en ignorant les chaînes vides. */
    private static Set<String> parseSet(String csv) {
        if (csv.isBlank()) return new HashSet<>();
        Set<String> set = new HashSet<>(Arrays.asList(csv.split(",")));
        set.remove("");
        return set;
    }
}