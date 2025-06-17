package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.*;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomRecipeFactory {
    private final Map<NamespacedKey, CustomRecipe> newRecipes = new HashMap<>();
    private final List<NamespacedKey> oldRecipes = new ArrayList<>();

    public CustomRecipeFactory() {
        loadRecipes();
    }

    public Map<NamespacedKey, CustomRecipe> getNewRecipes() {
        return newRecipes;
    }

    public List<NamespacedKey> getOldRecipes() {
        return oldRecipes;
    }

    public NamespacedKey getOldNamespaceKeyOrDefault(NamespacedKey newNsKey) {
        for (Map.Entry<NamespacedKey, CustomRecipe> custom : newRecipes.entrySet()) {
            if (custom.getKey().toString().equals(newNsKey.toString()))
                return custom.getValue().getResult().getType().getKey();
        }
        return newNsKey;
    }

    private void loadRecipes() {
        JavaPlugin plugin = GameManager.getInstance().getPlugin();
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection newSec = cfg.getConfigurationSection("new_recipes");
        if (newSec != null) {
            for (String key : newSec.getKeys(false)) {
                NamespacedKey nsKey = new NamespacedKey(plugin, key);
                String catStr = newSec.getString(key + ".category", "MISC");
                CraftingBookCategory category = CraftingBookCategory.valueOf(catStr);
                Material resultMat = Material.valueOf(newSec.getString(key + ".result"));
                List<String> list = newSec.getStringList(key + ".ingredients");
                if (list.size() != 9) {
                    plugin.getLogger().warning("Recipe " + key + " has not 9 ingredients. Skipped");
                    continue;
                }
                List<Material> mats = new ArrayList<>();
                for (String s : list) {
                    mats.add(Material.valueOf(s));
                }
                newRecipes.put(nsKey, new CustomRecipe(nsKey, category, new ItemStack(resultMat), mats));
            }
        }

        List<String> oldList = cfg.getStringList("old_recipes");
        for (String s : oldList) {
            oldRecipes.add(NamespacedKey.fromString(s));
        }
    }
}
