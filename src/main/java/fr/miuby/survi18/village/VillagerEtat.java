package fr.miuby.survi18.village;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class VillagerEtat extends AVillager {
    public VillagerEtat(Location location, String name, Villager.Type type, Villager.Profession profession) {
        super(location, name, type, profession);
    }

    public void Trade(boolean villagerInventory, ItemStack item, Player player){
        ItemEtat itemEtat = getItems().get(item.getType());
        if(itemEtat != null){
            AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId());
            if(villagerInventory && itemEtat.isOnSale()){
                PnjSellTo(itemEtat, alphaPlayer);
            }else if(!villagerInventory && !itemEtat.isOnSale()){
                PnjBuyTo(itemEtat, item.getAmount(), alphaPlayer);
                player.getInventory().removeItem(item);
            }else if(villagerInventory && !itemEtat.isOnSale()){
                player.sendMessage("Cet item n'est pas en vente !");
            }else if(!villagerInventory && itemEtat.isOnSale()){
                player.sendMessage("Cet item n'est pas a vendre !");
            }
        }else {
            player.sendMessage("Cet item n'est pas a vendre !");
        }
    }

    private void PnjSellTo(ItemEtat item, AlphaPlayer player) {
        int sous = item.getPrice();
        if (sous >= player.getCoins()) {
            player.getPlayer().sendMessage("Vous n'avez pas assez d'AlphaCoins pour acheter cet item !");
        } else {
            if(item.getItem().getType().equals(Material.SHIELD)) {
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 4800, 0));
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2400, 0));
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté une bonne nuit à l'auberge pour " + sous + " AlphaCoins !");
            }else if(item.getItem().getType().equals(Material.SUGAR)) {
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 4800, 0));
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 2400, 0));
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté une bonne nuit à l'auberge pour " + sous + " AlphaCoins !");
            }else if(item.getItem().getType().equals(Material.RABBIT_FOOT)) {
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 4800, 0));
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2400, 3));
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté une bonne nuit à l'auberge pour " + sous + " AlphaCoins !");
            }else if(item.getItem().getType().equals(Material.FEATHER)) {
                player.getPlayer().setAllowFlight(true);
                player.getPlayer().setFlying(true);
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté un fly pour " + sous + " AlphaCoins !");
            }else if(item.getItemStack().length > 0){
                for(int loop = 0; loop < item.getItemStack().length; loop++){
                    if(!player.getPlayer().getInventory().contains(item.getItemStack()[loop])){
                        player.getPlayer().sendMessage("Vous n'avez pas les items nécessaires à l'achat !");
                        return;
                    }
                }
                for(int loop = 0; loop < item.getItemStack().length; loop++){
                    player.getPlayer().getInventory().removeItem(item.getItemStack()[loop]);
                }
                ItemStack itemStack = item.getItem();
                ItemMeta meta = itemStack.getItemMeta();
                List<Component> lore = new ArrayList<>();
                meta.lore(lore);

                player.getPlayer().getInventory().addItem(itemStack);
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté un item a l'État !");
            }else {
                ItemStack itemStack = item.getItem();
                ItemMeta meta = itemStack.getItemMeta();
                List<Component> lore = new ArrayList<>();
                meta.lore(lore);

                player.getPlayer().getInventory().addItem(itemStack);
                player.addCoins(-sous);
                player.getPlayer().sendMessage("Vous avez acheté un item a l'État pour " + sous + " AlphaCoins !");
            }
        }
    }

    private void PnjBuyTo(ItemEtat item, int nbr, AlphaPlayer player){
        int sous = item.getPrice();
        player.addCoins(sous * nbr);
        player.getPlayer().sendMessage("Vous avez reçu " + sous * nbr + " AlphaCoins pour votre vente !");
    }
}
