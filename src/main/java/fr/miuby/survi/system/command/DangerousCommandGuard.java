package fr.miuby.survi.system.command;

import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Garde-fou "retape pour confirmer" pour les commandes admin destructrices ou difficiles à
 * annuler en prod (reset de monde, SQL arbitraire, purge de conteneurs, progression de quête
 * globale...).
 *
 * <p>Le premier appel pour un {@code (sender, commande exacte)} donné arme une confirmation en
 * attente pendant {@link #CONFIRM_WINDOW_MS} ms, avertit le sender et bloque l'exécution. Retaper
 * exactement la même commande (ou cliquer sur le bouton envoyé) dans la fenêtre exécute l'action.
 * Toute commande différente entre-temps remplace la confirmation en attente : une commande anodine
 * ne peut donc jamais "armer" par accident une commande dangereuse différente.</p>
 *
 * <h3>Utilisation dans un handler Brigadier</h3>
 * <pre>
 * .executes(ctx -> {
 *     LangService ls = GameManager.getInstance().getLangService();
 *     ELang lang = ls.resolveOrDefault(ctx.getSource().getSender());
 *     if (!DangerousCommandGuard.confirm(ctx, "worldreset", ls.text(lang, "cmd.worldreset.confirm_desc"))) {
 *         return Command.SINGLE_SUCCESS;
 *     }
 *     // ... action réelle ...
 * })
 * </pre>
 */
public final class DangerousCommandGuard {

    private static final long CONFIRM_WINDOW_MS = 15_000L;

    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

    private DangerousCommandGuard() {
        /* This utility class should not be instantiated */
    }

    /**
     * Vérifie (ou arme) la confirmation d'une action dangereuse.
     *
     * @param ctx             contexte Brigadier de la commande en cours
     * @param actionKey       identifiant technique stable de l'action, pour les logs uniquement (ex: "worldreset")
     * @param actionDescription description localisée de l'action, déjà résolue via {@link LangService}
     * @return {@code true} si l'action vient d'être confirmée et doit s'exécuter,
     *         {@code false} si un avertissement vient d'être envoyé et l'exécution doit s'arrêter là
     */
    public static boolean confirm(CommandContext<CommandSourceStack> ctx, String actionKey, Component actionDescription) {
        CommandSender sender = ctx.getSource().getSender();
        String senderKey = senderKey(sender);
        String rawInput = ctx.getInput();
        long now = System.currentTimeMillis();

        Pending pending = PENDING.get(senderKey);
        if (pending != null && pending.rawInput().equals(rawInput) && now <= pending.expiresAt()) {
            PENDING.remove(senderKey);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM, "[DangerousCommandGuard] " + sender.getName() + " a confirmé \"" + actionKey + "\" -> exécution (" + rawInput + ")");
            return true;
        }

        PENDING.put(senderKey, new Pending(rawInput, now + CONFIRM_WINDOW_MS));
        warn(sender, rawInput, actionDescription);

        MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                "[DangerousCommandGuard] " + sender.getName() + " a déclenché \"" + actionKey
                        + "\" (" + rawInput + ") - confirmation requise sous " + (CONFIRM_WINDOW_MS / 1000) + "s");
        return false;
    }

    private static void warn(CommandSender sender, String rawInput, Component actionDescription) {
        LangService ls = GameManager.getInstance().getLangService();
        ELang lang = ls.resolveOrDefault(sender);
        String rerun = rawInput.startsWith("/") ? rawInput : "/" + rawInput;

        sender.sendMessage(ls.text(lang, "cmd.confirm.warning",
                Placeholder.component("desc", actionDescription),
                Placeholder.unparsed("command", rerun),
                Placeholder.unparsed("seconds", String.valueOf(CONFIRM_WINDOW_MS / 1000))));

        if (sender instanceof Player) {
            Component button = ls.text(lang, "cmd.confirm.button")
                    .clickEvent(ClickEvent.runCommand(rerun))
                    .hoverEvent(HoverEvent.showText(ls.text(lang, "cmd.confirm.button_hover")));
            sender.sendMessage(button);
        }
    }

    private static String senderKey(CommandSender sender) {
        return sender instanceof Player p ? p.getUniqueId().toString() : "console:" + sender.getName();
    }

    private record Pending(String rawInput, long expiresAt) {}
}