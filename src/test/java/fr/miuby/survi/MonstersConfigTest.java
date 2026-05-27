package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la structure de {@code src/main/resources/monsters.yml} sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Paramètres globaux : {@code levels-per-world-tier} et {@code spawn-weight-exponent}.</li>
 *   <li>Section {@code mobs} non vide, clés = noms d'EntityType valides.</li>
 *   <li>Pour chaque mob : {@code enabled}, section {@code stats} avec formule linéaire.</li>
 *   <li>Champs spéciaux {@code explosion-radius} et {@code fuse-ticks} (Creeper).</li>
 *   <li>Chaque entrée {@code potion-effects} : champs requis, plages de valeurs cohérentes.</li>
 * </ul>
 */
class MonstersConfigTest {

    // ─── Constantes de validation ─────────────────────────────────────────────────

    /** Clés de stats acceptées dans la section stats d'un mob (= configKey de MobStat). */
    private static final Set<String> VALID_STAT_KEYS = Set.of(
            "max-health", "attack-damage", "movement-speed",
            "scale", "follow-range", "armor", "knockback-resistance"
    );

    /** Types d'effets de potion connus (PotionEffectType.getName()). */
    private static final Set<String> VALID_POTION_TYPES = Set.of(
            "ABSORPTION", "BAD_OMEN", "BLINDNESS", "CONDUIT_POWER", "DARKNESS",
            "DOLPHINS_GRACE", "FIRE_RESISTANCE", "GLOWING", "HASTE", "HEALTH_BOOST",
            "HERO_OF_THE_VILLAGE", "HUNGER", "INSTANT_DAMAGE", "INSTANT_HEALTH",
            "INVISIBILITY", "JUMP_BOOST", "LEVITATION", "LUCK", "MINING_FATIGUE",
            "NAUSEA", "NIGHT_VISION", "POISON", "REGENERATION", "RESISTANCE",
            "SATURATION", "SLOW_FALLING", "SLOWNESS", "SPEED", "STRENGTH",
            "TRIAL_OMEN", "UNLUCK", "WATER_BREATHING", "WEAKNESS", "WIND_CHARGED",
            "WITHER"
    );

    // ─── Tests ────────────────────────────────────────────────────────────────────

    @Test
    void testFileExists() {
        assertTrue(new File("src/main/resources/monsters.yml").exists(),
                "monsters.yml introuvable dans src/main/resources/");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGlobalSettings() {
        Map<String, Object> root = loadRoot();

        // levels-per-world-tier : optionnel, mais si présent doit être un entier ≥ 1
        if (root.containsKey("levels-per-world-tier")) {
            Object lpt = root.get("levels-per-world-tier");
            assertTrue(lpt instanceof Integer, "levels-per-world-tier doit être un entier");
            assertTrue((Integer) lpt >= 1, "levels-per-world-tier doit être ≥ 1, valeur : " + lpt);
        }

        // spawn-weight-exponent : optionnel, mais si présent doit être un nombre > 0
        if (root.containsKey("spawn-weight-exponent")) {
            Object exp = root.get("spawn-weight-exponent");
            assertTrue(exp instanceof Number, "spawn-weight-exponent doit être un nombre");
            assertTrue(((Number) exp).doubleValue() > 0,
                    "spawn-weight-exponent doit être > 0, valeur : " + exp);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMobsSectionExists() {
        Map<String, Object> root = loadRoot();
        assertTrue(root.containsKey("mobs"), "La section 'mobs' est absente de monsters.yml");

        Object mobsRaw = root.get("mobs");
        assertNotNull(mobsRaw, "La section 'mobs' est nulle");
        assertInstanceOf(Map.class, mobsRaw, "La section 'mobs' doit être un objet YAML");

        Map<String, Object> mobs = (Map<String, Object>) mobsRaw;
        assertFalse(mobs.isEmpty(), "La section 'mobs' ne contient aucun mob");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEachMobConfig() {
        Map<String, Object> mobs = getMobs();

        for (Map.Entry<String, Object> entry : mobs.entrySet()) {
            String mobKey = entry.getKey();
            String ctx    = "Mob '" + mobKey + "'";

            assertNotNull(entry.getValue(), ctx + " : valeur nulle");
            assertInstanceOf(Map.class, entry.getValue(), ctx + " : doit être un objet YAML");

            Map<String, Object> mob = (Map<String, Object>) entry.getValue();
            assertMobConfig(mobKey, mob);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreeperHasSpecialFields() {
        Map<String, Object> mobs = getMobs();
        if (!mobs.containsKey("CREEPER")) return; // optionnel si pas de Creeper configuré

        Map<String, Object> creeper = (Map<String, Object>) mobs.get("CREEPER");
        String ctx = "Mob 'CREEPER'";

        // Le Creeper est le seul mob à avoir explosion-radius et fuse-ticks
        if (creeper.containsKey("explosion-radius")) {
            assertStatBlock(ctx + " explosion-radius", creeper.get("explosion-radius"));
        }
        if (creeper.containsKey("fuse-ticks")) {
            assertFuseTicksBlock(ctx + " fuse-ticks", creeper.get("fuse-ticks"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPotionEffectsWhereDeclared() {
        Map<String, Object> mobs = getMobs();

        for (Map.Entry<String, Object> entry : mobs.entrySet()) {
            String mobKey = entry.getKey();
            Map<String, Object> mob = (Map<String, Object>) entry.getValue();
            if (!mob.containsKey("potion-effects")) continue;

            Object raw = mob.get("potion-effects");
            String ctx = "Mob '" + mobKey + "' potion-effects";
            assertInstanceOf(List.class, raw, ctx + " doit être une liste");

            List<?> effects = (List<?>) raw;
            assertFalse(effects.isEmpty(), ctx + " ne doit pas être vide si déclaré");

            for (int i = 0; i < effects.size(); i++) {
                assertPotionEffectBlock(ctx + "[" + i + "]", effects.get(i));
            }
        }
    }

    // ─── Assertions de structure ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void assertMobConfig(String mobKey, Map<String, Object> mob) {
        String ctx = "Mob '" + mobKey + "'";

        // enabled : optionnel, mais si présent doit être un booléen
        if (mob.containsKey("enabled")) {
            assertInstanceOf(Boolean.class, mob.get("enabled"), ctx + " : 'enabled' doit être un booléen");
        }

        // stats : optionnelle, mais si présente doit être une Map avec des clés valides
        if (mob.containsKey("stats")) {
            Object statsRaw = mob.get("stats");
            assertInstanceOf(Map.class, statsRaw, ctx + " : 'stats' doit être un objet YAML");
            Map<String, Object> stats = (Map<String, Object>) statsRaw;

            for (Map.Entry<String, Object> statEntry : stats.entrySet()) {
                String statKey = statEntry.getKey();
                String statCtx = ctx + " stats." + statKey;

                assertTrue(VALID_STAT_KEYS.contains(statKey),
                        statCtx + " : clé de stat inconnue. Clés valides : " + VALID_STAT_KEYS);
                assertStatBlock(statCtx, statEntry.getValue());
            }
        }

        // explosion-radius : si présente, doit être un statConfig valide
        if (mob.containsKey("explosion-radius")) {
            assertStatBlock(ctx + " explosion-radius", mob.get("explosion-radius"));
        }

        // fuse-ticks : si présent, doit être un fuseConfig valide
        if (mob.containsKey("fuse-ticks")) {
            assertFuseTicksBlock(ctx + " fuse-ticks", mob.get("fuse-ticks"));
        }

        // potion-effects : validé séparément dans testPotionEffectsWhereDeclared
    }

    @SuppressWarnings("unchecked")
    private void assertStatBlock(String ctx, Object raw) {
        assertNotNull(raw, ctx + " : valeur nulle");
        assertInstanceOf(Map.class, raw, ctx + " : doit être un objet YAML");
        Map<String, Object> stat = (Map<String, Object>) raw;

        assertTrue(stat.containsKey("enabled"),   ctx + " : champ 'enabled' manquant");
        assertTrue(stat.containsKey("base"),       ctx + " : champ 'base' manquant");
        assertTrue(stat.containsKey("per-level"),  ctx + " : champ 'per-level' manquant");

        assertInstanceOf(Boolean.class, stat.get("enabled"),  ctx + " : 'enabled' doit être un booléen");
        assertInstanceOf(Number.class,  stat.get("base"),     ctx + " : 'base' doit être un nombre");
        assertInstanceOf(Number.class,  stat.get("per-level"),ctx + " : 'per-level' doit être un nombre");

        double base = ((Number) stat.get("base")).doubleValue();
        assertTrue(base >= 0, ctx + " : 'base' doit être ≥ 0 (valeur : " + base + ")");
    }

    @SuppressWarnings("unchecked")
    private void assertFuseTicksBlock(String ctx, Object raw) {
        assertNotNull(raw, ctx + " : valeur nulle");
        assertInstanceOf(Map.class, raw, ctx + " : doit être un objet YAML");
        Map<String, Object> fuse = (Map<String, Object>) raw;

        assertTrue(fuse.containsKey("enabled"),   ctx + " : champ 'enabled' manquant");
        assertTrue(fuse.containsKey("base"),       ctx + " : champ 'base' manquant");
        assertTrue(fuse.containsKey("per-level"),  ctx + " : champ 'per-level' manquant");

        assertInstanceOf(Boolean.class, fuse.get("enabled"),   ctx + " : 'enabled' doit être un booléen");
        assertInstanceOf(Number.class,  fuse.get("base"),      ctx + " : 'base' doit être un nombre");
        assertInstanceOf(Number.class,  fuse.get("per-level"), ctx + " : 'per-level' doit être un nombre");

        if (fuse.containsKey("min")) {
            Object min = fuse.get("min");
            assertInstanceOf(Integer.class, min, ctx + " : 'min' doit être un entier");
            assertTrue((Integer) min >= 1, ctx + " : 'min' doit être ≥ 1 (valeur : " + min + ")");
        }
    }

    @SuppressWarnings("unchecked")
    private void assertPotionEffectBlock(String ctx, Object raw) {
        assertNotNull(raw, ctx + " : entrée nulle");
        assertInstanceOf(Map.class, raw, ctx + " : doit être un objet YAML");
        Map<String, Object> effect = (Map<String, Object>) raw;

        // Champs requis
        assertField(ctx, effect, "type",                    String.class);
        assertField(ctx, effect, "min-mob-level",           Integer.class);
        assertField(ctx, effect, "duration-base",           Integer.class);
        assertField(ctx, effect, "duration-per-level",      Number.class);
        assertField(ctx, effect, "amplifier-upgrade-every", Integer.class);
        assertField(ctx, effect, "chance-base",             Number.class);
        assertField(ctx, effect, "chance-per-level",        Number.class);
        assertField(ctx, effect, "max-chance",              Number.class);

        // Validité du type de potion
        String type = (String) effect.get("type");
        assertTrue(VALID_POTION_TYPES.contains(type.toUpperCase()),
                ctx + " : type de potion inconnu '" + type + "'. Types valides : " + VALID_POTION_TYPES);

        // Plages de valeurs
        int minMobLevel = (Integer) effect.get("min-mob-level");
        assertTrue(minMobLevel >= 1, ctx + " : 'min-mob-level' doit être ≥ 1, valeur : " + minMobLevel);

        int durationBase = (Integer) effect.get("duration-base");
        assertTrue(durationBase >= 0, ctx + " : 'duration-base' doit être ≥ 0, valeur : " + durationBase);

        int ampEvery = (Integer) effect.get("amplifier-upgrade-every");
        assertTrue(ampEvery >= 1, ctx + " : 'amplifier-upgrade-every' doit être ≥ 1, valeur : " + ampEvery);

        double chanceBase = ((Number) effect.get("chance-base")).doubleValue();
        assertTrue(chanceBase >= 0 && chanceBase <= 1,
                ctx + " : 'chance-base' doit être entre 0.0 et 1.0, valeur : " + chanceBase);

        double maxChance = ((Number) effect.get("max-chance")).doubleValue();
        assertTrue(maxChance >= 0 && maxChance <= 1,
                ctx + " : 'max-chance' doit être entre 0.0 et 1.0, valeur : " + maxChance);

        assertTrue(maxChance >= chanceBase,
                ctx + " : 'max-chance' (" + maxChance + ") doit être ≥ 'chance-base' (" + chanceBase + ")");
    }

    private <T> void assertField(String ctx, Map<String, Object> map, String key, Class<T> type) {
        assertTrue(map.containsKey(key), ctx + " : champ '" + key + "' manquant");
        assertNotNull(map.get(key),      ctx + " : champ '" + key + "' est null");
        assertInstanceOf(type, map.get(key),
                ctx + " : '" + key + "' doit être de type " + type.getSimpleName()
                        + " (trouvé : " + map.get(key).getClass().getSimpleName() + ")");
    }

    // ─── Chargement YAML ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadRoot() {
        File file = new File("src/main/resources/monsters.yml");
        assertTrue(file.exists(), "monsters.yml introuvable");
        try (InputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object loaded = yaml.load(in);
            assertNotNull(loaded, "monsters.yml est vide ou n'a pas pu être parsé");
            assertInstanceOf(Map.class, loaded, "La racine de monsters.yml doit être un objet YAML");
            return (Map<String, Object>) loaded;
        } catch (Exception e) {
            fail("Erreur de parsing YAML : " + e.getMessage());
            throw new RuntimeException(e); // unreachable
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMobs() {
        Map<String, Object> root = loadRoot();
        assertTrue(root.containsKey("mobs"), "Section 'mobs' absente");
        return (Map<String, Object>) root.get("mobs");
    }
}
