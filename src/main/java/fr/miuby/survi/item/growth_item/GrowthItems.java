package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.AddEnchantmentItemEffect;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
     * Seuls les mobs de niveau ≥ 30 comptent (vérifié côté listener).</p>
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

    // ─── API publique pour les commandes admin ────────────────────────────────

    /**
     * Retourne l'ID growth ({@code growth_id} PDC) d'un item, ou {@code null} s'il n'en a pas.
     *
     * @param item l'item à inspecter (peut être {@code null} ou air)
     * @return l'ID growth, ex. {@code "GROWTH_PICKAXE"}, ou {@code null}
     */
    @Nullable
    public static String getGrowthId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Force le passage au palier suivant d'un item de croissance sans passer par la logique d'utilisation.
     *
     * <p>Modifie {@code item} en place. L'appelant doit le remettre dans l'inventaire si nécessaire
     * (l'item était obtenu via un getter retournant une copie, ex. {@code getHelmet()}, {@code getItem(slot)}).</p>
     *
     * @param item  l'item à faire monter de palier
     * @param alpha le joueur cible (pour les effets de type message, haste, potion)
     * @return {@code true} si le palier a été appliqué, {@code false} si l'item n'est pas un growth item
     *         ou est déjà au palier maximum
     */
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

    // ─── Reload à chaud — mise à jour des items en main/équipés ──────────────

    /**
     * Réapplique les effets persistants (name, set_attribute, add_enchantment) de tous les paliers
     * atteints sur {@code item} en se basant sur la nouvelle config du registre.
     *
     * <p>Séquence :</p>
     * <ol>
     *   <li>Défait la contribution growth sur les enchantements (via tracking PDC ou inférence legacy).</li>
     *   <li>Réapplique les effets non-transitoires de chaque palier de 0 à {@code currentTier - 1}.</li>
     *   <li>Si la nouvelle config expose des paliers que les {@code uses} actuels suffisent à déclencher
     *       (ex. nouveau palier ajouté avec un seuil déjà franchi), les avance silencieusement.</li>
     * </ol>
     *
     * <p>Les effets transitoires (message, haste, potion) sont ignorés — ils ne sont pas rejoués
     * lors d'un reload.</p>
     *
     * <p>L'appelant doit remettre l'item dans l'inventaire si le getter utilisé retourne une copie
     * (ex. {@code getHelmet()}).</p>
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
        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);

        // Étape 1 : défaire la contribution growth sur les enchantements
        resetGrowthEnchantments(item, currentTier, config);

        // Étape 2 : réappliquer les effets persistants de tous les paliers déjà atteints
        int newTier = Math.min(currentTier, config.tiers().size());
        for (int i = 0; i < newTier; i++) {
            for (ItemEffect effect : config.tiers().get(i).effects()) {
                if (!effect.isTransient()) effect.apply(item, alpha);
            }
        }

        // Étape 3 : avancer silencieusement les paliers que les uses actuels débloquent déjà
        while (newTier < config.tiers().size() && uses >= config.tiers().get(newTier).requiredUses()) {
            for (ItemEffect effect : config.tiers().get(newTier).effects()) {
                if (!effect.isTransient()) effect.apply(item, alpha);
            }
            newTier++;
        }

        // Étape 4 : mettre à jour le tier en PDC si nécessaire
        if (newTier != currentTier) {
            meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(TIER_KEY, PersistentDataType.INTEGER, newTier);
            item.setItemMeta(meta);
        }
    }

    /**
     * Appelle {@link #reapplyAll} sur tous les growth items tenus en main ou équipés
     * (main, offhand, armure) par les joueurs actuellement en ligne.
     *
     * <p>À appeler après {@link GrowthItemLoader#reload()} pour que les items
     * au palier maximum (sans use restant) reçoivent immédiatement la nouvelle config.</p>
     */
    public static void reapplyOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer alpha;
            try {
                alpha = AlphaPlayer.get(player.getUniqueId());
            } catch (AlphaPlayerNotFoundException ignored) {
                continue;
            }
            PlayerInventory inv = player.getInventory();

            ItemStack mainHand = inv.getItemInMainHand();
            if (getGrowthId(mainHand) != null) {
                reapplyAll(mainHand, alpha);
                inv.setItemInMainHand(mainHand);
            }

            ItemStack offHand = inv.getItemInOffHand();
            if (getGrowthId(offHand) != null) {
                reapplyAll(offHand, alpha);
                inv.setItemInOffHand(offHand);
            }

            ItemStack[] armor = inv.getArmorContents();
            boolean anyUpdated = false;
            for (ItemStack piece : armor) {
                if (piece != null && !piece.getType().isAir() && getGrowthId(piece) != null) {
                    reapplyAll(piece, alpha);
                    anyUpdated = true;
                }
            }
            if (anyUpdated) inv.setArmorContents(armor);
        }
    }

    // ─── Reset enchantements growth ───────────────────────────────────────────

    /**
     * Défait la contribution growth sur les enchantements d'un item avant une réapplication complète.
     *
     * <p>Deux stratégies :</p>
     * <ul>
     *   <li><b>Tracking PDC</b> (items créés avec ce système) : les clés {@code growth_ench_*}
     *       contiennent le montant exact ajouté par le growth — on le soustrait et on retire les clés.</li>
     *   <li><b>Inférence legacy</b> (items sans tracking) : on somme les {@code amount} de tous les
     *       {@link AddEnchantmentItemEffect} des paliers atteints depuis la config courante et on
     *       soustrait ce total. Approximation acceptable pour un item existant avant ce système.</li>
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
            // Tracking précis : soustraire les montants enregistrés
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
            // Legacy : inférer depuis la config courante
            Map<Enchantment, Integer> inferred = new HashMap<>();
            for (int i = 0; i < Math.min(currentTier, config.tiers().size()); i++) {
                for (ItemEffect effect : config.tiers().get(i).effects()) {
                    if (effect instanceof AddEnchantmentItemEffect aeie)
                        inferred.merge(aeie.enchantment(), aeie.amount(), Integer::sum);
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

    /** Désérialise un CSV en {@link Set}, en ignorant les chaînes vides. */
    private static Set<String> parseSet(String csv) {
        if (csv.isBlank()) return new HashSet<>();
        Set<String> set = new HashSet<>(Arrays.asList(csv.split(",")));
        set.remove("");
        return set;
    }
}