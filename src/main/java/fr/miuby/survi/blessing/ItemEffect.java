package fr.miuby.survi.blessing;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

@RequiredArgsConstructor
public class ItemEffect extends BlessingEffect {
    private final ItemStack item;

    @Override
    public void applyEffect(AlphaPlayer player) {
        if (player.getPlayer() == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                    "[ItemEffect] " + player.getPseudo() + " hors ligne au moment de l'application, item perdu : "
                            + item.getType() + " x" + item.getAmount());
            return;
        }

        player.getPlayer().getInventory().addItem(item.clone());
        MLLogManager.getInstance().log(Level.FINE, ELogTag.ITEM,
                "[ItemEffect] " + player.getPseudo() + " a reçu " + item.getAmount() + "x " + item.getType());
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}