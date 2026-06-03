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

class GrowthItemConfigTest {

    // ─── Constantes de validation ─────────────────────────────────────────────

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            "BlockBreakEvent",
            "OreBreakEvent",
            "CropBreakEvent",
            "NewBiomeEvent",        // GROWTH_BOUSSOLE_AVENTURIER
            "NewMobTypeKillEvent"   // GROWTH_EPEE_SHINY
    );

    private static final Set<String> VALID_EFFECT_TYPES = Set.of(
            "name",
            "message",
            "haste",
            "potion",           // PotionItemEffect — effect + seconds + amplifier
            "add_enchantment",
            "set_attribute"
    );

    /** Doit rester en sync avec GrowthItemLoader.parseAttribute(). */
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

    // ─── Test principal ───────────────────────────────────────────────────────

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

    // ─── Racine ───────────────────────────────────────────────────────────────

    private void validateRoot(GrowthItemFileConfig config, String filename) {
        assertNotNull(config, filename + " : fichier vide ou non parseable");
        assertStringNotEmpty(config.id, filename + " : 'id' manquant");
        assertStringNotEmpty(config.eventType, filename + " : 'eventType' manquant");
        assertTrue(VALID_EVENT_TYPES.contains(config.eventType),
                filename + " : eventType invalide '" + config.eventType + "'. Valides : " + VALID_EVENT_TYPES);
        assertNotNull(config.tiers, filename + " : 'tiers' manquant");
        assertFalse(config.tiers.isEmpty(), filename + " : 'tiers' vide");
    }

    // ─── Paliers ──────────────────────────────────────────────────────────────

    private void validateTiers(GrowthItemFileConfig config, String filename) {
        int previousUses = 0;
        for (int i = 0; i < config.tiers.size(); i++) {
            GrowthItemFileConfig.TierConfig tier = config.tiers.get(i);
            String ctx = filename + " tiers[" + i + "]";
            assertNotNull(tier, ctx + " : palier null");
            assertTrue(tier.requiredUses >= 1, ctx + " : requiredUses doit être >= 1");
            assertTrue(tier.requiredUses > previousUses,
                    ctx + " : requiredUses (" + tier.requiredUses + ") doit être > au palier précédent ("
                            + previousUses + ") — paliers non croissants");
            assertNotNull(tier.effects, ctx + " : 'effects' manquant");
            assertFalse(tier.effects.isEmpty(), ctx + " : 'effects' vide");
            for (int j = 0; j < tier.effects.size(); j++)
                validateEffect(tier.effects.get(j), ctx + " effects[" + j + "]");
            previousUses = tier.requiredUses;
        }
    }

    // ─── Effets périodiques ───────────────────────────────────────────────────

    private void validatePeriodicEffects(GrowthItemFileConfig config, String filename) {
        if (config.periodicEffects == null || config.periodicEffects.isEmpty()) return;
        for (int i = 0; i < config.periodicEffects.size(); i++) {
            GrowthItemFileConfig.PeriodicConfig p = config.periodicEffects.get(i);
            String ctx = filename + " periodicEffects[" + i + "]";
            assertNotNull(p, ctx + " : entrée null");
            assertTrue(p.everyUses >= 1, ctx + " : everyUses doit être >= 1");
            assertNotNull(p.effects, ctx + " : 'effects' manquant");
            assertFalse(p.effects.isEmpty(), ctx + " : 'effects' vide");
            for (int j = 0; j < p.effects.size(); j++)
                validateEffect(p.effects.get(j), ctx + " effects[" + j + "]");
        }
    }

    // ─── Effet individuel ─────────────────────────────────────────────────────

    private void validateEffect(GrowthItemFileConfig.EffectConfig effect, String ctx) {
        assertNotNull(effect, ctx + " : effet null");
        assertStringNotEmpty(effect.type, ctx + " : 'type' manquant");
        assertTrue(VALID_EFFECT_TYPES.contains(effect.type),
                ctx + " : type invalide '" + effect.type + "'. Valides : " + VALID_EFFECT_TYPES);

        switch (effect.type) {

            case "name", "message" ->
                    assertStringNotEmpty(effect.value, ctx + " : 'value' requis pour type=" + effect.type);

            case "haste" ->
                    assertTrue(effect.seconds >= 1, ctx + " : 'seconds' doit être >= 1");

            case "potion" -> {
                assertStringNotEmpty(effect.effect,
                        ctx + " : 'effect' requis pour type=potion (ex. speed, strength, night_vision)");
                assertTrue(effect.seconds >= 1,
                        ctx + " : 'seconds' doit être >= 1");
                assertTrue(effect.amplifier >= 0,
                        ctx + " : 'amplifier' doit être >= 0 (0 = niveau I, 1 = niveau II…)");
            }

            case "add_enchantment" -> {
                assertStringNotEmpty(effect.enchantment, ctx + " : 'enchantment' requis");
                assertTrue(effect.amount >= 1, ctx + " : 'amount' doit être >= 1");
            }

            case "set_attribute" -> {
                assertStringNotEmpty(effect.attribute, ctx + " : 'attribute' requis");
                assertTrue(VALID_ATTRIBUTES.contains(effect.attribute.toLowerCase()),
                        ctx + " : attribut inconnu '" + effect.attribute + "'. "
                                + "Ajouter le case dans GrowthItemLoader.parseAttribute() si légitime. "
                                + "Connus : " + VALID_ATTRIBUTES);
                assertStringNotEmpty(effect.operation, ctx + " : 'operation' requis");
                assertTrue(VALID_OPERATIONS.contains(effect.operation.toUpperCase()),
                        ctx + " : opération inconnue '" + effect.operation + "'. Valides : " + VALID_OPERATIONS);
                assertStringNotEmpty(effect.slot, ctx + " : 'slot' requis");
                assertTrue(VALID_SLOTS.contains(effect.slot.toUpperCase()),
                        ctx + " : slot inconnu '" + effect.slot + "'. Valides : " + VALID_SLOTS);
            }
        }
    }

    // ─── Utilitaire ──────────────────────────────────────────────────────────

    private void assertStringNotEmpty(String value, String message) {
        assertTrue(value != null && !value.trim().isEmpty(), message);
    }
}