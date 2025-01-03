package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class CustomItemFactory {

    public CustomItemFactory() {
        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "chicken_mending"), new ItemStack(Material.CHICKEN_SPAWN_EGG), Arrays.asList(
            new ItemStack(Material.END_CRYSTAL),
            new ItemStack(Material.TORCHFLOWER),
            new ItemStack(Material.END_CRYSTAL),
            new ItemStack(Material.RED_NETHER_BRICK_SLAB),
            new ItemStack(Material.BOOKSHELF),
            new ItemStack(Material.RED_NETHER_BRICK_SLAB),
            new ItemStack(Material.STRIPPED_MANGROVE_WOOD),
            new ItemStack(Material.NETHERITE_INGOT),
            new ItemStack(Material.STRIPPED_PALE_OAK_WOOD)
        ));

        //region chainmail_armor
        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_helmet"), new ItemStack(Material.CHAINMAIL_HELMET), Arrays.asList(
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LEATHER_HELMET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_chestplate"), new ItemStack(Material.CHAINMAIL_CHESTPLATE), Arrays.asList(
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.AIR),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_leggings"), new ItemStack(Material.CHAINMAIL_LEGGINGS), Arrays.asList(
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.AIR),
                new ItemStack(Material.LAVA_BUCKET)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_boots"), new ItemStack(Material.CHAINMAIL_BOOTS), Arrays.asList(
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.LAVA_BUCKET),
                new ItemStack(Material.AIR),
                new ItemStack(Material.LAVA_BUCKET)
        ));
        //endregion

        //region iron_armor
        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_helmet"), new ItemStack(Material.IRON_HELMET), Arrays.asList(
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.CHAINMAIL_HELMET),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_chestplate"), new ItemStack(Material.IRON_CHESTPLATE), Arrays.asList(
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_leggings"), new ItemStack(Material.IRON_LEGGINGS), Arrays.asList(
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.IRON_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_boots"), new ItemStack(Material.IRON_BOOTS), Arrays.asList(
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.IRON_BLOCK)
        ));
        //endregion

        //region gold_armor
        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_helmet"), new ItemStack(Material.GOLDEN_HELMET), Arrays.asList(
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.IRON_HELMET),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_chestplate"), new ItemStack(Material.GOLDEN_CHESTPLATE), Arrays.asList(
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_leggings"), new ItemStack(Material.GOLDEN_LEGGINGS), Arrays.asList(
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.GOLD_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_boots"), new ItemStack(Material.GOLDEN_BOOTS), Arrays.asList(
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.GOLD_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.GOLD_BLOCK)
        ));
        //endregion

        //region diamond_armor
        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_helmet"), new ItemStack(Material.DIAMOND_HELMET), Arrays.asList(
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.GOLDEN_HELMET),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_chestplate"), new ItemStack(Material.DIAMOND_CHESTPLATE), Arrays.asList(
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.GOLDEN_CHESTPLATE),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_leggings"), new ItemStack(Material.DIAMOND_LEGGINGS), Arrays.asList(
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.GOLDEN_LEGGINGS),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.DIAMOND_BLOCK)
        ));

        new CustomRecipe(new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_boots"), new ItemStack(Material.DIAMOND_BOOTS), Arrays.asList(
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.GOLDEN_BOOTS),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.AIR),
                new ItemStack(Material.DIAMOND_BLOCK)
        ));
        //endregion
    }
}
