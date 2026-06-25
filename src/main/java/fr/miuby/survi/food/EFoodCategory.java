package fr.miuby.survi.food;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Catégories de nourriture pour le tirage quotidien de la "nourriture du jour"
 * (voir {@link FoodOfTheDayManager}). Chaque catégorie possède un pool de {@link Material}
 * parmi lesquels un seul est tiré au sort à chaque reset journalier (6h).
 */
public enum EFoodCategory {

    COOKED_MEAT(NamedTextColor.RED, List.of(
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD, Material.COOKED_SALMON
    )),
    PLANT(NamedTextColor.GREEN, List.of(
            Material.APPLE, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.DRIED_KELP,
            Material.MELON_SLICE, Material.SWEET_BERRIES, Material.GLOW_BERRIES, Material.CHORUS_FRUIT
    )),
    CRAFTED(NamedTextColor.GOLD, List.of(
            Material.BREAD, Material.COOKIE, Material.PUMPKIN_PIE, Material.BAKED_POTATO,
            Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.BEETROOT_SOUP, Material.GOLDEN_CARROT
    ));

    /** Noms français affichés dans le tab — fallback sur {@code Material.name()} si absent. */
    private static final Map<Material, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry(Material.COOKED_BEEF, "Bœuf cuit"),
            Map.entry(Material.COOKED_PORKCHOP, "Porc cuit"),
            Map.entry(Material.COOKED_CHICKEN, "Poulet cuit"),
            Map.entry(Material.COOKED_MUTTON, "Mouton cuit"),
            Map.entry(Material.COOKED_RABBIT, "Lapin cuit"),
            Map.entry(Material.COOKED_COD, "Morue cuite"),
            Map.entry(Material.COOKED_SALMON, "Saumon cuit"),
            Map.entry(Material.APPLE, "Pomme"),
            Map.entry(Material.CARROT, "Carotte"),
            Map.entry(Material.POTATO, "Pomme de terre"),
            Map.entry(Material.BAKED_POTATO, "Pomme de terre cuite"),
            Map.entry(Material.BEETROOT, "Betterave"),
            Map.entry(Material.MELON_SLICE, "Tranche de melon"),
            Map.entry(Material.SWEET_BERRIES, "Baies sucrées"),
            Map.entry(Material.GLOW_BERRIES, "Baies lumineuses"),
            Map.entry(Material.CHORUS_FRUIT, "Fruit chorus"),
            Map.entry(Material.DRIED_KELP, "Algue séchée"),
            Map.entry(Material.BREAD, "Pain"),
            Map.entry(Material.COOKIE, "Cookie"),
            Map.entry(Material.PUMPKIN_PIE, "Tarte à la citrouille"),
            Map.entry(Material.MUSHROOM_STEW, "Soupe aux champignons"),
            Map.entry(Material.RABBIT_STEW, "Ragoût de lapin"),
            Map.entry(Material.BEETROOT_SOUP, "Soupe de betterave"),
            Map.entry(Material.GOLDEN_CARROT, "Carotte dorée")
    );

    private final NamedTextColor color;
    private final List<Material> pool;

    EFoodCategory(NamedTextColor color, List<Material> pool) {
        this.color = color;
        this.pool = pool;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public List<Material> getPool() {
        return pool;
    }

    public Material drawRandom() {
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    public static String getDisplayName(Material material) {
        return DISPLAY_NAMES.getOrDefault(material, material.name());
    }
}