package fr.miuby.survi.system.lang;

import lombok.Getter;

/**
 * Clés de traduction pour tous les messages affichés aux joueurs.
 *
 * <p>Les fichiers YAML ({@code lang/fr.yml}, {@code lang/en.yml}) contiennent les chaînes
 * au format MiniMessage. Les placeholders positionnels {@code {0}}, {@code {1}}… sont remplacés
 * par des chaînes simples. Les placeholders nommés {@code <name>} sont des TagResolvers Adventure.
 *
 * <p>Le fallback de chaque clé est la chaîne anglaise qui s'applique si le fichier YAML est absent.
 */
public enum LangKey {

    // ─── Limite de zone ──────────────────────────────────────────────────────────
    BOUNDARY_WARNING("boundary.warning",
            "<red>You can't go any further!"),

    // ─── Tombe ───────────────────────────────────────────────────────────────────
    GRAVE_INDESTRUCTIBLE("grave.indestructible",
            "<red>This grave is indestructible!"),
    GRAVE_CREATED("grave.created",
            "<green>☠ Your grave was created at {0}, {1}, {2} [{3}]"),
    GRAVE_NOT_YOURS("grave.not_yours",
            "<red>This is not your grave!"),
    GRAVE_RECOVERED("grave.recovered",
            "<green>You have retrieved your grave."),

    // ─── Monde ───────────────────────────────────────────────────────────────────
    WORLD_LOCKED("world.locked",
            "<red>✖ This world is not yet accessible!"),
    WORLD_LEVEL_UP_BROADCAST("world.level_up.broadcast",
            "<gold>✦ The world has evolved! <yellow>Level {0} <gray>→ <gold>Level {1}"),
    WORLD_LEVEL_UP_TITLE("world.level_up.title",
            "<gold>✦ World — Level {0}"),
    WORLD_LEVEL_UP_SUBTITLE("world.level_up.subtitle",
            "<yellow>The world grows more dangerous…"),

    // ─── Métier ──────────────────────────────────────────────────────────────────
    JOB_LEVEL_UP_TITLE("job.level_up.title",
            "<gold>⚒ Level Up!"),
    /** Suffixe du subtitle de level-up : {0} = "niv.X" ou "lv.X". Préfixé par {@code job.toComponent()}. */
    JOB_LEVEL_UP_SUBTITLE_SUFFIX("job.level_up.subtitle_suffix",
            "<yellow> · lv.{0}"),
    /** Placeholders nommés : {@code <player>} (String), {@code <level>} (String), {@code <job>} (Component). */
    JOB_LEVEL_UP_BROADCAST("job.level_up.broadcast",
            "<gold>⚒ <white><player> <gold>has reached <yellow><level> <gold>in <job><gold>!"),
    /** Préfixe textuel avant {@code job.toComponent()} dans la note de montée de niveau. */
    JOB_NOTE_PREFIX("job.note_prefix",
            "<green>⚒ "),

    // ─── Joueur ───────────────────────────────────────────────────────────────────
    PLAYER_SUBROLE_ADDED_PREFIX("player.subrole.added_prefix",
            "<yellow>The sub-role "),
    PLAYER_SUBROLE_ADDED_SUFFIX("player.subrole.added_suffix",
            "<yellow> has been added!"),
    PLAYER_SUBROLE_REMOVED_PREFIX("player.subrole.removed_prefix",
            "<yellow>The sub-role "),
    PLAYER_SUBROLE_REMOVED_SUFFIX("player.subrole.removed_suffix",
            "<yellow> has been removed!"),
    /** Préfixe avant {@code newRank.displayComponent()}. */
    PLAYER_RANK_UP_PREFIX("player.rank_up.prefix",
            "<gold>✦ New rank achieved: "),
    /** Suffixe après {@code newRank.displayComponent()} : {0} = réputation totale. */
    PLAYER_RANK_UP_SUFFIX("player.rank_up.suffix",
            "<gray> (total reputation: {0})"),

    // ─── Quête ───────────────────────────────────────────────────────────────────
    QUEST_ALREADY_CLAIMED("quest.already_claimed",
            "<red>You already have a completed quest to claim!"),
    QUEST_ALREADY_ACTIVE("quest.already_active",
            "<red>You already have an active quest. Finish it first!"),
    QUEST_NOT_AVAILABLE("quest.not_available",
            "<red>No quest is available yet. Wait for the game to start!"),
    /** {0} = complétées, {1} = capacité. */
    QUEST_CAPACITY_FULL("quest.capacity_full",
            "<red>You've completed {0}/{1} available quests. Come back tomorrow!"),
    /** Préfixe avant {@code Component.text(traderId)} pour demander de valider. */
    QUEST_VALIDATE_TRADER_PREFIX("quest.validate_trader_prefix",
            "<red>This quest must be validated at "),
    /** Préfixe avant {@code Component.text(traderNameId)} lors de l'acceptation. */
    QUEST_ACCEPTED_PREFIX("quest.accepted.prefix",
            "<green>New quest accepted from "),
    QUEST_ACCEPTED_SEPARATOR("quest.accepted.separator",
            "<green> : "),
    /** {0} = slot utilisé, {1} = capacité totale. */
    QUEST_ACCEPTED_SLOTS("quest.accepted.slots",
            "<dark_gray>Quest {0}/{1} available."),
    /** Préfixe avant {@code Component.text(quest.getName())} à la complétion. */
    QUEST_COMPLETED_PREFIX("quest.completed.prefix",
            "<green>Quest completed: "),
    QUEST_COMPLETED_GO_TRADER("quest.completed.go_trader",
            "<gray>Go see the Trader to claim your reward!"),
    /** Préfixe avant {@code Component.text(used + "/" + capacity)} pour les récompenses. */
    QUEST_REWARDS_RECEIVED_PREFIX("quest.rewards_received.prefix",
            "<green>You have received the quest rewards! "),
    QUEST_REWARDS_RECEIVED_SUFFIX("quest.rewards_received.suffix",
            "<green> quests completed."),
    QUEST_RESET_BY_ADMIN("quest.reset_by_admin",
            "<yellow>Your quest has been reset by an admin. You can now accept a new one!"),
    QUEST_DROPPED_ON_RELOAD("quest.dropped_on_reload",
            "<yellow>Your active quest was removed due to an update. You can accept a new one from a Trader."),
    /** {0} = nom de la quête orpheline récompensée automatiquement. */
    QUEST_ORPHAN_REWARDED("quest.orphan_rewarded",
            "<green>Your quest «{0}» was automatically rewarded following an update."),
    /** Préfixe avant {@code Component.text(quest.getName())} en mode test. */
    QUEST_TEST_PREFIX("quest.test.prefix",
            "<yellow>[TEST] Test quest assigned: "),
    /** {0} = objectif, {1} = difficulté, {2} = métiers. */
    QUEST_TEST_DETAILS("quest.test.details",
            "<dark_gray>Goal: {0} | Difficulty: {1} | Jobs: {2}"),
    QUEST_NEW_SLOTS_PREFIX("quest.new_slots.prefix",
            "<gold>[Quests] "),
    /** {0} = créneaux libres, {1} = utilisés, {2} = capacité totale. */
    QUEST_NEW_SLOTS_BODY("quest.new_slots.body",
            "<green>{0} new slot(s) available! <dark_gray>({1}/{2} used)"),

    // ─── ActionBar quête journalière ─────────────────────────────────────────────
    QUEST_ACTIONBAR_QUESTS_SUFFIX("quest.actionbar.quests_suffix",
            "<dark_gray> quests]"),
    QUEST_ACTIONBAR_FINISHED_PREFIX("quest.actionbar.finished_prefix",
            "<bold><green>✔ Quest completed: "),
    QUEST_ACTIONBAR_FINISHED_SUFFIX("quest.actionbar.finished_suffix",
            "<gray> — Go see the Trader!"),

    // ─── BossBar quête globale ────────────────────────────────────────────────────
    /** {0} = nom, {1} = progression, {2} = objectif, {3} = pourcentage. */
    QUEST_GLOBAL_BOSSBAR_CONTENT("quest.global.bossbar.content",
            "⚔ {0}  ▶  {1}/{2}  ({3}%)"),
    /** {0} = nom de la quête terminée. */
    QUEST_GLOBAL_BOSSBAR_FINISHED("quest.global.bossbar.finished",
            "✔ {0}  —  Completed!"),

    // ─── Notifications hors-ligne ─────────────────────────────────────────────────
    OFFLINE_HEADER("offline.header",
            "<dark_gray>─── While you were away ───"),
    /** Placeholders nommés : {@code <job>} (Component), {@code <old>} et {@code <new>} (String). */
    OFFLINE_JOB_LEVEL("offline.job_level",
            "<gold>⚒ Job <job><gold>: <gray>lv.<old> <gray>→ <yellow>lv.<new>"),
    /** {0} = ancien niveau, {1} = nouveau niveau. */
    OFFLINE_WORLD_LEVEL("offline.world_level",
            "<gold>✦ World level: <yellow>Level {0} <gray>→ <gold>Level {1}"),
    /** Placeholders nommés : {@code <villager>} (Component), {@code <old>} et {@code <new>} (String). */
    OFFLINE_VILLAGER_LEVEL("offline.villager_level",
            "<gold>🏠 <villager><gold>: <gray>Level <old> <gray>→ <yellow>Level <new>"),
    /** {0} = nom de la quête globale. */
    OFFLINE_GLOBAL_QUEST_PREFIX("offline.global_quest.prefix",
            "<gold>⚔ Global quest in progress: <yellow>«{0}»"),
    /** {0} = progression, {1} = objectif, {2} = pourcentage, {3} = temps restant. */
    OFFLINE_GLOBAL_QUEST_PROGRESS("offline.global_quest.progress",
            "<aqua>{0}/{1} ({2}%)  <gray>⏰ {3}"),

    // ─── Récapitulatif d'effets à la connexion ────────────────────────────────────
    EFFECT_RESTORE_HEADER("effect.restore.header",
            "<yellow>-------------------- Recap --------------------"),
    /** {0} = niveau du monde. */
    EFFECT_RESTORE_WORLD_LEVEL("effect.restore.world_level",
            "<yellow>Difficulty Lv. {0}"),
    EFFECT_RESTORE_FOOTER("effect.restore.footer",
            "<yellow>----------------------------------------------"),

    // ─── Enchanteur ───────────────────────────────────────────────────────────────
    /** Placeholder nommé : {@code <job>} (Component). */
    ENCHANTER_NO_LEVEL("enchanter.no_level",
            "<red>✗ You can't enchant yet. Progress in the job <job><red>."),
    /** Placeholder nommé : {@code <job>} (Component). {0} = niveaux XP max. */
    ENCHANTER_XP_TOO_HIGH("enchanter.xp_too_high",
            "<red>✗ This slot costs too many XP levels for your job rank <job><red> (max {0} levels)."),
    /** Placeholder nommé : {@code <job>} (Component). {0} = niveau métier max. */
    ENCHANTER_LEVEL_TOO_HIGH("enchanter.level_too_high",
            "<red>✗ This enchantment exceeds your job level <job><red> (max lv.{0} enchantment)."),

    // ─── Villageois ───────────────────────────────────────────────────────────────
    /** {0} = nom du villageois, {1} = durée restante du lock. */
    VILLAGER_LOCKED("villager.locked",
            "<yellow>{0} is unavailable for {1} more");

    @Getter private final String key;
    @Getter private final String fallback;

    LangKey(String key, String fallback) {
        this.key = key;
        this.fallback = fallback;
    }
}