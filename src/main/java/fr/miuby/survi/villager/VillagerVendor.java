package fr.miuby.survi.villager;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.blessing.Blessing;
import fr.miuby.survi.villager.blessing.BlessingEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class VillagerVendor extends AVillager {
    private final ItemStack[] itemStacks;
    private final String name;
    private final TextComponent openMessage;

    public VillagerVendor(String name, Villager.Type type, Villager.Profession profession, Blessing[] blessings, TextComponent[] messages, ItemStack[] itemStacks, TextComponent openMessage) {
        super(name, type, profession, blessings, messages);
        this.itemStacks = itemStacks;
        this.name = name;
        this.openMessage = openMessage;

        getVillager().customName(getName());
        createInventory();
    }

    @Override
    public void giveItems(Inventory inventory, ItemStack item, Player player){
        for (ItemStack inventoryItem : inventory.getContents()) {
            if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                if (item.getAmount() < inventoryItem.getAmount()) {
                    player.sendMessage(Component.text("<" + name + "> Tu n'en as pas assez !", NamedTextColor.AQUA));
                } else if(player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(Component.text("<" + name + "> Tu es full !", NamedTextColor.AQUA));
                } else {
                    GameManager.getInstance().getLogger().info(name + " recupere " + inventoryItem.getAmount() + " de " + item.getType().name());
                    player.sendMessage(Component.text("<" + name + "> ", NamedTextColor.AQUA).append(getMessage(item)).color(NamedTextColor.AQUA));
                    applyBlessing(player, item);
                    item.setAmount(item.getAmount() - inventoryItem.getAmount());
                }
                return;
            }
        }
        player.sendMessage(Component.text("<" + name + "> Je ne veux pas de cet item !", NamedTextColor.AQUA));
    }

    public void createInventory() {
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));

        for (ItemStack item : itemStacks)
            inv.addItem(item);

        this.inventory = inv;
    }

    @Override
    public TextComponent getName() {
        return Component.text(name, NamedTextColor.AQUA);
    }

    public void applyBlessing(Player player, ItemStack itemStack) {
        for (BlessingEffect effect : getBlessing(itemStack).getBlessingEffects()) {
            effect.applyEffect(AlphaPlayer.get(player.getUniqueId()));
        }
    }

    public Blessing getBlessing(ItemStack itemStack) {
        return blessings[getItemIndex(itemStack)];
    }

    public TextComponent getMessage(ItemStack itemStack) {
        return messages[getItemIndex(itemStack)].color(NamedTextColor.AQUA);
    }

    public TextComponent getOpenMessage() {
        return openMessage;
    }

    private int getItemIndex(ItemStack itemStack) {
        for (int i = 0; i < itemStacks.length; i++) {
            if (itemStacks[i].isSimilar(itemStack))
                return i;
        }
        return -1;
    }
}
