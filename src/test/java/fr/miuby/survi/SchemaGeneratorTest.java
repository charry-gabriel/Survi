package fr.miuby.survi;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SchemaGeneratorTest {

    @Test
    void updateSchemas() {
        Assertions.assertDoesNotThrow(() -> {
            updateVillagersSchema();
            updateRecipesSchema();
            updateQuestsSchema();
            updateGlobalQuestsSchema();
            updateTradersSchema();
            updateMonstersSchema();
            updateRolesSchema();
        });
    }

    private void updateVillagersSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/villagers-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Villager Type enum
        content = replaceEnum(content, "type", getVillagerTypeNames());

        // Update Villager Profession enum
        content = replaceEnum(content, "profession", getVillagerProfessionNames());

        // Update Material enum (for tributes)
        content = replaceEnum(content, "material", getMaterialNames());

        // Update CustomItem enum
        content = replaceEnum(content, "customItem", getCustomItemNames());

        // Update blessing effect type enum
        content = replaceEnum(content, "type", getEnumNamesFromSource("src/main/java/fr/miuby/survi/villager/villagerlevel/blessing/BlessingLoader.java")
                .isEmpty()
                ? List.of("DAMAGE", "DISPEL", "FLY", "GAME_MODE", "ITEM", "LIMIT_WORLD",
                "LOCK_WORLD", "MAX_HEALTH", "MESSAGE", "RANDOM_ITEM", "REGEN",
                "RESISTANCE", "UNLOCK_ARMOR", "UNLOCK_TOOL", "WORLD_LEVEL", "WORLD_RESET")
                : List.of("DAMAGE", "DISPEL", "FLY", "GAME_MODE", "ITEM", "LIMIT_WORLD",
                "LOCK_WORLD", "MAX_HEALTH", "MESSAGE", "RANDOM_ITEM", "REGEN",
                "RESISTANCE", "UNLOCK_ARMOR", "UNLOCK_TOOL", "WORLD_LEVEL", "WORLD_RESET"));

        // Update tool enum (ELockedToolType)
        content = replaceEnum(content, "tool", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedToolType.java"));

        // Update armor enum (ELockedArmorType)
        content = replaceEnum(content, "armor", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedArmorType.java"));

        // Update world enum (EWorld)
        content = replaceEnum(content, "world", getEnumNamesFromSource("src/main/java/fr/miuby/survi/world/EWorld.java"));

        Files.writeString(path, content);
    }

    private void updateRecipesSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/recipes-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        List<String> materials = getMaterialNames();

        // Update old_recipes (namespaced keys: minecraft:item_name)
        List<String> namespacedKeys = materials.stream()
                .map(m -> "minecraft:" + m.toLowerCase())
                .sorted()
                .toList();

        // Regex to find "old_recipes" and its items block
        Pattern oldRecipesPattern = Pattern.compile("\"old_recipes\":\\s*\\{[^}]*\"items\":\\s*\\{[^{}]*\\}");
        Matcher oldMatcher = oldRecipesPattern.matcher(content);
        if (oldMatcher.find()) {
            String match = oldMatcher.group();
            String updated = replaceEnum(match, "items", namespacedKeys);
            content = content.replace(match, updated);
        }

        // Update Roles
        content = replaceEnum(content, "roles", getEnumNamesFromSource("src/main/java/fr/miuby/survi/role/ERole.java"));

        // Update Material definition
        content = replaceEnum(content, "Material", materials);

        // Update CustomItem enum
        content = replaceEnum(content, "CustomItem", getCustomItemNames());

        Files.writeString(path, content);
    }

    private void updateQuestsSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/quests-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Quest Type enum (MINE, KILL, …)
        content = replaceEnum(content, "type", getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/EQuestType.java"));

        // difficulty est désormais un integer dans le schéma — pas d'enum à mettre à jour.

        // Update jobs items.enum (EJob) — métiers autorisés à recevoir la quête.
        // jobs est un array avec items.enum imbriqué : on utilise un pattern dédié.
        List<String> jobNames = getEnumNamesFromSource("src/main/java/fr/miuby/survi/job/EJob.java");
        if (!jobNames.isEmpty()) {
            String jobEnumJson = jobNames.stream().distinct().sorted()
                    .map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n        "));
            Pattern jobsPattern = Pattern.compile("(\"jobs\"\\s*:\\s*\\{[^{}]*\"items\"\\s*:\\s*\\{[^{}]*\"enum\"\\s*:\\s*)\\[[^\\]]*\\]");
            content = jobsPattern.matcher(content).replaceAll(
                    m -> m.group(1) + "[\n        " + Matcher.quoteReplacement(jobEnumJson) + "\n      ]");
        }

        // Update targets items.enum (Material + EntityType).
        // targets est un array nullable dont items.enum contient tous les Materials et EntityTypes.
        List<String> allTargets = Stream.concat(getMaterialNames().stream(), getEntityTypeNames().stream())
                .distinct()
                .sorted()
                .toList();
        content = replaceTargetsItemsEnum(content, allTargets);

        // Update job enum in rewards > REPUTATION effect
        content = replaceEnum(content, "job", getEnumNamesFromSource("src/main/java/fr/miuby/survi/job/EJob.java"));

        // Update potion enum in rewards > POTION effect
        List<String> potionEffects = getPotionEffectTypeNames().stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();
        content = replaceEnum(content, "potion", potionEffects);

        // Update tool enum in rewards > UNLOCK_TOOL effect
        content = replaceEnum(content, "tool", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedToolType.java"));

        // Update armor enum in rewards > UNLOCK_ARMOR effect
        content = replaceEnum(content, "armor", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedArmorType.java"));

        // Update world enum in rewards > LOCK_WORLD / LIMIT_WORLD effects
        content = replaceEnum(content, "world", getEnumNamesFromSource("src/main/java/fr/miuby/survi/world/EWorld.java"));

        Files.writeString(path, content);
    }

    private void updateGlobalQuestsSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/global-quests-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update quest type enum (MINE, KILL, …)
        content = replaceEnum(content, "type", getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/EQuestType.java"));

        // Update targets items.enum (Material + EntityType).
        List<String> allTargets = Stream.concat(getMaterialNames().stream(), getEntityTypeNames().stream())
                .distinct()
                .sorted()
                .toList();
        content = replaceTargetsItemsEnum(content, allTargets);

        // Update job enum in rewards > REPUTATION effect
        content = replaceEnum(content, "job", getEnumNamesFromSource("src/main/java/fr/miuby/survi/job/EJob.java"));

        // Update potion enum in rewards > POTION effect
        List<String> potionEffects = getPotionEffectTypeNames().stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();
        content = replaceEnum(content, "potion", potionEffects);

        // Update tool enum in rewards > UNLOCK_TOOL effect
        content = replaceEnum(content, "tool", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedToolType.java"));

        // Update armor enum in rewards > UNLOCK_ARMOR effect
        content = replaceEnum(content, "armor", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/ELockedArmorType.java"));

        // Update world enum in rewards > LOCK_WORLD / LIMIT_WORLD effects
        content = replaceEnum(content, "world", getEnumNamesFromSource("src/main/java/fr/miuby/survi/world/EWorld.java"));

        Files.writeString(path, content);
    }

    private void updateTradersSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/traders-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Villager Type enum
        content = replaceEnum(content, "type", getVillagerTypeNames());

        // Update Villager Profession enum
        content = replaceEnum(content, "profession", getVillagerProfessionNames());

        // Update Jobs
        content = replaceEnum(content, "job", getEnumNamesFromSource("src/main/java/fr/miuby/survi/job/EJob.java"));

        // questDifficulty est désormais un integer — pas d'enum à mettre à jour.

        // Update CustomItem enum
        content = replaceEnum(content, "customItem", getCustomItemNames());

        // Update Material enum
        content = replaceEnum(content, "material", getMaterialNames());

        // Update mainHandItem (Material + CustomItem)
        List<String> allItems = Stream.concat(getMaterialNames().stream(), getCustomItemNames().stream())
                .distinct().sorted().toList();
        content = replaceEnum(content, "mainHandItem", allItems);

        Files.writeString(path, content);
    }

    private void updateRolesSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/roles-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Met à jour l'enum des rôles valides (propertyNames → ERole)
        content = replaceEnum(content, "propertyNames", getEnumNamesFromSource("src/main/java/fr/miuby/survi/role/ERole.java"));

        // Met à jour l'enum des mondes valides (EWorld)
        content = replaceEnum(content, "world", getEnumNamesFromSource("src/main/java/fr/miuby/survi/world/EWorld.java"));

        Files.writeString(path, content);
    }

    private void updateMonstersSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/monsters-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Met à jour l'enum des clés de mobs valides (propertyNames → EntityType, sans PLAYER/UNKNOWN).
        List<String> mobTypes = getEntityTypeNames().stream()
                .filter(name -> !name.equals("PLAYER") && !name.equals("UNKNOWN"))
                .sorted()
                .toList();
        content = replaceEnum(content, "propertyNames", mobTypes);

        // Met à jour l'enum des effets de potion depuis l'API Bukkit — se met à jour tout seul.
        content = replaceEnum(content, "type", getPotionEffectTypeNames());

        Files.writeString(path, content);
    }

    private List<String> getMaterialNames() {
        Set<String> materials = new TreeSet<>();
        try {
            Arrays.stream(Material.values())
                    .filter(m -> !m.isLegacy())
                    .map(Enum::name)
                    .forEach(materials::add);
        } catch (Throwable _) {
            // Fallback si Material.values() échoue
            materials.addAll(List.of("AIR", "STONE", "GRASS_BLOCK", "DIRT", "COBBLESTONE", "OAK_PLANKS", "EMERALD", "DIAMOND", "IRON_INGOT", "GOLD_INGOT"));
        }

        // Enrichir avec les YML
        materials.addAll(collectValuesFromYml("material"));

        return materials.stream().toList();
    }

    private List<String> getEntityTypeNames() {
        Set<String> entities = new TreeSet<>();
        try {
            Arrays.stream(EntityType.values())
                    .filter(e -> e != EntityType.UNKNOWN)
                    .map(Enum::name)
                    .forEach(entities::add);
        } catch (Throwable _) {
            entities.addAll(List.of("PLAYER", "ZOMBIE", "SKELETON", "CREEPER", "COW", "SHEEP", "PIG", "CHICKEN"));
        }

        // Enrichir avec les valeurs trouvées dans les YAML (targets est une liste inline)
        entities.addAll(collectValuesFromYml("targets"));

        return entities.stream().toList();
    }

    /**
     * Remplace l'enum dans le bloc {@code items} de la propriété {@code "targets"} du schéma.
     * La propriété {@code targets} est un tableau nullable dont la structure est :
     * <pre>
     * "targets": {
     *   "type": ["array", "null"],
     *   "items": {
     *     "type": "string",
     *     "enum": [...]
     *   }
     * }
     * </pre>
     */
    private String replaceTargetsItemsEnum(String content, List<String> values) {
        if (values.isEmpty()) return content;

        String enumJson = "\"enum\": [\n                " +
                values.stream().distinct().sorted().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n                ")) +
                "\n              ]";

        // On cherche "targets": { ... "items": { ... "enum": [...] } }
        // en remplaçant l'enum à l'intérieur du bloc items
        Pattern itemsEnumPattern = Pattern.compile(
                "(\"targets\"\\s*:\\s*\\{[^{}]*\"items\"\\s*:\\s*\\{[^{}]*)\"enum\"\\s*:\\s*\\[[^\\]]*\\]([^{}]*\\})([^{}]*\\})"
        );
        Matcher m = itemsEnumPattern.matcher(content);
        if (m.find()) {
            return m.replaceAll(mr -> Matcher.quoteReplacement(mr.group(1)) +
                    Matcher.quoteReplacement(enumJson) +
                    Matcher.quoteReplacement(mr.group(2)) +
                    Matcher.quoteReplacement(mr.group(3)));
        }
        return content;
    }


    private List<String> getVillagerTypeNames() {
        try {
            return org.bukkit.Registry.VILLAGER_TYPE.stream()
                    .map(e -> e.getKey().getKey().toUpperCase())
                    .sorted()
                    .toList();
        } catch (Throwable ignored) {
            return List.of("DESERT", "JUNGLE", "PLAINS", "SAVANNA", "SNOW", "SWAMP", "TAIGA");
        }
    }

    private List<String> getVillagerProfessionNames() {
        try {
            return org.bukkit.Registry.VILLAGER_PROFESSION.stream()
                    .map(e -> e.getKey().getKey().toUpperCase())
                    .sorted()
                    .toList();
        } catch (Throwable ignored) {
            return List.of("ARMORER", "BUTCHER", "CARTOGRAPHER", "CLERIC", "FARMER",
                    "FISHERMAN", "FLETCHER", "LEATHERWORKER", "LIBRARIAN", "MASON",
                    "NITWIT", "NONE", "SHEPHERD", "TOOLSMITH", "WEAPONSMITH");
        }
    }

    private List<String> getPotionEffectTypeNames() {
        Set<String> effects = new TreeSet<>();
        try {
            // Paper 1.20.3+ expose un Registry itérable pour les effets de potion
            org.bukkit.Registry.EFFECT.stream()
                    .map(e -> e.getKey().getKey().toUpperCase())
                    .forEach(effects::add);
        } catch (Throwable ignored) {
            // Fallback : PotionEffectType avait une méthode values() dans les anciennes API
        }
        if (effects.isEmpty()) {
            try {
                // Ancienne API Paper / CraftBukkit
                java.lang.reflect.Method m = PotionEffectType.class.getMethod("values");
                PotionEffectType[] types = (PotionEffectType[]) m.invoke(null);
                for (PotionEffectType t : types) {
                    if (t != null) effects.add(t.getName().toUpperCase());
                }
            } catch (Throwable ignored) {}
        }
        return effects.stream().toList();
    }

    private List<String> getCustomItemNames() {
        Set<String> items = new TreeSet<>(getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/ECustomItem.java"));
        items.addAll(collectValuesFromYml("customItem"));
        return items.stream().toList();
    }

    private Set<String> collectValuesFromYml(String key) {
        Set<String> values = new HashSet<>();
        try {
            Path resourcesPath = Paths.get("src/main/resources");
            if (!Files.exists(resourcesPath)) return values;

            Files.walk(resourcesPath)
                    .filter(p -> p.toString().endsWith(".yml"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            // Format scalaire : "key: VALUE"
                            Pattern scalarPattern = Pattern.compile("(?m)^\\s*" + key + ":\\s*[\"']?([A-Z0-9_]+)[\"']?");
                            Matcher matcher = scalarPattern.matcher(content);
                            while (matcher.find()) {
                                values.add(matcher.group(1).toUpperCase());
                            }
                            // Format liste inline : "key: [VALUE1, VALUE2, ...]"
                            Pattern listPattern = Pattern.compile("(?m)^\\s*" + key + ":\\s*\\[([^\\]]+)\\]");
                            Matcher listMatcher = listPattern.matcher(content);
                            while (listMatcher.find()) {
                                for (String item : listMatcher.group(1).split(",")) {
                                    String val = item.trim().replaceAll("[\"']", "").toUpperCase();
                                    if (!val.isEmpty() && !val.equals("NULL")) values.add(val);
                                }
                            }
                        } catch (IOException _) {
                            // ignore
                        }
                    });
        } catch (IOException _) {
            // ignore
        }
        return values;
    }

    private List<String> getEnumNamesFromSource(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return List.of();

            String content = Files.readString(p);
            // Regex simplifiée pour trouver les constantes d'enum
            // On cherche après l'ouverture de l'enum { et avant le premier ; (ou la fin si pas de ;)
            int startIndex = content.indexOf("{");
            // On cherche le dernier ; avant la fin du fichier ou avant les méthodes
            // Pour être sûr, on prend tout jusqu'à la fin et on filtrera
            String enumSection = content.substring(startIndex + 1);

            return Arrays.stream(enumSection.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("@") && !line.startsWith("private") && !line.startsWith("public") && !line.startsWith("static"))
                    .map(line -> {
                        // Une constante d'enum commence par un identifiant en majuscule
                        // et est suivie de ( ou , ou ; ou fin de ligne
                        Matcher m = Pattern.compile("^([A-Z0-9_]+)(?=[\\s(;,]|$)").matcher(line);
                        return m.find() ? m.group(1) : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (IOException _) {
            return List.of();
        }
    }

    private String replaceEnum(String content, String propertyName, List<String> values) {
        if (values.isEmpty()) return content;

        String enumJson = "\"enum\": [\n        " +
                values.stream().distinct().sorted().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n        ")) +
                "\n      ]";

        // On cherche le bloc de la propriété: "propertyName":
        // On utilise une regex qui essaie de trouver le bloc le plus petit possible
        Pattern propertyPattern = Pattern.compile("\"" + propertyName + "\":\\s*\\{[^{}]*\\}");
        Matcher matcher = propertyPattern.matcher(content);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(content, lastEnd, matcher.start());
            String propertyBlock = matcher.group();
            String updatedBlock;
            if (propertyBlock.contains("\"enum\":")) {
                updatedBlock = propertyBlock.replaceAll("\"enum\":\\s*\\[[^\\]]*\\]", Matcher.quoteReplacement(enumJson));
            } else if (propertyBlock.contains("\"examples\":")) {
                updatedBlock = propertyBlock.replaceAll("\"examples\":\\s*\\[[^\\]]*\\]", Matcher.quoteReplacement(enumJson));
            } else {
                // Insérer enum avant la dernière accolade fermante du bloc
                updatedBlock = propertyBlock.replaceFirst("\\}\\s*$", ",\n      " + enumJson + "\n    }");
                // Nettoyage si on a ajouté une virgule après rien (cas particulier)
                updatedBlock = updatedBlock.replace("{,\n", "{\n");
            }
            sb.append(updatedBlock);
            lastEnd = matcher.end();
        }
        sb.append(content.substring(lastEnd));
        return sb.toString();
    }
}