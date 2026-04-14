package fr.miuby.survi.item;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public class CustomRecipe {
    public static List<CustomRecipe> recipes = new ArrayList<>();

    private final ItemStack result;
    private final List<Material> ingredients;
    private final ShapedRecipe recipe;
    private final List<String> roles;
    private final List<String> tiers;
    private final List<String> categories;

    public CustomRecipe(NamespacedKey nsKey, CraftingBookCategory category, ItemStack result, List<Material> ingredients, List<String> roles, List<String> tiers, List<String> categories) {
        this.ingredients = ingredients;
        this.result = result;
        this.roles = roles;
        this.tiers = tiers;
        this.categories = categories;

        recipe = new ShapedRecipe(nsKey, result);

        // Build the minimal bounding box that contains every non-empty ingredient
        int minRow = 3, minCol = 3, maxRow = -1, maxCol = -1;
        for (int i = 0; i < 9; i++) {
            if (ingredients.get(i) == Material.AIR)
                continue;

            int row = i / 3;
            int col = i % 3;
            if (row < minRow) minRow = row;
            if (col < minCol) minCol = col;
            if (row > maxRow) maxRow = row;
            if (col > maxCol) maxCol = col;
        }
        // A recipe consisting only of AIR does not make sense
        if (maxRow == -1) {
            throw new IllegalArgumentException("CustomRecipe must contain at least one ingredient");
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        StringBuilder[] rows = new StringBuilder[height];
        for (int r = 0; r < height; r++) {
            rows[r] = new StringBuilder(" ".repeat(width));
        }

        // Fill rows with ingredient symbols (but register them later)
        List<Character> symbols = new ArrayList<>();
        List<Material> matsToRegister = new ArrayList<>();
        // Re-use a symbol for identical materials to minimise distinct symbols
        Map<Material, Character> materialSymbols = new HashMap<>();
        char nextSymbol = 'a';
        for (int i = 0; i < 9; i++) {
            Material mat = ingredients.get(i);
            if (mat == Material.AIR)
                continue;

            int globalRow = i / 3;
            int globalCol = i % 3;
            int r = globalRow - minRow;
            int c = globalCol - minCol;

            char symbol;
            if (materialSymbols.containsKey(mat)) {
                symbol = materialSymbols.get(mat);
            } else {
                symbol = nextSymbol;
                materialSymbols.put(mat, nextSymbol);
                nextSymbol++;
            }
            rows[r].setCharAt(c, symbol);

            // Register only once per distinct symbol
            if (!symbols.contains(symbol)) {
                symbols.add(symbol);
                matsToRegister.add(mat);
            }
        }

        String[] shapeRows = new String[height];
        for (int r = 0; r < height; r++) {
            shapeRows[r] = rows[r].toString();
        }
        recipe.shape(shapeRows);
        
        // Now register ingredients (shape is already defined)
        for (int idx = 0; idx < symbols.size(); idx++) {
            recipe.setIngredient(symbols.get(idx), matsToRegister.get(idx));
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