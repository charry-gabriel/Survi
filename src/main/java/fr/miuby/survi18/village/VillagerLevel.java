package fr.miuby.survi18.village;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.blessing.BlessingEffect;
import net.kyori.adventure.text.Component;
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
    private int level = 0;
    private final Tribute[] tributes;
    private final Blessing[] blessings;
    private final Component[] messages;
    private final Component[] names;

    public VillagerLevel(Location location, String name, Villager.Type type, Villager.Profession profession, Tribute[] tributes, Blessing[] blessings, Component[] messages, Component[] names) {
        super(location, name, type, profession);
        this.tributes = tributes;
        this.blessings = blessings;
        this.messages = messages;
        this.names = names;
    }

    public void GiveItems(ItemStack item, Player player){
        //TODO verification
        player.getInventory().removeItem(item);

        Bukkit.broadcast(getMessage());
        applyBlessing();
        addLevel();
        villager.customName(getName());
        updateInventory();
    }

    public void SetLevel(int level) {
        this.level = level;
    }

    public void updateInventory() {
        //float size = items.size() / 9f;
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));//(int) Math.ceil(size) * 9

        for (ItemStack item : getTribute().getItemStacks())
            inv.addItem(item);

        this.inventory = inv;
    }

    public void addLevel() {
        this.level++;
        GameManager.getInstance().getDatabaseManager().updateVillager(name, level);
    }

    public Tribute getTribute() {
        return tributes[this.level];
    }

    public Blessing getBlessing() {
        return blessings[this.level];
    }

    public Component getMessage() {
        return messages[this.level];
    }

    public Component getName() {
        return names[this.level];
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }

    public void ApplyAllCurrentBlessing(AlphaPlayer player) {
        for (Blessing blessing : getCurrentBlessings()) {
            for (BlessingEffect effect : blessing.getBlessingEffects()) {
                effect.applyEffect(player);
            }
        }
    }

    public void applyBlessing() {
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayers().values()) {
            for (BlessingEffect effect : getBlessing().getBlessingEffects()) {
                effect.applyEffect(player);
            }
        }
    }
}
