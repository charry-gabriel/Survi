package fr.miuby.survi.item;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Getter
public class CustomRecipe {
    public static List<CustomRecipe> recipes = new ArrayList<>();

    private final ItemStack result;
    private final List<Material> ingredients;
    private final ShapedRecipe recipe;

    public CustomRecipe(NamespacedKey nsKey, CraftingBookCategory category, ItemStack result, List<Material> ingredients) {

        this.ingredients = ingredients;
        this.result = result;

        recipe = new ShapedRecipe(nsKey, result);

        recipe.shape("abc", "def", "ghi");

        for (int i = 0; i < 9; i++) {
            if (ingredients.get(i) == Material.AIR)
                continue;
            recipe.setIngredient((char)(i+'a'), ingredients.get(i));
        }
        recipe.setCategory(category);

        recipes.add(this);
    }

    public static void registerRecipes() {
        for (CustomRecipe cr : recipes) {
            Bukkit.getServer().addRecipe(cr.getRecipe());
        }
    }

     @Nullable
    public static CustomRecipe getCustomRecipe(ItemStack result) {
        for (CustomRecipe cr : recipes) {
            if (cr.getResult().isSimilar(result))
                return cr;
        }
        return null;
    }

}