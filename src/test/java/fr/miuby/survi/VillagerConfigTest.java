package fr.miuby.survi;

import fr.miuby.survi.villager.villagerlevel.VillagerLevelConfig;
import fr.miuby.survi.villager.VillagerConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VillagerConfigTest {

    @Test
    void testAllVillagerConfigs() {
        File folder = new File("src/main/resources/villagers");
        assertTrue(folder.exists(), "Le dossier villagers/ est introuvable");

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files);
        assertTrue(files.length > 0, "Aucun fichier YAML trouvé dans villagers/");

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(VillagerConfig.class, options));

        for (File file : files) {
            try (InputStream in = new FileInputStream(file)) {

                VillagerConfig config = yaml.load(in);
                assertStringNotEmpty(config.name, "name manquant dans " + file.getName());
                assertStringNotEmpty(config.type, "type manquant dans " + file.getName());
                assertStringNotEmpty(config.profession, "profession manquant dans " + file.getName());
                assertNotNull(config.levels, "levels manquant dans " + file.getName());

                for (int i = 0; i < config.levels.size(); i++) {
                    VillagerLevelConfig level = config.levels.get(i);
                    assertStringNotEmpty(level.name, file.getName() + " : level[" + i + "] name manquant");
                    assertNotNull(level.message, file.getName() + " : level[" + i + "] message manquant");
                    assertNotNull(level.recap, file.getName() + " : level[" + i + "] recap manquant");
                    assertNotNull(level.tribute, file.getName() + " : level[" + i + "] tribute manquant");

                    // Validation des blessings si présents
                    if (level.blessings != null) {
                        assertFalse(level.blessings.isEmpty(),
                                file.getName() + " : level[" + i + "] blessings est une liste vide (supprimer la clé si vide)");

                        for (int j = 0; j < level.blessings.size(); j++) {
                            Map<String, Object> effect = level.blessings.get(j);
                            assertNotNull(effect, file.getName() + " : level[" + i + "] blessing[" + j + "] est null");
                            assertTrue(effect.containsKey("type"),
                                    file.getName() + " : level[" + i + "] blessing[" + j + "] manque le champ 'type'");
                            Object type = effect.get("type");
                            assertNotNull(type, file.getName() + " : level[" + i + "] blessing[" + j + "] type est null");
                            assertFalse(type.toString().isBlank(),
                                    file.getName() + " : level[" + i + "] blessing[" + j + "] type est vide");
                        }
                    }
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
