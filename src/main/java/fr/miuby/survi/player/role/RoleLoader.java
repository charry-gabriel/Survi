package fr.miuby.survi.player.role;

import fr.miuby.lib.utils.MultiKeyRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.service.PlayerAttributeService;
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

public class RoleLoader {
    private static final MultiKeyRegistry<Role> INSTANCE = new MultiKeyRegistry<>();

    public RoleLoader() {
        loadRoles();
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge {@code roles.yml} à chaud et ré-applique immédiatement les attributs
     * de rôle sur tous les joueurs connectés.
     *
     * <h3>Séquence pour chaque joueur connecté</h3>
     * <ol>
     *   <li>Suppression des anciens modificateurs d'attributs (en utilisant les anciens
     *       objets {@link Role} encore référencés par le joueur).</li>
     *   <li>Mise à jour des références de rôle (principal + sous-rôles) vers les nouveaux
     *       objets du registre rechargé.</li>
     *   <li>Application des nouveaux modificateurs d'attributs.</li>
     * </ol>
     *
     * <p>Si un rôle est supprimé du YAML mais qu'un joueur le possède encore,
     * ses attributs sont retirés mais le rôle n'est pas désaffecté (cas très inhabituel).
     * Aucune écriture en DB n'est faite — seul l'état en mémoire et les attributs Bukkit changent.</p>
     */
    public void reload() {
        INSTANCE.clear();
        loadRoles();
        reapplyOnlinePlayers();
    }

    /**
     * Pour chaque joueur connecté : retire les anciens attributs de rôle, met à jour
     * les références vers les nouveaux objets Role du registre, puis réapplique.
     */
    private void reapplyOnlinePlayers() {
        PlayerAttributeService attributeService = new PlayerAttributeService();
        int updated = 0;

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getPlayer() == null) continue;

            // 1. Retire les anciens modificateurs (utilise encore les anciens objets Role)
            attributeService.clearAllRoleAttributes(player);

            // 2. Met à jour la référence du rôle principal
            if (player.getRole() != null) {
                Role newMain = getRole(player.getRole().type());
                if (newMain != null) player.setRole(newMain);
                // Si null (rôle supprimé du YAML), le joueur garde la référence ancienne
                // mais ses attributs ont été nettoyés à l'étape 1 — ils ne seront plus
                // appliqués à l'étape 3 (getRole retournerait null).
            }

            // 3. Met à jour les références des sous-rôles
            List<Role> subs = player.getSubRoles();
            for (int i = 0; i < subs.size(); i++) {
                Role newSub = getRole(subs.get(i).type());
                if (newSub != null) subs.set(i, newSub);
            }

            // 4. Applique les nouveaux attributs
            attributeService.applyAllRoleAttributes(player);
            updated++;
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.ROLE,
                "[RoleLoader] Attributs de rôle ré-appliqués sur " + updated + " joueur(s) connecté(s).");
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

            Role role = new Role(eRole, eRole.toComponent(), attributes, roleId);
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
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE, "[RoleLoader] World inconnu : " + worldStr, e);
                continue;
            }

            Attribute attribute = Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(attributeStr.toLowerCase()));
            if (attribute == null) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.ROLE, "[RoleLoader] Attribut Bukkit inconnu : " + attributeStr);
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

    public Role getRole(ERole role)       { return INSTANCE.get(role); }
    public Collection<Role> getRoles()    { return INSTANCE.getAll(); }
    public Role getDefaultRole()          { return INSTANCE.get(ERole.NEUTRE); }

    public Role getRole(String roleType) {
        try { return getRole(ERole.valueOf(roleType)); }
        catch (IllegalArgumentException _) { return null; }
    }
}