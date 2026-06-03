package fr.miuby.survi;

import fr.miuby.survi.villager.trader.TraderConfig;
import fr.miuby.survi.villager.trader.TraderRecipeConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TraderConfigTest {

    @Test
    void testAllTraderConfigs() {
        File folder = new File("src/main/resources/traders");
        assertTrue(folder.exists(), "Le dossier traders/ est introuvable");

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files);
        assertTrue(files.length > 0, "Aucun fichier YAML trouvé dans traders/");

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(TraderConfig.class, options));

        for (File file : files) {
            try (InputStream in = new FileInputStream(file)) {

                TraderConfig config = yaml.load(in);
                assertStringNotEmpty(config.nameId, "nameId manquant dans " + file.getName());
                assertStringNotEmpty(config.displayName, "displayName manquant dans " + file.getName());
                assertStringNotEmpty(config.type, "type manquant dans " + file.getName());
                assertStringNotEmpty(config.profession, "profession manquant dans " + file.getName());
                assertNotNull(config.openMessage, "openMessage manquant dans " + file.getName());
                assertNotNull(config.recipes, "recipes manquant dans " + file.getName());

                for (int i = 0; i < config.recipes.size(); i++) {
                    TraderRecipeConfig recipe = config.recipes.get(i);
                    assertNotNull(recipe.result, file.getName() + " : recipe[" + i + "] result manquant");
                    assertNotNull(recipe.ingredient, file.getName() + " : recipe[" + i + "] ingredient manquant");
                    assertTrue(recipe.requiredReputation >= 0, file.getName() + " : recipe[" + i + "] requiredReputation invalide");
                }
            } catch (Exception e) {
                fail("Erreur de parsing YAML dans " + file.getName() + " : " + e.getMessage());
            }
        }
    }

    private void assertStringNotEmpty(String value, String message) {
        assertTrue(value != null && !value.trim().isEmpty(), message);
    }
}
