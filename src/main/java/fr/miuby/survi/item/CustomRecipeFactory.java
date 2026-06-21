package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.*;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

@Getter
public class CustomRecipeFactory {
    private final Map<NamespacedKey, CustomRecipe> newRecipes = new HashMap<>();
    private final List<NamespacedKey> oldRecipes = new ArrayList<>();

    // Or craftée à mi-chemin fer/diamant (armure, toughness et durabilité), au lieu des valeurs vanilla (plus faibles que le fer).
    private static final Map<Material, EquipmentSlotGroup> GOLD_ARMOR_SLOTS = Map.of(
            Material.GOLDEN_HELMET, EquipmentSlotGroup.HEAD,
            Material.GOLDEN_CHESTPLATE, EquipmentSlotGroup.CHEST,
            Material.GOLDEN_LEGGINGS, EquipmentSlotGroup.LEGS,
            Material.GOLDEN_BOOTS, EquipmentSlotGroup.FEET
    );
    private static final Map<Material, Double> GOLD_ARMOR_VALUES = Map.of(
            Material.GOLDEN_HELMET, 2.5,
            Material.GOLDEN_CHESTPLATE, 7.0,
            Material.GOLDEN_LEGGINGS, 5.5,
            Material.GOLDEN_BOOTS, 2.5
    );
    private static final double GOLD_ARMOR_TOUGHNESS = 1.0;

    private static final Map<Material, Integer> GOLD_ARMOR_DURABILITY = Map.of(
            Material.GOLDEN_HELMET, 264,
            Material.GOLDEN_CHESTPLATE, 384,
            Material.GOLDEN_LEGGINGS, 360,
            Material.GOLDEN_BOOTS, 312
    );

    private static ItemStack applyGoldenArmorBonus(ItemStack item) {
        EquipmentSlotGroup slot = GOLD_ARMOR_SLOTS.get(item.getType());
        if (slot == null) return item;

        return new CustomItemBuilder(item, "GoldenArmorTierBuff")
                .addAttribute(Attribute.ARMOR, GOLD_ARMOR_VALUES.get(item.getType()), ADD_NUMBER, slot)
                .addAttribute(Attribute.ARMOR_TOUGHNESS, GOLD_ARMOR_TOUGHNESS, ADD_NUMBER, slot)
                .maxDurability(GOLD_ARMOR_DURABILITY.get(item.getType()))
                .build();
    }

    public CustomRecipeFactory() {
        loadRecipes();
    }

    /**
     * Recharge {@code recipes.yml} à chaud : désenregistre les recettes Bukkit actuelles,
     * vide le cache local, relit le YAML, reconstruit les {@link CustomRecipe} puis les
     * réenregistre auprès de Bukkit.
     */
    public void reload() {
        for (CustomRecipe cr : CustomRecipe.recipes) {
            Bukkit.removeRecipe(cr.getRecipe().getKey());
        }
        CustomRecipe.recipes.clear();
        newRecipes.clear();
        oldRecipes.clear();

        loadRecipes();
        CustomRecipe.registerRecipes();
        removeOldRecipes();
    }

    public void removeOldRecipes() {
        for (NamespacedKey nsKey : oldRecipes) {
            Bukkit.removeRecipe(nsKey);
        }
    }

    public List<NamespacedKey> getRecipeKeysForMaterials(List<NamespacedKey> materialKeys) {
        return newRecipes.entrySet().stream()
                .filter(e -> materialKeys.contains(e.getValue().getResult().getType().getKey()))
                .map(Map.Entry::getKey)
                .toList();
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
                    resultItem = applyGoldenArmorBonus(new ItemStack(mat));
                } catch (IllegalArgumentException matEx) {
                    try {
                        // Fallback to custom item enum
                        ECustomItem custom = ECustomItem.valueOf(resultStr);
                        resultItem = custom.getItemStack();
                    } catch (IllegalArgumentException customEx) {
                        MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " : unknown result '" + resultStr + "'. Skipped");
                        continue;
                    }
                }
                List<Material> mats = new ArrayList<>(Collections.nCopies(9, Material.AIR));

                List<String> shapeLines = newSec.getStringList(key + ".shape");
                ConfigurationSection keySec = newSec.getConfigurationSection(key + ".keys");

                if (!shapeLines.isEmpty() && keySec != null) {
                    // parse shape definition
                    if (shapeLines.size() > 3) {
                        MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " has more than 3 shape lines. Skipped");
                        continue;
                    }
                    for (int r = 0; r < shapeLines.size(); r++) {
                        String line = shapeLines.get(r);
                        if (line.length() > 3) {
                            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " shape line too long. Skipped");
                            continue;
                        }
                        for (int c = 0; c < line.length(); c++) {
                            char sym = line.charAt(c);
                            if (sym == ' ') continue;
                            String matStr = keySec.getString(String.valueOf(sym));
                            if (matStr == null) {
                                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " missing key mapping for symbol " + sym + ". Skipped");
                                continue;
                            }
                            Material mat;
                            try {
                                mat = Material.valueOf(matStr);
                            } catch (IllegalArgumentException ex) {
                                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " unknown material " + matStr + ". Skipped");
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
                        MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM, "Recipe " + key + " has not 9 ingredients. Skipped");
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