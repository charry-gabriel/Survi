package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

public class RandomItemEffect extends BlessingEffect {
    public final Map<ItemStack, Integer> weightedMap;
    private final Random random = new Random();

    public RandomItemEffect(Map<ItemStack, Integer> weightedMap) {
        this.weightedMap = weightedMap;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        int rand = random.nextInt(100);
        int sum = 0;
        ItemStack item = null;

        for (var mapEntry : weightedMap.entrySet()) {
            if (rand <= sum + mapEntry.getValue()) {
                item = mapEntry.getKey();
                break;
            } else {
                sum = sum + mapEntry.getValue();
            }
        }

        if (item != null)
            player.getPlayer().getInventory().addItem(item);
    }
}
