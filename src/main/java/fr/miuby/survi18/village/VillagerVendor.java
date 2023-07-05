package fr.miuby.survi18.village;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.blessing.BlessingEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Objects;

public class VillagerVendor extends VillagerBlessing {
    private final ItemStack[] itemStacks;

    public VillagerVendor(Location location, String name, Villager.Type type, Villager.Profession profession, Blessing[] blessings, Component[] messages, ItemStack[] itemStacks) {
        super(location, name, type, profession, blessings, messages);
        this.itemStacks = itemStacks;
        villager.setMetadata("vendor", new FixedMetadataValue(GameManager.getInstance().getPlugin(), 0));
        updateInventory();
    }

    public void GiveItems(Inventory inventory, ItemStack item, Player player){
        for (ItemStack inventoryItem : inventory.getContents()) {
            if (inventoryItem != null && inventoryItem.getType() == item.getType()) {
                if (item.getAmount() < inventoryItem.getAmount()) {
                    player.sendMessage(Component.text("<" + name + "> Tu n'en as pas assez !").color(NamedTextColor.AQUA));
                } else {
                    GameManager.getInstance().getLogger().info(name + " recupere " + inventoryItem.getAmount() + " de " + item.getType().name());
                    player.sendMessage(Component.text("<" + name + "> " + getMessage(item)).color(NamedTextColor.AQUA));
                    applyBlessing(player, item);
                    item.setAmount(item.getAmount() - inventoryItem.getAmount());
                }
                return;
            }
        }
        player.sendMessage(Component.text("<" + name + "> Je ne veux pas de cet item !").color(NamedTextColor.AQUA));
    }

    public void updateInventory() {
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));

        for (ItemStack item : itemStacks)
            inv.addItem(item);

        this.inventory = inv;
    }

    public void applyBlessing(Player player, ItemStack itemStack) {
        for (BlessingEffect effect : getBlessing(itemStack).getBlessingEffects()) {
            AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId());
            effect.applyEffect(alphaPlayer);
        }
    }



    public Blessing getBlessing(ItemStack itemStack) {
        return blessings[getIndex(itemStack)];
    }

    public Component getMessage(ItemStack itemStack) {
        return messages[getIndex(itemStack)].color(NamedTextColor.AQUA);
    }

    private int getIndex(ItemStack itemStack) {
        for (int i = 0; i < itemStacks.length; i++) {
            if (itemStacks[i].getType() == itemStack.getType())
                return i;
        }
        return -1;
    }
}
