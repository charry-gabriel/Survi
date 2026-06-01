package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.*;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import static org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

public final class GrowthItems {
    public static final NamespacedKey ID_KEY   = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_id");
    public static final NamespacedKey USES_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_uses");
    public static final NamespacedKey TIER_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_tier");

    private GrowthItems() {}

    static {
        // ─── GROWTH_PICKAXE ──────────────────────────────────────────────────────────
        GrowthConfig pickaxe = GrowthConfig.builder("BlockBreakEvent")
                .tier(8,
                        new NameItemEffect(Component.text("the pioche 2 !")),
                        new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1),
                        new MessageItemEffect(Component.text("Lvlup de ta pioche ! une petite fortune de plus.")))
                .tier(16,
                        new NameItemEffect(Component.text("the pioche 3 !")),
                        new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1),
                        new MessageItemEffect(Component.text("Lvlup de ta pioche ! une petite fortune de plus.")))
                .tier(22,
                        new NameItemEffect(Component.text("the pioche des pioches !")),
                        new AddEnchantmentItemEffect(Enchantment.FORTUNE, 2),
                        new MessageItemEffect(Component.text("Lvlup de ta pioche ! deux fortune de plus, bim !")))
                .periodic(10,
                        new HasteItemEffect(10),
                        new MessageItemEffect(Component.text("Un peu de haste pour toi !")))
                .build();

        GrowthItemRegistry.register("GROWTH_PICKAXE", pickaxe);

        // ─── GROWTH_SWORD ────────────────────────────────────────────────────────────
        GrowthConfig sword = GrowthConfig.builder("BlockBreakEvent")
                .tier(2, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1))
                .tier(4, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1))
                .tier(6, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 2))
                .periodic(3, new HasteItemEffect(1))
                .build();

        GrowthItemRegistry.register("GROWTH_SWORD", sword);

        // ─── GROWTH_CASQUE_MINEUR ────────────────────────────────────────────────────
        // Item secondaire du MINEUR : porté sur la tête, grandit en minant des minerais.
        // Laisse la pioche totalement libre pour l'enchantement normal.
        // Les valeurs de MINING_EFFICIENCY remplacent la valeur initiale posée dans ECustomItem
        // (SetAttributeItemEffect retire tous les modificateurs existants avant d'appliquer le nouveau).
        GrowthConfig casque = GrowthConfig.builder("OreBreakEvent")
                .tier(15,
                        new NameItemEffect(Component.text("Casque de Mineur II")),
                        new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 8.0, ADD_NUMBER, EquipmentSlotGroup.HEAD),
                        new MessageItemEffect(Component.text("Ton casque grandit — tu mines un peu plus vite !")))
                .tier(35,
                        new NameItemEffect(Component.text("Casque de Mineur III")),
                        new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 14.0, ADD_NUMBER, EquipmentSlotGroup.HEAD),
                        new MessageItemEffect(Component.text("Impressionnant ! Tu tailles dans la roche comme dans du beurre.")))
                .tier(65,
                        new NameItemEffect(Component.text("Casque du Maître Mineur")),
                        new SetAttributeItemEffect(Attribute.MINING_EFFICIENCY, 22.0, ADD_NUMBER, EquipmentSlotGroup.HEAD),
                        new MessageItemEffect(Component.text("Casque de légende. Tu es le roi des profondeurs.")))
                .periodic(10,
                        new HasteItemEffect(8),
                        new MessageItemEffect(Component.text("Un souffle de haste surgit de ton casque !")))
                .build();

        GrowthItemRegistry.register("GROWTH_CASQUE_MINEUR", casque);

        // ─── GROWTH_BATON_FERMIER ────────────────────────────────────────────────────
        // Item secondaire du FERMIER : tenu en main secondaire, grandit en récoltant des cultures.
        // Laisse la houe totalement libre en main principale.
        // Le bonus de récolte est calculé dans GrowthItemListener (pas via ItemEffect)
        // car il nécessite le contexte du BlockBreakEvent.
        GrowthConfig baton = GrowthConfig.builder("CropBreakEvent")
                .tier(20,
                        new NameItemEffect(Component.text("Bâton du Fermier II")),
                        new MessageItemEffect(Component.text("Ton bâton grandit — tes récoltes s'améliorent (×1,5) !")))
                .tier(50,
                        new NameItemEffect(Component.text("Bâton du Fermier III")),
                        new MessageItemEffect(Component.text("Excellent ! Tes récoltes doublent maintenant (×2,0) !")))
                .tier(90,
                        new NameItemEffect(Component.text("Bâton du Grand Fermier")),
                        new MessageItemEffect(Component.text("Bâton de légende — tes champs débordent (×2,5) !")))
                .periodic(12,
                        new HasteItemEffect(6),
                        new MessageItemEffect(Component.text("Un élan de vitalité te traverse !")))
                .build();

        GrowthItemRegistry.register("GROWTH_BATON_FERMIER", baton);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Incrément principal — item tenu en main (existant)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Incrémente les utilisations du growth item tenu en main principale ou secondaire.
     * N'agit que si l'item est un growth item dont l'eventType correspond à {@code event}.
     */
    public static void IncrementUses(Player player, String event) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            hand = player.getInventory().getItemInOffHand();
        if (hand.getType().isAir())
            return;

        ItemMeta meta = hand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(GrowthItems.USES_KEY, PersistentDataType.INTEGER))
            return;

        String growthId = pdc.get(GrowthItems.ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, uses);
        hand.setItemMeta(meta);

        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            hand.setItemMeta(meta);
            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(hand, AlphaPlayer.get(player.getUniqueId()));
        }

        final ItemStack usedItem = hand;
        final AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(periodicEffect -> {
            if (uses % periodicEffect.everyUses() == 0) {
                for (ItemEffect effect : periodicEffect.effects())
                    effect.apply(usedItem, alpha);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Incrément casque — item équipé en slot HEAD (nouveau)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Incrémente les utilisations du growth item porté en casque (slot HEAD).
     * Utilisé par le {@code GROWTH_CASQUE_MINEUR}.
     *
     * <p>Contrairement à {@link #IncrementUses}, cette méthode cible l'inventaire
     * d'armure. Elle doit appeler {@code player.getInventory().setHelmet(item)}
     * à la fin car {@code getHelmet()} retourne une copie, pas une référence directe.
     *
     * @param player le joueur portant le casque
     * @param event  le type d'event attendu (ex. "OreBreakEvent")
     */
    public static void IncrementUsesFromHelmet(Player player, String event) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().isAir()) return;

        ItemMeta meta = helmet.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(GrowthItems.USES_KEY, PersistentDataType.INTEGER)) return;

        String growthId = pdc.get(GrowthItems.ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, uses);
        helmet.setItemMeta(meta);

        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            helmet.setItemMeta(meta);
            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(helmet, AlphaPlayer.get(player.getUniqueId()));
        }

        final ItemStack helmetFinal = helmet;
        final AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(periodicEffect -> {
            if (uses % periodicEffect.everyUses() == 0) {
                for (ItemEffect effect : periodicEffect.effects())
                    effect.apply(helmetFinal, alpha);
            }
        });

        // getHelmet() retourne une copie — on pousse l'état final dans l'inventaire
        player.getInventory().setHelmet(helmetFinal);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Utilitaire — détection d'un growth item dans les mains
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Cherche un growth item identifié par {@code growthId} dans la main principale
     * puis la main secondaire du joueur.
     *
     * @return l'ItemStack correspondant, ou {@code null} si absent
     */
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

    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Force le chargement de la classe (et donc l'exécution du bloc static).
     * Appelée depuis GameManager.
     */
    public static void init() {
        // rien : l'appel garantit simplement l'exécution du bloc static.
    }
}