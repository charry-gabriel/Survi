package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.*;

public class CustomItemFactory {
    private final Map<NamespacedKey, CustomRecipe> newRecipes = new HashMap<>();
    private final List<NamespacedKey> oldRecipes = new ArrayList<>();

    public CustomItemFactory() {
        NamespacedKey nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "mending_egg");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.MISC, new ItemStack(Material.CHICKEN_SPAWN_EGG), Arrays.asList(
            Material.END_CRYSTAL,
            Material.TORCHFLOWER,
            Material.END_CRYSTAL,
            Material.RED_NETHER_BRICK_SLAB,
            Material.BOOKSHELF,
            Material.RED_NETHER_BRICK_SLAB,
            Material.STRIPPED_MANGROVE_WOOD,
            Material.NETHERITE_INGOT,
            Material.STRIPPED_PALE_OAK_WOOD
        )));

        //region chainmail_armor
        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_helmet");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.CHAINMAIL_HELMET), Arrays.asList(
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LEATHER_HELMET,
                Material.LAVA_BUCKET,
                Material.AIR,
                Material.AIR,
                Material.AIR
        )));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_chestplate");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.CHAINMAIL_CHESTPLATE), Arrays.asList(
                Material.LAVA_BUCKET,
                Material.AIR,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LEATHER_CHESTPLATE,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET
        )));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_leggings");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.CHAINMAIL_LEGGINGS), Arrays.asList(
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.LEATHER_LEGGINGS,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.AIR,
                Material.LAVA_BUCKET
        )));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "chainmail_boots");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.CHAINMAIL_BOOTS), Arrays.asList(
                Material.AIR,
                Material.AIR,
                Material.AIR,
                Material.LAVA_BUCKET,
                Material.LEATHER_BOOTS,
                Material.LAVA_BUCKET,
                Material.LAVA_BUCKET,
                Material.AIR,
                Material.LAVA_BUCKET
        )));
        //endregion

        //region iron_armor
        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_helmet");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.IRON_HELMET), Arrays.asList(
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.CHAINMAIL_HELMET,
                Material.IRON_BLOCK,
                Material.AIR,
                Material.AIR,
                Material.AIR
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:iron_helmet"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_chestplate");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.IRON_CHESTPLATE), Arrays.asList(
                Material.IRON_BLOCK,
                Material.AIR,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:iron_chestplate"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_leggings");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.IRON_LEGGINGS), Arrays.asList(
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.AIR,
                Material.IRON_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:iron_leggings"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "iron_boots");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.IRON_BOOTS), Arrays.asList(
                Material.AIR,
                Material.AIR,
                Material.AIR,
                Material.IRON_BLOCK,
                Material.CHAINMAIL_BOOTS,
                Material.IRON_BLOCK,
                Material.IRON_BLOCK,
                Material.AIR,
                Material.IRON_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:iron_boots"));
        //endregion

        //region gold_armor
        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_helmet");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.GOLDEN_HELMET), Arrays.asList(
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.IRON_HELMET,
                Material.GOLD_BLOCK,
                Material.AIR,
                Material.AIR,
                Material.AIR
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:golden_helmet"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_chestplate");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.GOLDEN_CHESTPLATE), Arrays.asList(
                Material.GOLD_BLOCK,
                Material.AIR,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.IRON_CHESTPLATE,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:golden_chestplate"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_leggings");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.GOLDEN_LEGGINGS), Arrays.asList(
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.IRON_LEGGINGS,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.AIR,
                Material.GOLD_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:golden_leggings"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "golden_boots");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.GOLDEN_BOOTS), Arrays.asList(
                Material.AIR,
                Material.AIR,
                Material.AIR,
                Material.GOLD_BLOCK,
                Material.IRON_BOOTS,
                Material.GOLD_BLOCK,
                Material.GOLD_BLOCK,
                Material.AIR,
                Material.GOLD_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:golden_boots"));
        //endregion

        //region diamond_armor
        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_helmet");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.DIAMOND_HELMET), Arrays.asList(
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.GOLDEN_HELMET,
                Material.DIAMOND_BLOCK,
                Material.AIR,
                Material.AIR,
                Material.AIR
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:diamond_helmet"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_chestplate");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.DIAMOND_CHESTPLATE), Arrays.asList(
                Material.DIAMOND_BLOCK,
                Material.AIR,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.GOLDEN_CHESTPLATE,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:diamond_chestplate"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_leggings");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.DIAMOND_LEGGINGS), Arrays.asList(
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.GOLDEN_LEGGINGS,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.AIR,
                Material.DIAMOND_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:diamond_leggings"));

        nsKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "diamond_boots");
        newRecipes.put(nsKey, new CustomRecipe(nsKey, CraftingBookCategory.EQUIPMENT, new ItemStack(Material.DIAMOND_BOOTS), Arrays.asList(
                Material.AIR,
                Material.AIR,
                Material.AIR,
                Material.DIAMOND_BLOCK,
                Material.GOLDEN_BOOTS,
                Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.AIR,
                Material.DIAMOND_BLOCK
        )));
        oldRecipes.add(NamespacedKey.fromString("minecraft:diamond_boots"));
        //endregion
    }

    public Map<NamespacedKey, CustomRecipe> getNewRecipes() {
        return newRecipes;
    }

    public List<NamespacedKey> getOldRecipes() {
        return oldRecipes;
    }
}
