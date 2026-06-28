package fr.miuby.survi.system.block;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Regroupe les variants d'un même minerai : forme overworld, forme deepslate.
 *
 * <p>Les minerais nether-only ({@code NETHER_GOLD_ORE}, {@code NETHER_QUARTZ_ORE},
 * {@code ANCIENT_DEBRIS}) n'ont pas de variante deepslate et forment chacun
 * leur propre famille — distincte de leur équivalent overworld le cas échéant
 * (ex. {@code NETHER_GOLD} ≠ {@code GOLD}).</p>
 *
 * <p>Toutes les constantes de {@link MaterialUtils} liées aux minerais sont dérivées de cet enum.</p>
 */
public enum EOreFamily {
    COAL        (Material.COAL_ORE,          Material.DEEPSLATE_COAL_ORE),
    IRON        (Material.IRON_ORE,          Material.DEEPSLATE_IRON_ORE),
    GOLD        (Material.GOLD_ORE,          Material.DEEPSLATE_GOLD_ORE),
    DIAMOND     (Material.DIAMOND_ORE,       Material.DEEPSLATE_DIAMOND_ORE),
    EMERALD     (Material.EMERALD_ORE,       Material.DEEPSLATE_EMERALD_ORE),
    LAPIS       (Material.LAPIS_ORE,         Material.DEEPSLATE_LAPIS_ORE),
    REDSTONE    (Material.REDSTONE_ORE,      Material.DEEPSLATE_REDSTONE_ORE),
    COPPER      (Material.COPPER_ORE,        Material.DEEPSLATE_COPPER_ORE),
    // ─── Nether-only (pas de variant deepslate, famille indépendante) ───────────
    NETHER_GOLD (Material.NETHER_GOLD_ORE,   null),
    QUARTZ      (Material.NETHER_QUARTZ_ORE, null),
    DEBRIS      (Material.ANCIENT_DEBRIS,    null);

    /** Forme principale (overworld pour les minerais normaux, nether pour les nether-only). */
    public final Material ore;
    /** Variant deepslate ; {@code null} si le minerai n'en a pas. */
    @Nullable public final Material deepslate;

    EOreFamily(Material ore, @Nullable Material deepslate) {
        this.ore = ore;
        this.deepslate = deepslate;
    }
}