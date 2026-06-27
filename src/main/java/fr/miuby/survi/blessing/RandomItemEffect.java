package fr.miuby.survi.blessing;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

@RequiredArgsConstructor
public class RandomItemEffect extends BlessingEffect {
    private final Map<ItemStack, Integer> weightedMap;
    private final Random random = new Random();

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

        if (item == null) return;

        Player p = player.getPlayer();
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
        if (leftover.isEmpty()) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                    "[RandomItemEffect] " + player.getPseudo() + " a reçu " + item.getAmount() + "x " + item.getType());
        } else {
            for (ItemStack dropped : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), dropped);
            }
            MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                    "[RandomItemEffect] " + player.getPseudo() + " inventaire plein — " + item.getAmount() + "x " + item.getType() + " droppé au sol");
        }
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}