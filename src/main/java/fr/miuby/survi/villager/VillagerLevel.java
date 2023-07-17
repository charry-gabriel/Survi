package fr.miuby.survi.villager;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.blessing.Blessing;
import fr.miuby.survi.villager.blessing.BlessingEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class VillagerLevel extends AVillager {
    private final Tribute[] tributes;
    private final TextComponent[] names;

    public VillagerLevel(String name, Location location, Villager.Type type, Villager.Profession profession, Blessing[] blessings, TextComponent[] messages, Tribute[] tributes, TextComponent[] names) {
        super(name, location, type, profession, blessings, messages);
        this.tributes = tributes;
        this.names = names;

        getVillager().customName(getName());
        createInventory();
    }

    @Override
    public void giveItems(Inventory inventory, ItemStack item, Player player) {
        removeItemStack(inventory, item, player);

        if (inventory.isEmpty()) {
            Bukkit.broadcast(Component.text("<").color(NamedTextColor.AQUA).append(getName()).append(Component.text("> ").color(NamedTextColor.AQUA)).append(getMessage()));
            applyBlessing();
            addLevel();
            villager.customName(getName());
            updateInventory();
            player.closeInventory();
        }
    }

    public void createInventory() {
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));

        for (ItemStack item : getTribute().getItemStacks())
            inv.addItem(item);

        this.inventory = inv;
    }

    public void updateInventory() {
        this.inventory.clear();

        for (ItemStack item : getTribute().getItemStacks())
            this.inventory.addItem(item);
    }

    public void addLevel() {
        this.level++;
        GameManager.getInstance().getDatabase().updateVillager(uuid, level);
    }

    public void applyAllCurrentBlessing(AlphaPlayer player) {
        for (Blessing blessing : getCurrentBlessings()) {
            for (BlessingEffect effect : blessing.getBlessingEffects()) {
                effect.applyEffect(player);
            }
        }
    }

    public void applyBlessing() {
        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.AMBIENT, 1f, 1.1f);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(myCustomSound);

            for (BlessingEffect effect : getBlessing().getBlessingEffects()) {
                effect.applyEffect(AlphaPlayer.get(player.getUniqueId()));
            }
        }
    }

    public void removeItemStack(Inventory inventory, ItemStack item, Player player) {
        ItemStack itemToRemove = null;

        for (ItemStack tributeItem : inventory.getContents()) {
            if (tributeItem != null && tributeItem.isSimilar(item)) {
                GameManager.getInstance().getLogger().info(getName().content() + " recupere " + item.getAmount() + " de " + item.getType().name());
                if (item.getAmount() < tributeItem.getAmount()) {
                    tributeItem.setAmount(tributeItem.getAmount() - item.getAmount());
                    player.getInventory().remove(item);
                } else {
                    itemToRemove = tributeItem;
                    item.setAmount(item.getAmount() - tributeItem.getAmount());
                }
                break;
            }
        }

        if (itemToRemove != null)
            inventory.removeItem(itemToRemove);
    }


    public Tribute getTribute() {
        return tributes[this.level];
    }

    public Blessing getBlessing() {
        return blessings[this.level];
    }

    public TextComponent getMessage() {
        return messages[this.level].color(NamedTextColor.AQUA);
    }

    @Override
    public TextComponent getName() {
        return names[this.level].color(NamedTextColor.AQUA);
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }
}
