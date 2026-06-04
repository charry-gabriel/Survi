package fr.miuby.survi.role;

import fr.miuby.lib.utils.MultiKeyRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Charge et expose tous les rôles définis dans {@code roles.yml}.
 *
 * <h3>Architecture</h3>
 * Les attributs de gameplay sont lus depuis {@code roles.yml}.
 * Le {@code displayName} et la {@code color} sont définis dans {@link ERole}
 * (comme pour {@link fr.miuby.survi.job.EJob}).
 *
 * <h3>Ajouter un nouveau rôle</h3>
 * <ol>
 *   <li>Ajouter la valeur dans {@link ERole} avec son displayName et sa couleur.</li>
 *   <li>Ajouter la section correspondante dans {@code roles.yml}.</li>
 * </ol>
 */
public class RoleLoader {
    private static final MultiKeyRegistry<Role> INSTANCE = new MultiKeyRegistry<>();

    public RoleLoader() {
        loadRoles();
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge {@code roles.yml} à chaud, sans redémarrage.
     *
     * <p>Le registre est entièrement reconstruit depuis le fichier. Les joueurs
     * connectés conservent leurs attributs de rôle actuels ; les modifications
     * seront effectives à leur prochaine reconnexion ou réattribution de rôle.</p>
     */
    public void reload() {
        INSTANCE.clear();
        loadRoles();
    }

    // =========================================================================
    // Chargement interne
    // =========================================================================

    private void loadRoles() {
        File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), "roles.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection rolesSection = config.getConfigurationSection("roles");
        if (rolesSection == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.ROLE,
                    "[RoleLoader] Section 'roles' introuvable dans roles.yml !");
            return;
        }

        for (String key : rolesSection.getKeys(false)) {
            ERole eRole;
            try {
                eRole = ERole.valueOf(key);
            } catch (IllegalArgumentException e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE,
                        "[RoleLoader] Rôle inconnu dans roles.yml : " + key, e);
                continue;
            }

            ConfigurationSection section = rolesSection.getConfigurationSection(key);
            if (section == null) continue;

            String roleId = section.getString("roleId", key.toLowerCase());
            List<RoleAttribute> attributes = parseAttributes(section);

            Role role = new Role(
                    eRole,
                    eRole.toComponent(),
                    attributes,
                    roleId
            );

            attributes.forEach(attr -> attr.setRole(roleId));

            INSTANCE.register(role, eRole);
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.ROLE,
                "[RoleLoader] " + INSTANCE.getAll().size() + " rôle(s) chargé(s) depuis roles.yml.");
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private List<RoleAttribute> parseAttributes(ConfigurationSection section) {
        List<RoleAttribute> result = new ArrayList<>();
        List<?> attrList = section.getList("attributes");
        if (attrList == null) return result;

        for (Object raw : attrList) {
            if (!(raw instanceof java.util.Map<?, ?> rawMap)) continue;

            // Cast sûr : SnakeYAML produit toujours des Map<String, Object>
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) rawMap;

            String worldStr     = map.containsKey("world")     ? String.valueOf(map.get("world"))     : "ALL";
            String attributeStr = map.containsKey("attribute") ? String.valueOf(map.get("attribute")) : null;
            String opStr        = map.containsKey("operation") ? String.valueOf(map.get("operation")) : "ADD_SCALAR";
            float  value        = map.containsKey("value")     ? ((Number) map.get("value")).floatValue() : 0f;

            if (attributeStr == null) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE,
                        "[RoleLoader] Attribut manquant dans un élément de la liste 'attributes'");
                continue;
            }

            EWorld world;
            try {
                world = EWorld.valueOf(worldStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE,
                        "[RoleLoader] World inconnu : " + worldStr, e);
                continue;
            }

            Attribute attribute = Registry.ATTRIBUTE.get(
                    org.bukkit.NamespacedKey.minecraft(attributeStr.toLowerCase())
            );
            if (attribute == null) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE,
                        "[RoleLoader] Attribut Bukkit inconnu : " + attributeStr);
                continue;
            }

            RoleAttribute.EOperation operation;
            try {
                operation = RoleAttribute.EOperation.valueOf(opStr.toUpperCase());
            } catch (IllegalArgumentException _) {
                operation = RoleAttribute.EOperation.ADD_SCALAR;
            }

            result.add(new RoleAttribute(world, attribute, value, operation));
        }

        return result;
    }

    // =========================================================================
    // Accesseurs
    // =========================================================================

    public Role getRole(ERole role) {
        return INSTANCE.get(role);
    }

    public Collection<Role> getRoles() {
        return INSTANCE.getAll();
    }

    public Role getDefaultRole() {
        return INSTANCE.get(ERole.NAIN);
    }

    public Role getRole(String roleType) {
        try {
            return getRole(ERole.valueOf(roleType));
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}