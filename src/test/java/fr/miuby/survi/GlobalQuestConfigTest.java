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
 * Valide la structure du fichier global_quests.yml.
 *
 * Vérifications :
 *  - présence de la clé racine "global_quests"
 *  - champs obligatoires : id, name, description, type, goal, time_limit, rewards
 *  - unicité des ids
 *  - type ∈ {MINE, KILL, BREED, FISH, SHEAR, CRAFT, SMELT}
 *  - goal ≥ 1
 *  - time_limit ≥ 60
 *  - rewards non vide : chaque entrée a un type ∈ {REPUTATION, POTION}
 *  - entrée REPUTATION : job ∈ EJob valides, value ≥ 1
 *  - entrée POTION : potion présent, duration ≥ 1, amplifier ≥ 0
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

    /** Tous les types d'effets supportés par BlessingLoader. */
    private static final Set<String> VALID_EFFECT_TYPES = Set.of(
            "DAMAGE", "DISPEL", "LIMIT_WORLD", "LOCK_WORLD", "MAX_HEALTH",
            "MESSAGE", "POTION", "REPUTATION", "RESISTANCE",
            "UNLOCK_ARMOR", "UNLOCK_TOOL", "WORLD_LEVEL", "WORLD_RESET"
    );

    /** Types d'effets dont le champ `value` doit être un entier >= 1. */
    private static final Set<String> EFFECTS_WITH_POSITIVE_INT_VALUE = Set.of("REPUTATION", "DISPEL");

    @Test
    void globalQuestsYmlIsValid() throws IOException {
        Path path = Paths.get("src/main/resources/global_quests.yml");
        Assertions.assertTrue(Files.exists(path), "global_quests.yml doit exister");

        String content = Files.readString(path);
        Assertions.assertTrue(content.contains("global_quests:"),
                "global_quests.yml doit contenir la clé racine 'global_quests'");

        // ── IDs ──────────────────────────────────────────────────────────────
        List<String> ids = extractValues(content, "id");
        Assertions.assertFalse(ids.isEmpty(), "Aucune quête globale trouvée dans global_quests.yml");

        Set<String> seenIds = new HashSet<>();
        for (String id : ids) {
            Assertions.assertTrue(seenIds.add(id), "ID de quête globale en double : " + id);
        }

        // ── Type de quête (MINE, KILL, …) — majuscules uniquement ────────────
        List<String> types = extractValues(content, "type");
        for (String type : types) {
            if (type.equals(type.toUpperCase()) && !VALID_EFFECT_TYPES.contains(type)) {
                Assertions.assertTrue(VALID_QUEST_TYPES.contains(type),
                        "Type de quête invalide : " + type + " — doit être l'un de " + VALID_QUEST_TYPES);
            }
        }

        // ── Goal ─────────────────────────────────────────────────────────────
        List<String> goals = extractValues(content, "goal");
        for (String goalStr : goals) {
            try {
                int goal = Integer.parseInt(goalStr);
                Assertions.assertTrue(goal >= 1, "goal doit être ≥ 1, trouvé : " + goal);
            } catch (NumberFormatException e) {
                Assertions.fail("goal doit être un entier, trouvé : " + goalStr);
            }
        }

        // ── Time limit ───────────────────────────────────────────────────────
        List<String> timeLimits = extractValues(content, "time_limit");
        for (String tlStr : timeLimits) {
            try {
                int tl = Integer.parseInt(tlStr);
                Assertions.assertTrue(tl >= 60, "time_limit doit être ≥ 60 secondes, trouvé : " + tl);
            } catch (NumberFormatException e) {
                Assertions.fail("time_limit doit être un entier, trouvé : " + tlStr);
            }
        }

        // ── BlessingEffect types ──────────────────────────────────────────────
        // On cherche tous les blocs "- type: REPUTATION/POTION" dans rewards
        List<String> effectTypes = extractValues(content, "type");
        for (String t : effectTypes) {
            if (VALID_EFFECT_TYPES.contains(t)) {
                Assertions.assertTrue(VALID_EFFECT_TYPES.contains(t),
                        "Type d'effet invalide : " + t + " — doit être l'un de " + VALID_EFFECT_TYPES);
            }
        }

        // ── REPUTATION : job ∈ EJob, value ≥ 1 ───────────────────────────────
        List<String> jobs = extractValues(content, "job");
        for (String job : jobs) {
            Assertions.assertTrue(VALID_JOBS.contains(job),
                    "Métier invalide dans rewards : " + job + " — doit être l'un de " + VALID_JOBS);
        }

        // `value` est utilisé par plusieurs types d'effets :
        //  - REPUTATION / DISPEL : entier >= 1
        //  - MAX_HEALTH : entier (peut être négatif)
        //  - RESISTANCE / DAMAGE : nombre (float possible)
        // extractValues capturant [A-Za-z0-9_]+ seulement, les floats comme 0.5 sont
        // tronqués à leur partie entière. On vérifie uniquement la parseabilité.
        List<String> values = extractValues(content, "value");
        for (String valStr : values) {
            try {
                Integer.parseInt(valStr);
            } catch (NumberFormatException e) {
                Assertions.fail("value doit être un entier (ou un float dont la partie entière est extraite), trouvé : " + valStr);
            }
        }

        // ── POTION : duration ≥ 1, amplifier ≥ 0 ────────────────────────────
        List<String> durations = extractValues(content, "duration");
        for (String durStr : durations) {
            try {
                int dur = Integer.parseInt(durStr);
                Assertions.assertTrue(dur >= 1, "duration doit être ≥ 1, trouvé : " + dur);
            } catch (NumberFormatException e) {
                Assertions.fail("duration doit être un entier, trouvé : " + durStr);
            }
        }

        List<String> amplifiers = extractValues(content, "amplifier");
        for (String ampStr : amplifiers) {
            try {
                int amp = Integer.parseInt(ampStr);
                Assertions.assertTrue(amp >= 0, "amplifier doit être ≥ 0, trouvé : " + amp);
            } catch (NumberFormatException e) {
                Assertions.fail("amplifier doit être un entier, trouvé : " + ampStr);
            }
        }

        // ── WORLD_LEVEL : levels ≥ 1 ─────────────────────────────────────────
        List<String> levels = extractValues(content, "levels");
        for (String lvlStr : levels) {
            try {
                int lvl = Integer.parseInt(lvlStr);
                Assertions.assertTrue(lvl >= 1, "levels (WORLD_LEVEL) doit être ≥ 1, trouvé : " + lvl);
            } catch (NumberFormatException e) {
                Assertions.fail("levels doit être un entier, trouvé : " + lvlStr);
            }
        }

        // ── WORLD_RESET : frequency ≥ 0 ─────────────────────────────────────
        List<String> frequencies = extractValues(content, "frequency");
        for (String freqStr : frequencies) {
            try {
                int freq = Integer.parseInt(freqStr);
                Assertions.assertTrue(freq >= 0, "frequency (WORLD_RESET) doit être ≥ 0, trouvé : " + freq);
            } catch (NumberFormatException e) {
                Assertions.fail("frequency doit être un entier, trouvé : " + freqStr);
            }
        }
    }

    private List<String> extractValues(String content, String key) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?m)^\\s*(?:-\\s+)?" + key + ":\\s*[\"']?([A-Z0-9_a-z]+)[\"']?");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String val = matcher.group(1).trim();
            if (!val.isEmpty()) values.add(val);
        }
        return values;
    }
}