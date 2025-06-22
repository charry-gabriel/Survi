package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.*;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CustomRecipeFactory {
    private final Map<NamespacedKey, CustomRecipe> newRecipes = new HashMap<>();
    private final List<NamespacedKey> oldRecipes = new ArrayList<>();

    public CustomRecipeFactory() {
        loadRecipes();
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
                String resultStr = newSec.getString(key + ".result");
                List<String> roles = newSec.getStringList(key + ".roles");
                List<String> tiers = newSec.getStringList(key + ".tiers");
                List<String> categoryTypes = newSec.getStringList(key + ".categories");
                
                ItemStack resultItem;
                try {
                    // Try vanilla Material first
                    Material mat = Material.valueOf(resultStr);
                    resultItem = new ItemStack(mat);
                } catch (IllegalArgumentException matEx) {
                    try {
                        // Fallback to custom item enum
                        ECustomItem custom = ECustomItem.valueOf(resultStr);
                        resultItem = custom.getItemStack();
                    } catch (IllegalArgumentException customEx) {
                        plugin.getLogger().warning("Recipe " + key + " : unknown result '" + resultStr + "'. Skipped");
                        continue;
                    }
                }
                List<Material> mats = new ArrayList<>(Collections.nCopies(9, Material.AIR));

                List<String> shapeLines = newSec.getStringList(key + ".shape");
                ConfigurationSection keySec = newSec.getConfigurationSection(key + ".keys");

                if (!shapeLines.isEmpty() && keySec != null) {
                    // parse shape definition
                    if (shapeLines.size() > 3) {
                        plugin.getLogger().warning("Recipe " + key + " has more than 3 shape lines. Skipped");
                        continue;
                    }
                    for (int r = 0; r < shapeLines.size(); r++) {
                        String line = shapeLines.get(r);
                        if (line.length() > 3) {
                            plugin.getLogger().warning("Recipe " + key + " shape line too long. Skipped");
                            continue;
                        }
                        for (int c = 0; c < line.length(); c++) {
                            char sym = line.charAt(c);
                            if (sym == ' ') continue;
                            String matStr = keySec.getString(String.valueOf(sym));
                            if (matStr == null) {
                                plugin.getLogger().warning("Recipe " + key + " missing key mapping for symbol " + sym + ". Skipped");
                                continue;
                            }
                            Material mat;
                            try {
                                mat = Material.valueOf(matStr);
                            } catch (IllegalArgumentException ex) {
                                plugin.getLogger().warning("Recipe " + key + " unknown material " + matStr + ". Skipped");
                                continue;
                            }
                            int index = r * 3 + c;
                            mats.set(index, mat);
                        }
                    }
                } else {
                    // fallback to old 9-element list
                    List<String> list = newSec.getStringList(key + ".ingredients");
                    if (list.size() != 9) {
                        plugin.getLogger().warning("Recipe " + key + " has not 9 ingredients. Skipped");
                        continue;
                    }
                    for (int i = 0; i < 9; i++) {
                        mats.set(i, Material.valueOf(list.get(i)));
                    }
                }
                newRecipes.put(nsKey, new CustomRecipe(nsKey, category, resultItem, mats, roles, tiers, categoryTypes));
            }
        }

        List<String> oldList = cfg.getStringList("old_recipes");
        for (String s : oldList) {
            oldRecipes.add(NamespacedKey.fromString(s));
        }
    }
}
