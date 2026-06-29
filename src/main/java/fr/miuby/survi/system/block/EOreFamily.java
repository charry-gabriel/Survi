package fr.miuby.survi.system.block;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Regroupe les variants d'un même minerai : forme overworld, forme deepslate, et item droppé normalement.
 *
 * <p>Les minerais nether-only ({@code NETHER_GOLD_ORE}, {@code NETHER_QUARTZ_ORE},
 * {@code ANCIENT_DEBRIS}) n'ont pas de variante deepslate et forment chacun
 * leur propre famille — distincte de leur équivalent overworld le cas échéant
 * (ex. {@code NETHER_GOLD} ≠ {@code GOLD}).</p>
 *
 * <p>{@link #drop} est l'item obtenu en cassant le minerai sans Silk Touch.
 * Pour {@code DEBRIS}, le bloc drop lui-même ({@code ANCIENT_DEBRIS}) puisque
 * ce minerai retourne toujours son propre bloc, Silk Touch ou non.</p>
 *
 * <p>Toutes les constantes de {@link MaterialUtils} liées aux minerais sont dérivées de cet enum.</p>
 */
public enum EOreFamily {
    COAL        (Material.COAL_ORE,          Material.DEEPSLATE_COAL_ORE,     Material.COAL),
    IRON        (Material.IRON_ORE,          Material.DEEPSLATE_IRON_ORE,     Material.RAW_IRON),
    GOLD        (Material.GOLD_ORE,          Material.DEEPSLATE_GOLD_ORE,     Material.RAW_GOLD),
    DIAMOND     (Material.DIAMOND_ORE,       Material.DEEPSLATE_DIAMOND_ORE,  Material.DIAMOND),
    EMERALD     (Material.EMERALD_ORE,       Material.DEEPSLATE_EMERALD_ORE,  Material.EMERALD),
    LAPIS       (Material.LAPIS_ORE,         Material.DEEPSLATE_LAPIS_ORE,    Material.LAPIS_LAZULI),
    REDSTONE    (Material.REDSTONE_ORE,      Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE),
    COPPER      (Material.COPPER_ORE,        Material.DEEPSLATE_COPPER_ORE,   Material.RAW_COPPER),
    // ─── Nether-only (pas de variant deepslate, famille indépendante) ───────────
    NETHER_GOLD (Material.NETHER_GOLD_ORE,   null,                            Material.GOLD_NUGGET),
    QUARTZ      (Material.NETHER_QUARTZ_ORE, null,                            Material.QUARTZ),
    DEBRIS      (Material.ANCIENT_DEBRIS,    null,                            Material.ANCIENT_DEBRIS);

    /** Forme principale (overworld pour les minerais normaux, nether pour les nether-only). */
    public final Material ore;
    /** Variant deepslate ; {@code null} si le minerai n'en a pas. */
    @Nullable public final Material deepslate;

    public final Material drop;

    EOreFamily(Material ore, @Nullable Material deepslate, Material drop) {
        this.ore = ore;
        this.deepslate = deepslate;
        this.drop = drop;
    }
}