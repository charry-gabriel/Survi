package fr.miuby.survi.mob;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.survi.GameManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Objects;

/**
 * Commandes de test et de debug pour le système de niveaux de monstres.
 *
 * <h3>Commandes disponibles (toutes OP)</h3>
 * <pre>
 *  /mob spawn <type> [level]        — Spawn un mob scalé devant soi
 *  /mob info                        — Affiche les infos du mob que tu regardes
 *  /mob reload                      — Recharge monsters.yml sans redémarrer
 *  /mob stats <type> <level>        — Affiche les stats calculées pour un type/niveau
 *  /mob testtier <tier> <type>      — Simule un palier du monde
 * </pre>
 *
 * <p>L'auto-complétion du paramètre {@code <type>} ne propose que les mobs
 * configurés dans {@code monsters.yml}, pas tous les {@link EntityType}.</p>
 *
 * <h3>Enregistrement dans Survi.java</h3>
 * <pre>
 *   commands.registrar().register(MobCommand.createCommand().build());
 * </pre>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class MobCommand {

    private static final String ARG_TYPE  = "type";
    private static final String ARG_LEVEL = "level";

    private MobCommand() {}

    // ─── Point d'entrée ───────────────────────────────────────────────────────────

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("mob")
                .requires(source -> source.getSender().isOp())
                .then(buildSpawnSubCommand())
                .then(buildInfoSubCommand())
                .then(buildStatsSubCommand())
                .then(buildTestTierSubCommand());
    }

    // ─── Sous-commandes ───────────────────────────────────────────────────────────

    /**
     * {@code /mob spawn <type> [level]}
     * Spawne un mob scalé à 3 blocs devant le joueur.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildSpawnSubCommand() {
        return Commands.literal("spawn")
                .then(Commands.argument(ARG_TYPE, StringArgumentType.word())
                        .suggests((_, builder) -> {
                            suggestConfiguredMobs(builder);
                            return builder.buildFuture();
                        })

                        // /mob spawn <type>  → niveau aléatoire du palier courant
                        .executes(ctx -> {
                            EntityType type = parseType(ctx.getSource().getSender(),
                                    StringArgumentType.getString(ctx, ARG_TYPE));
                            if (type == null) return Command.SINGLE_SUCCESS;
                            int level = GameManager.getInstance().getMobLevelManager().rollMobLevel();
                            return spawnMob(ctx.getSource(), type, level);
                        })

                        // /mob spawn <type> <level>  → niveau précis
                        .then(Commands.argument(ARG_LEVEL, IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    EntityType type = parseType(ctx.getSource().getSender(),
                                            StringArgumentType.getString(ctx, ARG_TYPE));
                                    if (type == null) return Command.SINGLE_SUCCESS;
                                    int level = IntegerArgumentType.getInteger(ctx, ARG_LEVEL);
                                    return spawnMob(ctx.getSource(), type, level);
                                })
                        )
                );
    }

    /**
     * {@code /mob info}
     * Affiche les infos du mob vivant le plus proche dans la ligne de vue (10 blocs).
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildInfoSubCommand() {
        return Commands.literal("info")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Commande réservée aux joueurs.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    LivingEntity target = getTargetMob(player, 10);
                    if (target == null) {
                        player.sendMessage(Component.text(
                                "Aucun monstre dans ta ligne de vue (10 blocs).", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    sendMobInfo(player, target);
                    return Command.SINGLE_SUCCESS;
                });
    }

    /**
     * {@code /mob stats <type> <level>}
     * Affiche les stats théoriques d'un type/niveau donné sans spawner de mob.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildStatsSubCommand() {
        return Commands.literal("stats")
                .then(Commands.argument(ARG_TYPE, StringArgumentType.word())
                        .suggests((_, builder) -> {
                            suggestConfiguredMobs(builder);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument(ARG_LEVEL, IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    EntityType type = parseType(sender,
                                            StringArgumentType.getString(ctx, ARG_TYPE));
                                    if (type == null) return Command.SINGLE_SUCCESS;
                                    int level = IntegerArgumentType.getInteger(ctx, ARG_LEVEL);
                                    sendMobStats(sender, type, level);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    /**
     * {@code /mob testtier <tier> <type>}
     * Spawne un mob au niveau minimal du palier demandé, sans modifier la progression réelle.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTestTierSubCommand() {
        return Commands.literal("testtier")
                .then(Commands.argument("tier", IntegerArgumentType.integer(1))
                        .then(Commands.argument(ARG_TYPE, StringArgumentType.word())
                                .suggests((_, builder) -> {
                                    suggestConfiguredMobs(builder);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    int tier  = IntegerArgumentType.getInteger(ctx, "tier");
                                    int lpt   = GameManager.getInstance().getMobLevelManager().getLevelsPerTier();
                                    int level = (tier - 1) * lpt + 1;

                                    EntityType type = parseType(sender,
                                            StringArgumentType.getString(ctx, ARG_TYPE));
                                    if (type == null) return Command.SINGLE_SUCCESS;

                                    sender.sendMessage(Component.text(
                                            "Spawn " + type.name() + " niveau " + level
                                                    + " (palier " + tier + ")",
                                            NamedTextColor.YELLOW));
                                    return spawnMob(ctx.getSource(), type, level);
                                })
                        )
                );
    }

    // ─── Logique métier ───────────────────────────────────────────────────────────

    private static int spawnMob(CommandSourceStack source, EntityType type, int level) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande nécessite d'être en jeu.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Location loc = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(3));

        Entity entity = player.getWorld().spawnEntity(loc, type);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            sender.sendMessage(Component.text(type.name() + " n'est pas un mob vivant.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        GameManager.getInstance().getMobLevelManager().applyLevel(living, level);

        sender.sendMessage(Component.text()
                .append(Component.text("Spawné : ", NamedTextColor.GRAY))
                .append(Component.text(type.name(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" niveau ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(level), NamedTextColor.GOLD, TextDecoration.BOLD))
                .build());

        return Command.SINGLE_SUCCESS;
    }

    private static void sendMobInfo(Player player, LivingEntity target) {
        int storedLevel = GameManager.getInstance().getMobLevelManager().getStoredLevel(target);
        MobTypeConfig cfg = GameManager.getInstance().getMobLevelManager().getConfig(target.getType());

        player.sendMessage(header("MOB INFO"));
        player.sendMessage(row("Type",   target.getType().name()));
        player.sendMessage(row("Niveau", storedLevel < 0 ? "non scalé" : String.valueOf(storedLevel)));
        player.sendMessage(row("PV actuels", String.format("%.1f / %.1f",
                target.getHealth(),
                target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                        ? Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue()
                        : 0)));

        if (cfg != null && storedLevel > 0) {
            for (EMobStat stat : EMobStat.values()) {
                double v = cfg.getStatValue(stat, storedLevel);
                if (v >= 0)
                    player.sendMessage(row(stat.getConfigKey(), String.format("%.3f", v)));
            }
            if (cfg.getExplosionRadius() != null)
                player.sendMessage(row("explosion-radius",
                        String.format("%.1f", cfg.getExplosionRadius().compute(storedLevel))));
            if (cfg.getFuseTicks() != null)
                player.sendMessage(row("fuse-ticks",
                        String.format("%.1f", cfg.getFuseTicks().compute(storedLevel))));
            if (!cfg.getPotionEffects().isEmpty())
                player.sendMessage(row("effets de potion", cfg.getPotionEffects().size() + " configuré(s)"));
        }

        player.sendMessage(footer());
    }

    private static void sendMobStats(CommandSender sender, EntityType type, int level) {
        MobTypeConfig cfg = GameManager.getInstance().getMobLevelManager().getConfig(type);
        if (cfg == null) {
            sender.sendMessage(Component.text(
                    type.name() + " n'est pas configuré dans monsters.yml.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(header("STATS  " + type.name() + "  niv." + level));
        for (EMobStat stat : EMobStat.values()) {
            double v = cfg.getStatValue(stat, level);
            if (v >= 0)
                sender.sendMessage(row(stat.getConfigKey(), String.format("%.3f", v)));
        }
        if (cfg.getExplosionRadius() != null)
            sender.sendMessage(row("explosion-radius",
                    String.format("%.1f", cfg.getExplosionRadius().compute(level))));
        if (cfg.getFuseTicks() != null)
            sender.sendMessage(row("fuse-ticks",
                    String.format("%.1f ticks (%.2f s)",
                            cfg.getFuseTicks().compute(level),
                            cfg.getFuseTicks().compute(level) / 20.0)));
        for (MobPotionEffectConfig pec : cfg.getPotionEffects()) {
            if (level >= pec.minMobLevel()) {
                sender.sendMessage(row(
                        "potion/" + pec.type().getName(),
                        String.format("amp.%d  %d ticks  %.0f%%",
                                pec.computeAmplifier(level),
                                pec.computeDuration(level),
                                pec.computeChance(level) * 100)));
            }
        }
        sender.sendMessage(footer());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Remplit le builder d'auto-complétion avec les mobs configurés dans
     * {@code monsters.yml} uniquement — pas tous les {@link EntityType}.
     */
    private static void suggestConfiguredMobs(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (EntityType et : GameManager.getInstance().getMobLevelManager().getConfiguredTypes()) {
            builder.suggest(et.name().toLowerCase());
        }
    }

    /** Retourne le premier mob vivant proche du joueur dans {@code range} blocs. */
    private static LivingEntity getTargetMob(Player player, int range) {
        return player.getWorld().getNearbyEntities(player.getLocation(), range, range, range).stream()
                .filter(e -> e instanceof LivingEntity && e != player)
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(a -> a.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private static EntityType parseType(CommandSender sender, String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException _) {
            sender.sendMessage(Component.text("Type de mob inconnu : " + name, NamedTextColor.RED));
            return null;
        }
    }

    // ─── Formatage UI ─────────────────────────────────────────────────────────────

    private static Component header(String title) {
        return Component.text("━━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ━━━", NamedTextColor.DARK_GRAY));
    }

    private static Component footer() {
        return Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY);
    }

    private static Component row(String key, String value) {
        return Component.text("  " + key + " : ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }
}