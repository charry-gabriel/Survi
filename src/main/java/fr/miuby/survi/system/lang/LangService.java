package fr.miuby.survi.system.lang;

import fr.miuby.lib.message.MLMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Service de traduction des messages joueur — façade fine sur {@link MLMessageService}
 * (MiubyLib), configuré en mono-langue française.
 *
 * <p>Toutes les traductions vivent dans {@code lang/fr.yml} (dossier de données du plugin,
 * déployé depuis {@code resources/lang/}), au format YAML + MiniMessage. Voir
 * {@link MLMessageService} pour le détail du format, des placeholders ({@code {0}}, {@code {1}}…
 * et {@code <name>} via {@link TagResolver}) et du filet "clé manquante".</p>
 *
 * <p>Cette classe ne fait que convertir {@link ELang} (enum à une seule valeur, {@code FR})
 * en codes de locale {@code String} pour {@link MLMessageService} — conservée pour ne pas
 * casser les call-sites existants ({@code text(ELang, ...)}, {@code resolveLanguage(Player)},
 * {@code getServerDefault()}...).</p>
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
 *     Placeholder.component("job", job.toComponent())
 * ));
 *
 * // Broadcast à tous les joueurs (mono-FR : trivial)
 * ls.broadcast("world.level_up.broadcast", oldLevel, newLevel);
 * </pre>
 */
public class LangService {

    /**
     * Message affiché au joueur quand une clé est absente de {@code lang/fr.yml}.
     * Volontairement très visible (rouge, gras) et contient la clé exacte à ajouter.
     */
    private static final String MISSING_KEY_TEMPLATE =
            "<red><bold>⚠ Traduction manquante (<white>{0}<red>) — merci de le signaler à un admin !";

    private final JavaPlugin plugin;
    private MLMessageService delegate;

    public LangService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.delegate = new MLMessageService(plugin, "lang", List.of("fr"), true, MISSING_KEY_TEMPLATE);
    }

    /** Recharge {@code lang/fr.yml} à chaud — reconstruit le délégué MiubyLib depuis le disque. */
    public void reload() {
        this.delegate = new MLMessageService(plugin, "lang", List.of("fr"), true, MISSING_KEY_TEMPLATE);
    }

    // =========================================================================
    // Résolution de la langue
    // =========================================================================

    /** Mono-FR : retourne toujours {@link ELang#FR}. */
    public ELang resolveLanguage(Player player) {
        return ELang.fromCode(delegate.resolveLanguage(player));
    }

    /** Résout la langue d'un {@link CommandSender} — mono-FR : retourne toujours {@link ELang#FR}. */
    public ELang resolveOrDefault(CommandSender sender) {
        return ELang.fromCode(delegate.resolveOrDefault(sender));
    }

    /** Mono-FR : retourne toujours {@link ELang#FR}. */
    public ELang getServerDefault() {
        return ELang.fromCode(delegate.getDefaultLocale());
    }

    // =========================================================================
    // text() — composant pour un joueur
    // =========================================================================

    /** Résout un message pour ce joueur, sans arguments. */
    public Component text(Player player, String key) {
        return delegate.text(player, key);
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(Player player, String key, Object... args) {
        return delegate.text(player, key, args);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(Player player, String key, TagResolver... resolvers) {
        return delegate.text(player, key, resolvers);
    }

    // =========================================================================
    // text() — composant pour une langue donnée
    // =========================================================================

    /** Résout un message dans une langue précise, sans arguments. */
    public Component text(ELang lang, String key) {
        return delegate.text(lang.getCode(), key);
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(ELang lang, String key, Object... args) {
        return delegate.text(lang.getCode(), key, args);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(ELang lang, String key, TagResolver... resolvers) {
        return delegate.text(lang.getCode(), key, resolvers);
    }

    // =========================================================================
    // broadcast() — envoi à tous les joueurs en ligne
    // =========================================================================

    /** Envoie un message à tous les joueurs en ligne. */
    public void broadcast(String key) {
        delegate.broadcast(key);
    }

    /** Envoie avec placeholders positionnels. */
    public void broadcast(String key, Object... args) {
        delegate.broadcast(key, args);
    }

    /** Envoie avec TagResolvers partagés (même valeur pour tous les joueurs). */
    public void broadcast(String key, TagResolver... resolvers) {
        delegate.broadcast(key, resolvers);
    }

    // =========================================================================
    // getString() — chaîne brute (pour insertion dans d'autres templates)
    // =========================================================================

    /** Retourne la chaîne brute (non parsée) d'une clé, utile pour la passer en arg positionnel. */
    public String getString(ELang lang, String key) {
        return delegate.getString(lang.getCode(), key);
    }
}