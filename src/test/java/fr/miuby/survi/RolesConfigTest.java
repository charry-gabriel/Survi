package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la structure de {@code src/main/resources/roles.yml} sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Section {@code roles} non vide, clés = noms d'ERole valides.</li>
 *   <li>Pour chaque rôle : présence de {@code roleId} et {@code attributes}.</li>
 *   <li>Pour chaque attribut : {@code world}, {@code attribute}, {@code value} requis,
 *       {@code operation} optionnel mais valide s'il est présent.</li>
 *   <li>Les valeurs sont des nombres (pas de strings).</li>
 * </ul>
 */
class RolesConfigTest {

    private static final Set<String> VALID_WORLDS = Set.of(
            "ALL", "VILLAGE", "WILDERNESS", "NETHER", "END"
    );

    private static final Set<String> VALID_OPERATIONS = Set.of(
            "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1", "REMOVE"
    );

    @SuppressWarnings("unchecked")
    private static Set<String> validRoleNames() {
        try {
            String json = Files.readString(Paths.get("src/main/resources/schema/roles-schema.json"));
            Yaml yaml = new Yaml(new LoaderOptions());
            Map<String, Object> schema = (Map<String, Object>) yaml.load(json);
            Map<String, Object> props = (Map<String, Object>) schema.get("properties");
            Map<String, Object> roles = (Map<String, Object>) props.get("roles");
            Map<String, Object> propNames = (Map<String, Object>) roles.get("propertyNames");
            List<String> values = (List<String>) propNames.get("enum");
            if (values != null && !values.isEmpty())
                return new java.util.LinkedHashSet<>(values);
        } catch (IOException | ClassCastException ignored) {}
        // Fallback statique
        return Set.of("DRAGON", "LOUP_GAROU", "FEE", "NAIN", "GEANT",
                "NOVICE", "COMBATANT", "MINEUR", "ALCHIMISTE", "ENCHANTEUR", "FERMIER");
    }

    @Test
    void testFileExists() {
        assertTrue(new File("src/main/resources/roles.yml").exists(),
                "roles.yml introuvable dans src/main/resources/");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRolesSectionExists() {
        Map<String, Object> root = loadRoot();
        assertTrue(root.containsKey("roles"), "La section 'roles' est absente de roles.yml");

        Object rolesRaw = root.get("roles");
        assertNotNull(rolesRaw, "La section 'roles' est nulle");
        assertInstanceOf(Map.class, rolesRaw, "La section 'roles' doit être un objet YAML");

        Map<String, Object> roles = (Map<String, Object>) rolesRaw;
        assertFalse(roles.isEmpty(), "La section 'roles' ne contient aucun rôle");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEachRoleConfig() {
        Map<String, Object> roles = getRoles();
        Set<String> validNames = validRoleNames();

        for (Map.Entry<String, Object> entry : roles.entrySet()) {
            String roleKey = entry.getKey();
            String ctx = "Rôle '" + roleKey + "'";

            assertTrue(validNames.contains(roleKey),
                    ctx + " : clé inconnue. Rôles valides : " + validNames);

            assertNotNull(entry.getValue(), ctx + " : valeur nulle");
            assertInstanceOf(Map.class, entry.getValue(), ctx + " : doit être un objet YAML");

            Map<String, Object> roleConfig = (Map<String, Object>) entry.getValue();
            assertRoleConfig(roleKey, roleConfig);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAllErolesAreDefined() {
        Set<String> validNames = validRoleNames();
        Map<String, Object> roles = getRoles();
        for (String expected : validNames) {
            assertTrue(roles.containsKey(expected),
                    "Le rôle '" + expected + "' est défini dans ERole mais absent de roles.yml");
        }
    }

    @SuppressWarnings("unchecked")
    private void assertRoleConfig(String roleKey, Map<String, Object> config) {
        String ctx = "Rôle '" + roleKey + "'";

        // roleId requis
        assertTrue(config.containsKey("roleId"), ctx + " : champ 'roleId' manquant");
        assertInstanceOf(String.class, config.get("roleId"), ctx + " : 'roleId' doit être une chaîne");
        assertFalse(((String) config.get("roleId")).isBlank(), ctx + " : 'roleId' ne doit pas être vide");

        // attributes requis
        assertTrue(config.containsKey("attributes"), ctx + " : champ 'attributes' manquant");
        Object attrsRaw = config.get("attributes");
        assertInstanceOf(List.class, attrsRaw, ctx + " : 'attributes' doit être une liste");

        List<?> attrs = (List<?>) attrsRaw;
        assertFalse(attrs.isEmpty(), ctx + " : 'attributes' ne doit pas être vide");

        for (int i = 0; i < attrs.size(); i++) {
            assertRoleAttribute(ctx + ".attributes[" + i + "]", attrs.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    private void assertRoleAttribute(String ctx, Object raw) {
        assertNotNull(raw, ctx + " : entrée nulle");
        assertInstanceOf(Map.class, raw, ctx + " : doit être un objet YAML");

        Map<String, Object> attr = (Map<String, Object>) raw;

        // world
        assertTrue(attr.containsKey("world"), ctx + " : champ 'world' manquant");
        String world = String.valueOf(attr.get("world")).toUpperCase();
        assertTrue(VALID_WORLDS.contains(world),
                ctx + " : world inconnu '" + world + "'. Valeurs valides : " + VALID_WORLDS);

        // attribute
        assertTrue(attr.containsKey("attribute"), ctx + " : champ 'attribute' manquant");
        assertInstanceOf(String.class, attr.get("attribute"),
                ctx + " : 'attribute' doit être une chaîne");

        // value
        assertTrue(attr.containsKey("value"), ctx + " : champ 'value' manquant");
        assertInstanceOf(Number.class, attr.get("value"),
                ctx + " : 'value' doit être un nombre");

        // operation (optionnel)
        if (attr.containsKey("operation")) {
            String op = String.valueOf(attr.get("operation")).toUpperCase();
            assertTrue(VALID_OPERATIONS.contains(op),
                    ctx + " : operation inconnue '" + op + "'. Valeurs valides : " + VALID_OPERATIONS);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadRoot() {
        File file = new File("src/main/resources/roles.yml");
        assertTrue(file.exists(), "roles.yml introuvable");
        try (InputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object loaded = yaml.load(in);
            assertNotNull(loaded, "roles.yml est vide ou n'a pas pu être parsé");
            assertInstanceOf(Map.class, loaded, "La racine de roles.yml doit être un objet YAML");
            return (Map<String, Object>) loaded;
        } catch (Exception e) {
            fail("Erreur de parsing YAML : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRoles() {
        Map<String, Object> root = loadRoot();
        assertTrue(root.containsKey("roles"), "Section 'roles' absente");
        return (Map<String, Object>) root.get("roles");
    }
}
