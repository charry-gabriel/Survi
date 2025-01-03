package fr.miuby.survi.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class CustomRecipe {

    public static List<CustomRecipe> recipes = new ArrayList<>();

    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private final ShapedRecipe recipe;

    public CustomRecipe(NamespacedKey nsKey, ItemStack result, List<ItemStack> ingredients) {

        this.ingredients = ingredients;
        this.result = result;

        recipe = new ShapedRecipe(nsKey, result);

        recipe.shape("abc", "def", "ghi");

        for (int i = 0; i < 9; i++) {
            if (ingredients.get(i) == null || ingredients.get(i).getType() == Material.AIR)
                continue;
            recipe.setIngredient((char)(i+'a'), ingredients.get(i).getType());
        }

        recipes.add(this);
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public ShapedRecipe getRecipe() {
        return recipe;
    }

    public static void registerRecipes() {
        for (CustomRecipe cr : recipes) {
            Bukkit.getServer().addRecipe(cr.getRecipe());
        }
    }

    public static CustomRecipe getCustomRecipe(ItemStack result) {
        for (CustomRecipe cr : recipes) {
            if (cr.getResult().isSimilar(result))
                return cr;
        }
        return null;
    }

}