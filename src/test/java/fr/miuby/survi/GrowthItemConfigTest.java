package fr.miuby.survi;

import fr.miuby.survi.item.growth_item.config.GrowthItemFileConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la structure de tous les fichiers {@code src/main/resources/growth_items/*.yml}
 * sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Le dossier {@code growth_items/} existe et contient au moins un fichier.</li>
 *   <li>Chaque fichier est parseable en {@link GrowthItemFileConfig} via SnakeYAML.</li>
 *   <li>Champs obligatoires au niveau racine : {@code id}, {@code eventType}, {@code tiers}.</li>
 *   <li>{@code eventType} appartient à la liste des valeurs reconnues par
 *       {@code GrowthItemListener}.</li>
 *   <li>Chaque palier ({@code tiers}) : {@code requiredUses >= 1}, liste d'effets non vide,
 *       paliers ordonnés de façon croissante.</li>
 *   <li>Chaque effet périodique ({@code periodicEffects}) : {@code everyUses >= 1}.</li>
 *   <li>Pour chaque effet : {@code type} valide, champs requis selon le type présents et
 *       cohérents (ex. {@code seconds >= 1} pour {@code haste}).</li>
 * </ul>
 */
class GrowthItemConfigTest {

    // ─── Constantes de validation ─────────────────────────────────────────────────

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            "BlockBreakEvent", "OreBreakEvent", "CropBreakEvent"
    );

    private static final Set<String> VALID_EFFECT_TYPES = Set.of(
            "name", "message", "haste", "add_enchantment", "set_attribute"
    );

    /** Doit rester en sync avec le switch de GrowthItemLoader.parseAttribute(). */
    private static final Set<String> VALID_ATTRIBUTES = Set.of(
            "mining_efficiency", "movement_speed", "attack_damage", "attack_speed",
            "armor", "armor_toughness", "max_health", "block_break_speed",
            "luck", "knockback_resistance"
    );

    private static final Set<String> VALID_OPERATIONS = Set.of(
            "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1"
    );

    private static final Set<String> VALID_SLOTS = Set.of(
            "HEAD", "CHEST", "LEGS", "FEET", "HAND", "OFF_HAND", "ARMOR", "ANY"
    );

    // ─── Test principal ───────────────────────────────────────────────────────────

    @Test
    void testAllGrowthItemConfigs() {
        File folder = new File("src/main/resources/growth_items");
        assertTrue(folder.exists(), "Le dossier growth_items/ est introuvable dans src/main/resources/");

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files, "Impossible de lister growth_items/");
        assertTrue(files.length > 0, "Aucun fichier YAML trouvé dans growth_items/");

        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(GrowthItemFileConfig.class, options));

        for (File file : files) {
            try (InputStream in = new FileInputStream(file)) {
                GrowthItemFileConfig config = yaml.load(in);
                validateRoot(config, file.getName());
                validateTiers(config, file.getName());
                validatePeriodicEffects(config, file.getName());
            } catch (Exception e) {
                fail("Erreur de parsing YAML dans " + file.getName() + " : " + e.getMessage());
            }
        }
    }

    // ─── Validation racine ────────────────────────────────────────────────────────

    private void validateRoot(GrowthItemFileConfig config, String filename) {
        assertNotNull(config, filename + " : fichier vide ou non parseable");

        assertStringNotEmpty(config.id,
                filename + " : champ 'id' manquant ou vide");

        assertStringNotEmpty(config.eventType,
                filename + " : champ 'eventType' manquant ou vide");

        assertTrue(VALID_EVENT_TYPES.contains(config.eventType),
                filename + " : eventType invalide '" + config.eventType + "'. "
                        + "Valeurs valides : " + VALID_EVENT_TYPES);

        assertNotNull(config.tiers,
                filename + " : champ 'tiers' manquant");

        assertFalse(config.tiers.isEmpty(),
                filename + " : 'tiers' doit contenir au moins un palier");
    }

    // ─── Validation des paliers ───────────────────────────────────────────────────

    private void validateTiers(GrowthItemFileConfig config, String filename) {
        int previousUses = 0;
        for (int i = 0; i < config.tiers.size(); i++) {
            GrowthItemFileConfig.TierConfig tier = config.tiers.get(i);
            String ctx = filename + " tiers[" + i + "]";

            assertNotNull(tier, ctx + " : palier null");

            assertTrue(tier.requiredUses >= 1,
                    ctx + " : requiredUses doit être >= 1 (valeur : " + tier.requiredUses + ")");

            assertTrue(tier.requiredUses > previousUses,
                    ctx + " : requiredUses (" + tier.requiredUses + ") doit être strictement supérieur "
                            + "au palier précédent (" + previousUses + ") — les paliers doivent être croissants");

            assertNotNull(tier.effects, ctx + " : 'effects' manquant");
            assertFalse(tier.effects.isEmpty(), ctx + " : 'effects' ne doit pas être vide");

            for (int j = 0; j < tier.effects.size(); j++) {
                validateEffect(tier.effects.get(j), ctx + " effects[" + j + "]");
            }

            previousUses = tier.requiredUses;
        }
    }

    // ─── Validation des effets périodiques ───────────────────────────────────────

    private void validatePeriodicEffects(GrowthItemFileConfig config, String filename) {
        if (config.periodicEffects == null || config.periodicEffects.isEmpty()) return;

        for (int i = 0; i < config.periodicEffects.size(); i++) {
            GrowthItemFileConfig.PeriodicConfig periodic = config.periodicEffects.get(i);
            String ctx = filename + " periodicEffects[" + i + "]";

            assertNotNull(periodic, ctx + " : entrée null");

            assertTrue(periodic.everyUses >= 1,
                    ctx + " : everyUses doit être >= 1 (valeur : " + periodic.everyUses + ")");

            assertNotNull(periodic.effects, ctx + " : 'effects' manquant");
            assertFalse(periodic.effects.isEmpty(), ctx + " : 'effects' ne doit pas être vide");

            for (int j = 0; j < periodic.effects.size(); j++) {
                validateEffect(periodic.effects.get(j), ctx + " effects[" + j + "]");
            }
        }
    }

    // ─── Validation d'un effet individuel ────────────────────────────────────────

    private void validateEffect(GrowthItemFileConfig.EffectConfig effect, String ctx) {
        assertNotNull(effect, ctx + " : effet null");

        assertStringNotEmpty(effect.type, ctx + " : champ 'type' manquant ou vide");

        assertTrue(VALID_EFFECT_TYPES.contains(effect.type),
                ctx + " : type d'effet invalide '" + effect.type + "'. "
                        + "Valeurs valides : " + VALID_EFFECT_TYPES);

        switch (effect.type) {

            case "name", "message" ->
                    assertStringNotEmpty(effect.value,
                            ctx + " : champ 'value' requis pour type=" + effect.type);

            case "haste" ->
                    assertTrue(effect.seconds >= 1,
                            ctx + " : 'seconds' doit être >= 1 (valeur : " + effect.seconds + ")");

            case "add_enchantment" -> {
                assertStringNotEmpty(effect.enchantment,
                        ctx + " : champ 'enchantment' requis pour type=add_enchantment");
                assertTrue(effect.amount >= 1,
                        ctx + " : 'amount' doit être >= 1 (valeur : " + effect.amount + ")");
            }

            case "set_attribute" -> {
                assertStringNotEmpty(effect.attribute,
                        ctx + " : champ 'attribute' requis pour type=set_attribute");

                assertTrue(VALID_ATTRIBUTES.contains(effect.attribute.toLowerCase()),
                        ctx + " : attribut inconnu '" + effect.attribute + "'. "
                                + "Ajouter le case dans GrowthItemLoader.parseAttribute() si légitime. "
                                + "Attributs connus : " + VALID_ATTRIBUTES);

                assertStringNotEmpty(effect.operation,
                        ctx + " : champ 'operation' requis pour type=set_attribute");

                assertTrue(VALID_OPERATIONS.contains(effect.operation.toUpperCase()),
                        ctx + " : opération inconnue '" + effect.operation + "'. "
                                + "Valeurs valides : " + VALID_OPERATIONS);

                assertStringNotEmpty(effect.slot,
                        ctx + " : champ 'slot' requis pour type=set_attribute");

                assertTrue(VALID_SLOTS.contains(effect.slot.toUpperCase()),
                        ctx + " : slot inconnu '" + effect.slot + "'. "
                                + "Valeurs valides : " + VALID_SLOTS);
            }
        }
    }

    // ─── Utilitaire ──────────────────────────────────────────────────────────────

    private void assertStringNotEmpty(String value, String message) {
        assertTrue(value != null && !value.trim().isEmpty(), message);
    }
}