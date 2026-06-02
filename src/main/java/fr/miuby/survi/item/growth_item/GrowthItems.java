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

public final class GrowthItems {

    public static final NamespacedKey ID_KEY   = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_id");
    public static final NamespacedKey USES_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_uses");
    public static final NamespacedKey TIER_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_tier");

    private GrowthItems() {}

    /**
     * Charge tous les growth items depuis {@code growth_items/*.yml}.
     * Appelé depuis {@link fr.miuby.survi.GameManager}.
     */
    public static void init() {
        GrowthItemLoader.loadAll();
    }

    // ─── Incrément principal — item tenu en main ──────────────────────────────

    public static void IncrementUses(Player player, String event) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            hand = player.getInventory().getItemInOffHand();
        if (hand.getType().isAir())
            return;

        ItemMeta meta = hand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(USES_KEY, PersistentDataType.INTEGER)) return;

        String growthId = pdc.get(ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        hand.setItemMeta(meta);

        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            hand.setItemMeta(meta);
            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(hand, AlphaPlayer.get(player.getUniqueId()));
        }

        final ItemStack usedItem = hand;
        final AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(pe -> {
            if (uses % pe.everyUses() == 0)
                pe.effects().forEach(e -> e.apply(usedItem, alpha));
        });
    }

    // ─── Incrément casque — item porté en slot HEAD ───────────────────────────

    /**
     * Incrémente les uses d'un growth item équipé en casque.
     * Appelle {@code setHelmet()} en fin de méthode car {@code getHelmet()}
     * retourne une copie, pas une référence directe.
     */
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

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        helmet.setItemMeta(meta);

        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            helmet.setItemMeta(meta);
            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(helmet, AlphaPlayer.get(player.getUniqueId()));
        }

        final ItemStack helmetFinal = helmet;
        final AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(pe -> {
            if (uses % pe.everyUses() == 0)
                pe.effects().forEach(e -> e.apply(helmetFinal, alpha));
        });

        // Commit : getHelmet() retourne une copie
        player.getInventory().setHelmet(helmetFinal);
    }

    // ─── Utilitaire — détection en mains ─────────────────────────────────────

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
}