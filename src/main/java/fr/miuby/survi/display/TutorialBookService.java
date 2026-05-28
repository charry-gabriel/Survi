package fr.miuby.survi.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Génère et distribue le "Guide de la survi" (livre écrit) aux nouveaux joueurs.
 *
 * <p>Utilisation : appeler {@link #giveTutorialBookIfNew(Player)} dans le
 * {@code ServerListener.onPlayerJoin} lorsque {@code player.hasPlayedBefore() == false}.
 */
public class TutorialBookService {

    private static final String BOOK_TITLE  = "Guide de la survi";
    private static final String BOOK_AUTHOR = "TeamAlpha";

    private TutorialBookService() {}

    public static void giveTutorialBookIfNew(Player player) {
        if (player.hasPlayedBefore()) return;
        giveTutorialBook(player);
    }

    public static void giveTutorialBook(Player player) {
        ItemStack book = createTutorialBook();
        player.getInventory().addItem(book).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));

        player.sendMessage(Component.text()
                .append(Component.text("[Survi] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Bienvenue ! Tu as reçu le "))
                .append(Component.text(BOOK_TITLE, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" dans ton inventaire."))
                .build());
    }

    private static ItemStack createTutorialBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta  = (BookMeta) book.getItemMeta();
        meta.setTitle(BOOK_TITLE);
        meta.setAuthor(BOOK_AUTHOR);
        meta.pages(buildPages());
        book.setItemMeta(meta);
        return book;
    }

    private static List<Component> buildPages() {
        List<Component> pages = new ArrayList<>();

        // ── Page 1 : Sommaire ─────────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("Bienvenue !\n\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Ce serveur modifie beaucoup de mécaniques. Lis ce guide.\n\n"))
                .append(Component.text("Sommaire\n", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .append(Component.text("I.  Les Villageois\n"))
                .append(Component.text("II. Métiers\n"))
                .append(Component.text("III.Quêtes\n"))
                .append(Component.text("IV. Crafts custom"))
                .build());

        // ── Page 2 : Les Villageois ───────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("I. Les Villageois\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("C'est la base du jeu.\n\n"))
                .append(Component.text("Apporte des "))
                .append(Component.text("tributs").decorate(TextDecoration.BOLD))
                .append(Component.text(" (items) aux PNJ du village.\nEn échange : des "))
                .append(Component.text("bénédictions", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" permanentes.\n\nClique sur un PNJ pour voir ce qu'il veut."))
                .build());

        // ── Page 3 : Le Survivant ─────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("Le Survivant\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Commence par lui.\n\n"))
                .append(Component.text("Outils et armures sont "))
                .append(Component.text("verrouillés", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" au départ.\nIl les débloque palier par palier.\n\n"))
                .append(Component.text("Bois→Pierre→Fer\n→Or→Diamant", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .build());

        // ── Page 4 : Les autres PNJ ───────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("Les autres PNJ\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text("Thomas Pesquet\n", NamedTextColor.BLUE, TextDecoration.BOLD))
                .append(Component.text("Débloque le Nether, l'End…\n\n"))
                .append(Component.text("Maddox\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Augmente ta force.\n\n"))
                .append(Component.text("Nain Roux\n", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("Augmente ta résistance.\n\n"))
                .build());

        // ── Page 5 : Métiers ──────────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("II. Métiers\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Tu as 12 métiers en même temps.\nChacun a son propre niveau.\n\n"))
                .append(Component.text("Monte un métier via son "))
                .append(Component.text("villageois").decorate(TextDecoration.BOLD))
                .append(Component.text(" dans le village.\n\nPlus le niveau est haut, plus les effets sont puissants."))
                .build());

        // ── Page 6 : Quêtes ───────────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("III. Quêtes\n\n", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.text("2 quêtes par jour", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(", renouvelées chaque jour.\n\n"))
                .append(Component.text("Récompenses : des effets et de l'xp de métier."))
                .build());

        // ── Page 7 : Crafts custom ────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("IV. Crafts custom\n\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Armures fer, or, diamant\nvanilla : "))
                .append(Component.text("DÉSACTIVÉES", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(".\n\nChaque pièce se craft en entourant l'armure précédente du matériau suivant.\n\n"))
                .append(Component.text("Cuir→Mailles(lave)\n→Fer→Or→Diamant", NamedTextColor.DARK_GRAY))
                .build());

        // ── Page 8 : Pour démarrer ────────────────────────────────────────────
        pages.add(Component.text()
                .append(Component.text("Pour démarrer\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("1. Parle au "))
                .append(Component.text("Survivant").decorate(TextDecoration.BOLD))
                .append(Component.text(".\n\n"))
                .append(Component.text("2. Fais tes "))
                .append(Component.text("2 quêtes").decorate(TextDecoration.BOLD))
                .append(Component.text(" chaque jour.\n\n"))
                .append(Component.text("3. Explore le village.\n\n"))
                .append(Component.text("Bonne aventure !", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build());

        return pages;
    }
}