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

    private static final String BOOK_TITLE  = "Guide de Survie";
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
                .append(Component.text("[Survie] ", NamedTextColor.GOLD, TextDecoration.BOLD))
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

        pages.add(Component.text()
                .append(Component.text("Bienvenue !\n\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Ce serveur modifie beaucoup de mécaniques. Lis ce guide.\n\n"))
                .append(Component.text("Sommaire\n", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .append(Component.text("I.   Villageois\n"))
                .append(Component.text("II.  Métiers\n"))
                .append(Component.text("III. Quêtes\n"))
                .append(Component.text("IV.  Crafts custom"))
                .build());

        pages.add(Component.text()
                .append(Component.text("I. Villageois\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("C'est la base du jeu.\n\n"))
                .append(Component.text("Apporte des "))
                .append(Component.text("tributs").decorate(TextDecoration.BOLD))
                .append(Component.text(" (items) en échange de "))
                .append(Component.text("bénédictions", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" permanentes pour tous les joueurs."))
                .build());

        pages.add(Component.text()
                .append(Component.text("II. Métiers\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Tu as 6 métiers, à un niveau trop bas.\n\nEt donc beaucoup de "))
                .append(Component.text("malus", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(".\n\nMonte un métier via son "))
                .append(Component.text("Marchand", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" dans le village.\n\n"))
                .build());

        pages.add(Component.text()
                .append(Component.text("III. Quêtes\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Tu as droit à 2 quêtes par jour.\n"))
                .append(Component.text("(Ou plus si on est gentil)\n\n", NamedTextColor.GRAY))
                .append(Component.text("Elles te font monter le niveau de tes métiers et peuvent te donner des bonus."))
                .build());

        pages.add(Component.text()
                .append(Component.text("IV. Crafts custom\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Armures vanilla : "))
                .append(Component.text("DÉSACTIVÉES", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(".\n\nChaque pièce se craft en entourant l'armure précédente du matériau suivant.\n\n"))
                .append(Component.text("Tu peux voir les recettes dans la table de craft."))
                .build());

        pages.add(Component.text()
                .append(Component.text("Pour démarrer\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("1. Découvre tous les villageois.\n\n"))
                .append(Component.text("2. Fais tes "))
                .append(Component.text("2 quêtes").decorate(TextDecoration.BOLD))
                .append(Component.text(" chaque jour.\n\n"))
                .append(Component.text("3. Construis dans "))
                .append(Component.text("Village", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" et récolte dans "))
                .append(Component.text("Wilderness", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(".\n\nBonne aventure !"))
                .build());

        return pages;
    }
}