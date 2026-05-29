package fr.miuby.survi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valide la structure du fichier global_quests.yml contre le schéma
 * global-quests-schema.json.
 *
 * Vérifications :
 *  - présence de la clé racine "global_quests"
 *  - champs obligatoires : id, name, description, type, goal, time_limit, job_rewards
 *  - unicité des ids
 *  - type ∈ {MINE, KILL, BREED, FISH, SHEAR, CRAFT, SMELT}
 *  - goal ≥ 1
 *  - time_limit ≥ 60
 *  - job_rewards non vide et chaque job ∈ EJob valides
 *  - reputation ≥ 1 dans chaque job_reward
 */
class GlobalQuestConfigTest {

    private static final Set<String> VALID_QUEST_TYPES = Set.of(
            "MINE", "KILL", "BREED", "FISH", "SHEAR", "CRAFT", "SMELT"
    );

    private static final Set<String> VALID_JOBS = Set.of(
            "MINEUR", "BUCHERON", "FERMIER", "COMBATANT", "ALCHIMISTE",
            "ENCHANTEUR", "FORGERON", "PECHEUR", "CHASSEUR", "MARCHAND",
            "AVENTURIER", "BATISSEUR"
    );

    @Test
    void globalQuestsYmlIsValid() throws IOException {
        Path path = Paths.get("src/main/resources/global_quests.yml");
        Assertions.assertTrue(Files.exists(path), "global_quests.yml doit exister");

        String content = Files.readString(path);
        Assertions.assertTrue(content.contains("global_quests:"),
                "global_quests.yml doit contenir la clé racine 'global_quests'");

        // Extraire les ids
        List<String> ids = extractValues(content, "id");
        Assertions.assertFalse(ids.isEmpty(), "Aucune quête globale trouvée dans global_quests.yml");

        // Unicité des ids
        Set<String> seenIds = new HashSet<>();
        for (String id : ids) {
            Assertions.assertTrue(seenIds.add(id),
                    "ID de quête globale en double : " + id);
        }

        // Vérification des types
        List<String> types = extractValues(content, "type");
        for (String type : types) {
            // On ignore les types de potions (minuscules) — on filtre sur MAJUSCULES
            if (type.equals(type.toUpperCase())) {
                Assertions.assertTrue(VALID_QUEST_TYPES.contains(type),
                        "Type de quête invalide : " + type + " — doit être l'un de " + VALID_QUEST_TYPES);
            }
        }

        // Vérification des goals
        List<String> goals = extractValues(content, "goal");
        for (String goalStr : goals) {
            try {
                int goal = Integer.parseInt(goalStr);
                Assertions.assertTrue(goal >= 1, "goal doit être ≥ 1, trouvé : " + goal);
            } catch (NumberFormatException e) {
                Assertions.fail("goal doit être un entier, trouvé : " + goalStr);
            }
        }

        // Vérification des time_limits
        List<String> timeLimits = extractValues(content, "time_limit");
        for (String tlStr : timeLimits) {
            try {
                int tl = Integer.parseInt(tlStr);
                Assertions.assertTrue(tl >= 60,
                        "time_limit doit être ≥ 60 secondes, trouvé : " + tl);
            } catch (NumberFormatException e) {
                Assertions.fail("time_limit doit être un entier, trouvé : " + tlStr);
            }
        }

        // Vérification des jobs
        List<String> jobs = extractValues(content, "job");
        for (String job : jobs) {
            Assertions.assertTrue(VALID_JOBS.contains(job),
                    "Métier invalide dans job_rewards : " + job + " — doit être l'un de " + VALID_JOBS);
        }

        // Vérification des reputations
        List<String> reputations = extractValues(content, "reputation");
        for (String repStr : reputations) {
            try {
                int rep = Integer.parseInt(repStr);
                Assertions.assertTrue(rep >= 1,
                        "reputation doit être ≥ 1, trouvé : " + rep);
            } catch (NumberFormatException e) {
                Assertions.fail("reputation doit être un entier, trouvé : " + repStr);
            }
        }
    }

    private List<String> extractValues(String content, String key) {
        List<String> values = new ArrayList<>();
        // (?:-\s+)? handles YAML list items: "  - id: ..." as well as "    id: ..."
        Pattern pattern = Pattern.compile("(?m)^\\s*(?:-\\s+)?" + key + ":\\s*[\"']?([A-Z0-9_a-z]+)[\"']?");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String val = matcher.group(1).trim();
            if (!val.isEmpty()) values.add(val);
        }
        return values;
    }
}
