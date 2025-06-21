package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test that validates the structure of {@code src/main/resources/recipes.yml}.
 * <p>
 * It does NOT require a Bukkit runtime; instead it leverages SnakeYAML to load the
 * YAML file into trivial POJOs and performs a few sanity checks:
 * <ul>
 *     <li>The root object exists and contains at least one new recipe</li>
 *     <li>Each recipe defines category, result and exactly 9 ingredients</li>
 *     <li>No ingredient entry is empty</li>
 *     <li>The old_recipes list (if present) only contains non-empty strings</li>
 * </ul>
 * This mirrors the approach used in {@link VillagerConfigTest}.
 */
public class RecipesConfigTest {
    // --- Helper POJOs matching the YAML structure ---------------------------------------------
    public static class RecipesFile {
        public Map<String, RecipeEntry> new_recipes;
        public List<String> old_recipes;
    }

    public static class RecipeEntry {
        public String category;
        public String result;
        public List<String> roles;
        public List<String> tiers;
        public List<String> categories;
        public List<String> ingredients;
        public List<String> shape;
        public Map<String, String> keys;
    }

    // ------------------------------------------------------------------------------------------
    @Test
    public void testRecipesYaml() {
        File file = new File("src/main/resources/recipes.yml");
        assertTrue(file.exists(), "Le fichier recipes.yml est introuvable");

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(RecipesFile.class, options));

        try (InputStream in = new FileInputStream(file)) {
            RecipesFile wrapper = yaml.load(in);
            assertNotNull(wrapper, "recipes.yml n'a pas pu être chargé");
            assertNotNull(wrapper.new_recipes, "new_recipes manquant dans recipes.yml");
            assertFalse(wrapper.new_recipes.isEmpty(), "new_recipes est vide dans recipes.yml");

            // Validate each new recipe
            wrapper.new_recipes.forEach((key, entry) -> {
                String context = "Recette '" + key + "'";
                assertStringNotEmpty(entry.category, context + " : category manquant");
                assertStringNotEmpty(entry.result, context + " : result manquant");
                
                // Validate roles if present
                if (entry.roles != null) {
                    assertFalse(entry.roles.contains(null), context + " : les rôles ne doivent pas contenir de valeurs nulles");
                    entry.roles.forEach(role ->
                        assertStringNotEmpty(role, context + " : rôle vide trouvé")
                    );
                }
                
                // Validate tiers if present
                if (entry.tiers != null) {
                    assertFalse(entry.tiers.contains(null), context + " : les tiers ne doivent pas contenir de valeurs nulles");
                    entry.tiers.forEach(tier ->
                        assertStringNotEmpty(tier, context + " : tier vide trouvé")
                    );
                }
                
                // Validate categories if present
                if (entry.categories != null) {
                    assertFalse(entry.categories.contains(null), context + " : les catégories ne doivent pas contenir de valeurs nulles");
                    entry.categories.forEach(cat ->
                        assertStringNotEmpty(cat, context + " : catégorie vide trouvée")
                    );
                }

                boolean hasIngredients = entry.ingredients != null;
                boolean hasShapeKeys = entry.shape != null && entry.keys != null;

                assertTrue(hasIngredients || hasShapeKeys, context + " : doit définir soit ingredients soit shape+keys");

                if (hasIngredients) {
                    assertEquals(9, entry.ingredients.size(), context + " : ingredients doit contenir exactement 9 éléments");
                    entry.ingredients.forEach(mat -> assertStringNotEmpty(mat, context + " : ingredient vide"));
                } else {
                    // shape validations
                    assertTrue(!entry.shape.isEmpty() && entry.shape.size() <= 3, context + " : shape doit contenir 1 à 3 lignes");
                    entry.shape.forEach(row -> {
                        assertTrue(!row.isEmpty() && row.length() <= 3, context + " : chaque ligne de shape doit contenir 1 à 3 caractères");
                    });

                    // keys validations
                    assertFalse(entry.keys.isEmpty(), context + " : keys ne doit pas être vide");
                    entry.keys.forEach((sym, mat) -> {
                        assertTrue(sym != null && sym.length() == 1, context + " : chaque clé doit être un caractère unique");
                        assertStringNotEmpty(mat, context + " : material vide pour la clé '" + sym + "'");
                    });

                    // ensure all non-space symbols in shape are present in keys
                    entry.shape.forEach(row -> {
                        row.chars()
                                .filter(c -> c != ' ')
                                .mapToObj(c -> String.valueOf((char) c))
                                .forEach(sym -> assertTrue(entry.keys.containsKey(sym), context + " : symbole '" + sym + "' dans shape non défini dans keys"));
                    });
                }
            });

            if (wrapper.old_recipes != null) {
                wrapper.old_recipes.forEach(id -> assertStringNotEmpty(id, "old_recipes contient une entrée vide"));
            }
        } catch (Exception e) {
            fail("Erreur de parsing YAML : " + e.getMessage());
        }
    }

    private void assertStringNotEmpty(String value, String message) {
        assertTrue(value != null && !value.trim().isEmpty(), message);
    }
}
