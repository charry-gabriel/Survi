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

        // Update tool enum (LockedToolType)
        content = replaceEnum(content, "tool", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/LockedToolType.java"));

        // Update armor enum (LockedArmorType)
        content = replaceEnum(content, "armor", getEnumNamesFromSource("src/main/java/fr/miuby/survi/item/locked_item/LockedArmorType.java"));

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
        Path path = Paths.get("src/main/resources/schema/quests.schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Quest Type (distinct from reward type)
        List<String> questTypes = getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/QuestType.java");
        String questTypeEnumJson = "\"enum\": [\n              " +
                questTypes.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n              ")) +
                "\n            ]";
        // Regex matching only the first "type" which is the quest type (no description)
        Pattern questTypePattern = Pattern.compile("\"type\":\\s*\\{\\s*\"type\":\\s*\"string\",\\s*\"enum\":\\s*\\[[^\\]]*\\]\\s*\\}");
        content = questTypePattern.matcher(content).replaceFirst(Matcher.quoteReplacement("\"type\": {\n            \"type\": \"string\",\n            " + questTypeEnumJson + "\n          }"));

        // Update Quest Difficulty
        content = replaceEnum(content, "difficulty", getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/QuestDifficulty.java"));

        // Update Reward Potion Effects
        List<String> potionEffects = getPotionEffectTypeNames().stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();

        String potionEnumJson = "\"enum\": [\n                    " +
                potionEffects.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n                    ")) +
                "\n                  ]";
        // Regex matching the type with description
        Pattern potionTypePattern = Pattern.compile("\"type\":\\s*\\{\\s*\"type\":\\s*\"string\",\\s*\"description\":\\s*\"Nom de l'effet de potion[^\"]*\",\\s*\"enum\":\\s*\\[[^\\]]*\\]\\s*\\}");
        content = potionTypePattern.matcher(content).replaceFirst(Matcher.quoteReplacement("\"type\": {\n                  \"type\": \"string\",\n                  \"description\": \"Nom de l'effet de potion (ex: speed, haste, strength)\",\n                  " + potionEnumJson + "\n                }"));

        // Update target enum
        List<String> allTargets = Stream.concat(getMaterialNames().stream(), getEntityTypeNames().stream())
                .distinct()
                .sorted()
                .toList();

        // Replace target type with enum if it matches the pattern
        Pattern targetPattern = Pattern.compile("\"target\":\\s*\\{\\s*\"type\":\\s*\\[\"string\",\\s*\"null\"\\][^{}]*\\}");
        String targetEnumJson = "\"target\": {\n            \"type\": [\"string\", \"null\"],\n            \"enum\": [\n              null,\n              " +
                allTargets.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n              ")) +
                "\n            ]\n          }";

        if (targetPattern.matcher(content).find()) {
            content = targetPattern.matcher(content).replaceAll(Matcher.quoteReplacement(targetEnumJson));
        } else {
            // Try to update existing enum if already present
            content = replaceEnum(content, "target", allTargets);
        }

        Files.writeString(path, content);
    }

    private void updateGlobalQuestsSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/global-quests-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update quest type enum (MINE, KILL, etc.)
        List<String> questTypes = getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/EQuestType.java");
        if (!questTypes.isEmpty()) {
            content = replaceEnum(content, "type", questTypes);
        }

        // Update target enum (Material + EntityType)
        List<String> allTargets = Stream.concat(getMaterialNames().stream(), getEntityTypeNames().stream())
                .distinct()
                .sorted()
                .toList();
        Pattern targetPattern = Pattern.compile("\"target\":\\s*\\{\\s*\"type\":\\s*\\[\"string\",\\s*\"null\"\\][^{}]*\\}");
        String targetEnumJson = "\"target\": {\n            \"type\": [\"string\", \"null\"],\n            \"description\": \"Matériau (MINE/CRAFT/SMELT) ou EntityType (KILL/SHEAR/BREED). Null pour FISH ou pour tout type.\",\n            \"enum\": [\n              null,\n              " +
                allTargets.stream().map(s -> "\"" + s + "\"").collect(java.util.stream.Collectors.joining(",\n              ")) +
                "\n            ]\n          }";
        if (targetPattern.matcher(content).find()) {
            content = targetPattern.matcher(content).replaceAll(java.util.regex.Matcher.quoteReplacement(targetEnumJson));
        } else {
            content = replaceEnum(content, "target", allTargets);
        }

        // Update job enum from EJob
        content = replaceEnum(content, "job", getEnumNamesFromSource("src/main/java/fr/miuby/survi/job/EJob.java"));

        // Update potion effect type enum
        List<String> potionEffects = getPotionEffectTypeNames().stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();
        content = replaceEnum(content, "type", potionEffects);

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

        // Update Quest Difficulty
        content = replaceEnum(content, "questDifficulty", getEnumNamesFromSource("src/main/java/fr/miuby/survi/quest/QuestDifficulty.java"));

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

        // Enrichir avec les YML (parfois utilisé comme target dans les quêtes)
        entities.addAll(collectValuesFromYml("target"));

        return entities.stream().toList();
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
                            // Regex pour trouver "key: VALUE" ou "key: 'VALUE'" ou "key: "VALUE""
                            Pattern pattern = Pattern.compile("(?m)^\\s*" + key + ":\\s*[\"']?([A-Z0-9_]+)[\"']?");
                            Matcher matcher = pattern.matcher(content);
                            while (matcher.find()) {
                                values.add(matcher.group(1).toUpperCase());
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