package fr.miuby.survi.system.lang;

import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.lib.log.MLLogManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service de traduction des messages joueur.
 *
 * <p>Toutes les traductions vivent dans {@code lang/fr.yml} et {@code lang/en.yml} (dossier
 * de données du plugin, déployés depuis {@code resources/lang/}). Il n'y a <b>pas d'enum
 * de clés</b> : une clé est une simple chaîne du type {@code "categorie.sous_categorie.nom"}.
 * Toute clé utilisée dans le code doit exister dans les DEUX fichiers YAML.</p>
 *
 * <h3>Clé manquante — pas de crash</h3>
 * <p>Si une clé est absente des deux fichiers (FR et EN), le joueur reçoit un message
 * d'avertissement bien visible (rouge, gras) contenant le nom exact de la clé, afin qu'il
 * puisse le signaler à un admin. Côté serveur, un warning est loggé une seule fois par clé
 * (pour ne pas spammer la console si le message s'affiche en boucle).</p>
 *
 * <h3>Format des templates (MiniMessage)</h3>
 * <ul>
 *   <li>{@code {0}}, {@code {1}}… — remplacements de chaînes simples (échappés pour MiniMessage)</li>
 *   <li>{@code <name>} — TagResolver Adventure (ex. composant coloré du nom d'un métier)</li>
 * </ul>
 *
 * <h3>API</h3>
 * <pre>
 * LangService ls = GameManager.getInstance().getLangService();
 *
 * // Message simple
 * player.sendMessage(ls.text(player, "world.locked"));
 *
 * // Avec args positionnels
 * player.sendMessage(ls.text(player, "grave.created", x, y, z, worldName));
 *
 * // Avec TagResolvers (composants Adventure)
 * player.sendMessage(ls.text(player, "job.level_up.broadcast",
 *     Placeholder.unparsed("player", pseudo),
 *     Placeholder.unparsed("level", "niv." + level),
 *     Placeholder.component("job", job.toComponent())
 * ));
 *
 * // Broadcast à tous les joueurs dans leur langue
 * ls.broadcast("world.level_up.broadcast", oldLevel, newLevel);
 * </pre>
 */
public class LangService {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Message affiché au joueur quand une clé est absente des deux fichiers YAML.
     * Volontairement très visible (rouge, gras) et contient la clé exacte à ajouter
     * dans {@code lang/fr.yml} et {@code lang/en.yml}.
     *
     * <p>Ce template n'est PAS dans les fichiers YAML (pour éviter le problème de
     * "clé manquante pour signaler une clé manquante"). Pour le modifier, éditer
     * directement cette constante.</p>
     */
    private static final String MISSING_KEY_TEMPLATE =
            "<red><bold>⚠ Traduction manquante (<white>{0}<red>) — merci de le signaler à un admin !";

    private final Map<ELang, Map<String, String>> translations = new EnumMap<>(ELang.class);

    /** Clés déjà signalées dans les logs, pour n'avertir qu'une seule fois par clé. */
    private final Set<String> loggedMissingKeys = ConcurrentHashMap.newKeySet();

    @Getter
    private final ELang serverDefault;

    // =========================================================================
    // Initialisation
    // =========================================================================

    public LangService(JavaPlugin plugin, ELang serverDefault) {
        this.serverDefault = serverDefault;
        for (ELang lang : ELang.values()) {
            translations.put(lang, loadFile(plugin, lang));
        }
        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[LangService] Initialisé — langue serveur : " + serverDefault.getCode()
                        + " — FR=" + translations.get(ELang.FR).size() + " clés"
                        + ", EN=" + translations.get(ELang.EN).size() + " clés");
    }

    private Map<String, String> loadFile(JavaPlugin plugin, ELang lang) {
        File file = new File(plugin.getDataFolder(), "lang/" + lang.getCode() + ".yml");
        if (!file.exists()) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                    "[LangService] Fichier manquant : lang/" + lang.getCode() + ".yml");
            return Collections.emptyMap();
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new HashMap<>();
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) map.put(key, cfg.getString(key));
        }
        return Collections.unmodifiableMap(map);
    }

    // =========================================================================
    // Résolution de la langue
    // =========================================================================

    /**
     * Détermine la langue à utiliser pour ce joueur.
     * Utilise {@code player.locale().getLanguage()} et replie sur {@link #serverDefault}.
     */
    public ELang resolveLanguage(Player player) {
        if (player == null) return serverDefault;
        String code = player.locale().getLanguage();
        for (ELang lang : ELang.values()) {
            if (lang.getCode().equalsIgnoreCase(code)) return lang;
        }
        return serverDefault;
    }

    // =========================================================================
    // text() — composant pour un joueur
    // =========================================================================

    /** Résout un message pour ce joueur, sans arguments. */
    public Component text(Player player, String key) {
        return text(resolveLanguage(player), key);
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(Player player, String key, Object... args) {
        return text(resolveLanguage(player), key, args);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(Player player, String key, TagResolver... resolvers) {
        return text(resolveLanguage(player), key, resolvers);
    }

    // =========================================================================
    // text() — composant pour une langue donnée
    // =========================================================================

    /** Résout un message dans une langue précise, sans arguments. */
    public Component text(ELang lang, String key) {
        return MM.deserialize(resolve(lang, key));
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(ELang lang, String key, Object... args) {
        String template = resolve(lang, key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", MM.escapeTags(String.valueOf(args[i])));
        }
        return MM.deserialize(template);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(ELang lang, String key, TagResolver... resolvers) {
        String template = resolve(lang, key);
        return resolvers.length == 0 ? MM.deserialize(template) : MM.deserialize(template, resolvers);
    }

    // =========================================================================
    // broadcast() — envoi à tous les joueurs en ligne dans leur langue
    // =========================================================================

    /** Envoie un message à tous les joueurs en ligne, chacun dans sa langue. */
    public void broadcast(String key) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key));
    }

    /** Envoie avec placeholders positionnels. */
    public void broadcast(String key, Object... args) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, args));
    }

    /**
     * Envoie avec TagResolvers partagés (même valeur pour tous les joueurs).
     * Ne pas utiliser si la valeur d'un resolver dépend du joueur.
     */
    public void broadcast(String key, TagResolver... resolvers) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, resolvers));
    }

    // =========================================================================
    // Résolution interne du template
    // =========================================================================

    /**
     * Résout le template brut pour {@code key} dans {@code lang}, avec repli sur l'anglais,
     * puis sur le message "clé manquante" si la clé n'existe nulle part.
     */
    private String resolve(ELang lang, String key) {
        Map<String, String> map = translations.get(lang);
        if (map != null) {
            String val = map.get(key);
            if (val != null) return val;
        }
        // Repli sur l'anglais si la clé est absente dans la langue demandée
        if (lang != ELang.EN) {
            Map<String, String> en = translations.get(ELang.EN);
            if (en != null) {
                String val = en.get(key);
                if (val != null) return val;
            }
        }
        // Clé absente des deux langues : on avertit (log une fois) et on renvoie
        // un message visible contenant la clé, pour que le joueur puisse signaler le bug.
        if (loggedMissingKeys.add(key)) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                    "[LangService] Clé de traduction manquante : \"" + key
                            + "\" — ajoutez-la dans lang/fr.yml et lang/en.yml.");
        }
        return MISSING_KEY_TEMPLATE.replace("{0}", MM.escapeTags(key));
    }
}