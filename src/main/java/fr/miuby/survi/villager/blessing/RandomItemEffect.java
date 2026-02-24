package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
public class RandomItemEffect extends BlessingEffect {
    private final Map<ItemStack, Integer> weightedMap;
    private final Random random = new Random();

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
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
