package fr.miuby.survi.item.growth_item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.*;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class GrowthItems {
    public static final NamespacedKey ID_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_id");
    public static final NamespacedKey USES_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_uses");
    public static final NamespacedKey TIER_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_tier");

    private GrowthItems() {}

    static {
        // Configuration de la pioche
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

        // Configuration de l'épée
        GrowthConfig sword = GrowthConfig.builder("BlockBreakEvent")
            .tier(2, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1))
            .tier(4, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 1))
            .tier(6, new AddEnchantmentItemEffect(Enchantment.FORTUNE, 2))
            .periodic(3, new HasteItemEffect(1))
            .build();
            
        GrowthItemRegistry.register("GROWTH_SWORD", sword);
    }

    public static void IncrementUses(Player player, String event) {
        //take the correct hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            hand = player.getInventory().getItemInOffHand();

        // Check if growth item
        ItemMeta meta = hand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(GrowthItems.USES_KEY, PersistentDataType.INTEGER))
            return;

        // Get growth ID
        String growthId = pdc.get(GrowthItems.ID_KEY, PersistentDataType.STRING);
        if (growthId == null) return;

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        if (config == null || !config.eventType().equals(event)) return;

        // Increment uses
        int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0) + 1;
        int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

        pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, uses);
        hand.setItemMeta(meta);

        // Upgrade tier
        if (tier < config.tiers().size() && uses >= config.tiers().get(tier).requiredUses()) {
            pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, tier + 1);
            hand.setItemMeta(meta);

            for (ItemEffect effect : config.tiers().get(tier).effects())
                effect.apply(hand, AlphaPlayer.get(player.getUniqueId()));
        }

        // Periodic effects
        final ItemStack usedItem = hand;
        final var alpha = AlphaPlayer.get(player.getUniqueId());
        config.periodicEffects().forEach(periodicEffect -> {
            if (uses % periodicEffect.everyUses() == 0) {
                for (ItemEffect effect : periodicEffect.effects())
                    effect.apply(usedItem, alpha);
            }
        });
    }

    /**
     * Force la classe à se charger (et donc à enregistrer les configurations).
     * Appelée depuis GameManager.
     */
    public static void init() {
        // rien : l'appel garantit simplement l'exécution du bloc static.
    }
}
