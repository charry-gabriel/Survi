package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applique un effet de potion Bukkit au joueur pendant N secondes.
 *
 * <p>Utilisé dans les YAML de growth items via {@code type: potion} :
 * <pre>
 *   - { type: potion, effect: speed,    seconds: 30, amplifier: 1 }
 *   - { type: potion, effect: strength, seconds: 15, amplifier: 0 }
 * </pre>
 *
 * <p>{@code amplifier} : 0 = niveau I, 1 = niveau II, etc.
 * Si absent dans le YAML, SnakeYAML le positionne à 0 (niveau I).
 */
public record PotionItemEffect(PotionEffectType potionEffectType, int seconds, int amplifier) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        Player p = player.getPlayer();
        if (p == null) return;
        p.addPotionEffect(new PotionEffect(potionEffectType, seconds * 20, amplifier,
                false,  // ambient  – pas de particules d'ambiance
                true,   // particles – visible
                true)); // icon      – icône dans l'inventaire
    }

    @Override
    public boolean isTransient() { return true; }
}