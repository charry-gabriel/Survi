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
import java.util.logging.Level;

/**
 * Service de traduction des messages joueur.
 *
 * <p>Charge {@code lang/fr.yml} et {@code lang/en.yml} depuis le dossier de données du plugin.
 * La langue d'un joueur est déterminée par {@code player.locale().getLanguage()} avec repli
 * sur {@link #serverDefault} si la langue du client n'est pas supportée.</p>
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
 * player.sendMessage(ls.text(player, LangKey.WORLD_LOCKED));
 *
 * // Avec args positionnels
 * player.sendMessage(ls.text(player, LangKey.GRAVE_CREATED, x, y, z, worldName));
 *
 * // Avec TagResolvers (composants Adventure)
 * player.sendMessage(ls.text(player, LangKey.JOB_LEVEL_UP_BROADCAST,
 *     Placeholder.unparsed("player", pseudo),
 *     Placeholder.unparsed("level", "niv." + level),
 *     Placeholder.component("job", job.toComponent())
 * ));
 *
 * // Broadcast à tous les joueurs dans leur langue
 * ls.broadcast(LangKey.WORLD_LEVEL_UP_BROADCAST, oldLevel, newLevel);
 * </pre>
 */
public class LangService {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<ELang, Map<String, String>> translations = new EnumMap<>(ELang.class);

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
                    "[LangService] Fichier manquant : lang/" + lang.getCode() + ".yml — fallback enum activé.");
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
    public Component text(Player player, LangKey key) {
        return text(resolveLanguage(player), key);
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(Player player, LangKey key, Object... args) {
        return text(resolveLanguage(player), key, args);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(Player player, LangKey key, TagResolver... resolvers) {
        return text(resolveLanguage(player), key, resolvers);
    }

    // =========================================================================
    // text() — composant pour une langue donnée
    // =========================================================================

    /** Résout un message dans une langue précise, sans arguments. */
    public Component text(ELang lang, LangKey key) {
        return MM.deserialize(resolve(lang, key));
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(ELang lang, LangKey key, Object... args) {
        String template = resolve(lang, key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", MM.escapeTags(String.valueOf(args[i])));
        }
        return MM.deserialize(template);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(ELang lang, LangKey key, TagResolver... resolvers) {
        String template = resolve(lang, key);
        return resolvers.length == 0 ? MM.deserialize(template) : MM.deserialize(template, resolvers);
    }

    // =========================================================================
    // broadcast() — envoi à tous les joueurs en ligne dans leur langue
    // =========================================================================

    /** Envoie un message à tous les joueurs en ligne, chacun dans sa langue. */
    public void broadcast(LangKey key) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key));
    }

    /** Envoie avec placeholders positionnels. */
    public void broadcast(LangKey key, Object... args) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, args));
    }

    /**
     * Envoie avec TagResolvers partagés (même valeur pour tous les joueurs).
     * Ne pas utiliser si la valeur d'un resolver dépend du joueur.
     */
    public void broadcast(LangKey key, TagResolver... resolvers) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, resolvers));
    }

    // =========================================================================
    // Résolution interne du template
    // =========================================================================

    private String resolve(ELang lang, LangKey key) {
        Map<String, String> map = translations.get(lang);
        if (map != null) {
            String val = map.get(key.getKey());
            if (val != null) return val;
        }
        // Repli sur l'anglais si la clé est absente dans la langue demandée
        if (lang != ELang.EN) {
            Map<String, String> en = translations.get(ELang.EN);
            if (en != null) {
                String val = en.get(key.getKey());
                if (val != null) return val;
            }
        }
        // Repli ultime sur le fallback de l'enum
        return key.getFallback();
    }
}