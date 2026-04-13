package fr.miuby.survi;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaGeneratorTest {

    @Test
    public void updateSchemas() throws IOException {
        updateVillagersSchema();
        updateRecipesSchema();
        updateQuestsSchema();
    }

    private void updateVillagersSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/villagers-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Villager Type enum
        List<String> types = List.of("DESERT", "JUNGLE", "PLAINS", "SAVANNA", "SNOW", "SWAMP", "TAIGA");
        content = replaceEnum(content, "type", types);

        // Update Villager Profession enum
        List<String> professions = List.of("ARMORER", "BUTCHER", "CARTOGRAPHER", "CLERIC", "FARMER", "FISHERMAN", "FLETCHER", "LEATHERWORKER", "LIBRARIAN", "MASON", "NITWIT", "NONE", "SHEPHERD", "TOOLSMITH", "WEAPONSMITH");
        content = replaceEnum(content, "profession", professions);

        // Update Material enum (for tributes)
        List<String> materials = Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
        content = replaceEnum(content, "material", materials);

        Files.writeString(path, content);
    }

    private void updateRecipesSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/recipes-schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update Material definition enum
        List<String> materials = Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());

        // Update old_recipes enum (namespaced keys: minecraft:item_name)
        List<String> namespacedKeys = materials.stream()
                .map(m -> "minecraft:" + m.toLowerCase())
                .sorted()
                .collect(Collectors.toList());
        
        // Regex to find "old_recipes" and its enum items
        Pattern oldRecipesPattern = Pattern.compile("\"old_recipes\":\\s*\\{[^}]*\"items\":\\s*\\{[^}]*\"enum\":\\s*\\[[^\\]]*\\]");
        Matcher oldMatcher = oldRecipesPattern.matcher(content);
        if (oldMatcher.find()) {
            String match = oldMatcher.group();
            String enumJson = "\"enum\": [\n              " + 
                    namespacedKeys.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n              ")) + 
                    "\n            ]";
            String replaced = match.replaceAll("\"enum\":\\s*\\[[^\\]]*\\]", Matcher.quoteReplacement(enumJson));
            content = content.replace(match, replaced);
        }

        // Specially update the Material definition at the end
        Pattern materialPattern = Pattern.compile("\"Material\":\\s*\\{\\s*\"type\":\\s*\"string\",\\s*\"enum\":\\s*\\[[^\\]]*\\]\\s*\\}");
        String materialEnumJson = "\"Material\": {\n      \"type\": \"string\",\n      \"enum\": [\n        " + 
                materials.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n        ")) + 
                "\n      ]\n    }";
        content = materialPattern.matcher(content).replaceAll(Matcher.quoteReplacement(materialEnumJson));

        Files.writeString(path, content);
    }

    private void updateQuestsSchema() throws IOException {
        Path path = Paths.get("src/main/resources/schema/quests.schema.json");
        if (!Files.exists(path)) return;

        String content = Files.readString(path);

        // Update target enum if it exists, or add it.
        // In the provided quests.schema.json, target is: "target": { "type": ["string", "null"] }
        // We can improve it by adding an enum of Materials and EntityTypes.
        
        List<String> materials = Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .map(Enum::name)
                .collect(Collectors.toList());
        
        List<String> entities = Arrays.stream(EntityType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        
        List<String> allTargets = Stream.concat(materials.stream(), entities.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Replace target type with enum if it matches the pattern
        Pattern targetPattern = Pattern.compile("\"target\":\\s*\\{\\s*\"type\":\\s*\\[\"string\",\\s*\"null\"\\]\\s*\\}");
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

    private String replaceEnum(String content, String propertyName, List<String> values) {
        String enumJson = "\"enum\": [\n        " + 
                values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n        ")) + 
                "\n      ]";
        
        Pattern pattern = Pattern.compile("\"" + propertyName + "\":\\s*\\{[^}]*\"enum\":\\s*\\[[^\\]]*\\]");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String match = matcher.group();
            String replaced = match.replaceAll("\"enum\":\\s*\\[[^\\]]*\\]", Matcher.quoteReplacement(enumJson));
            return content.replace(match, replaced);
        }
        return content;
    }
}
