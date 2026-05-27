package fr.miuby.survi.villager.villagerlevel.blessing;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.item.locked_item.ELockedArmorType;
import fr.miuby.survi.item.locked_item.ELockedToolType;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.WorldArgument;
import fr.miuby.survi.world.EWorld;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

/**
 * Commande admin : /blessing <joueur> <effet> [paramètres...]
 *
 * Effets disponibles :
 *   damage <float>           — modificateur de dégâts (ex: 0.5 = 50%)
 *   resistance <float>       — modificateur de résistance
 *   max_health <int>         — bonus de vie max (peut être négatif)
 *   gamemode <mode>          — survival | creative | adventure | spectator
 *   lock_world <monde>       — déverrouille un monde : WILDERNESS | NETHER | END
 *   unlock_armor <type>      — LEATHER | CHAINMAIL | IRON | GOLD | DIAMOND | NETHERITE
 *   unlock_tool <type>       — WOOD | STONE | IRON | GOLD | DIAMOND
 *   fly                      — active le vol pour le joueur
 *   regen                    — active la régénération naturelle en Wilderness
 *   dispel                   — déclenche l'effet Dispel
 *   world_reset <worldName>  — reset un monde Multiverse (WILDERNESS | NETHER | END)
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class BlessingCommand {
    private static final String playerArgument = "player";
    private static final String valueArgument = "value";

    private BlessingCommand() {
        /* This utility class should not be instantiated */
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("blessing")
                .requires(src -> src.getSender().hasPermission("survi.admin"))
                .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())

                        // ── damage <float> ──────────────────────────────
                        .then(Commands.literal("damage")
                                .then(Commands.argument(valueArgument, FloatArgumentType.floatArg(0.01f, 10f))
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            float value = FloatArgumentType.getFloat(ctx, valueArgument);
                                            new DamageEffect(value).applyEffect(null, target);
                                            feedback(ctx, target, "Damage modifier → " + value);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── resistance <float> ──────────────────────────
                        .then(Commands.literal("resistance")
                                .then(Commands.argument(valueArgument, FloatArgumentType.floatArg(0.01f, 10f))
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            float value = FloatArgumentType.getFloat(ctx, valueArgument);
                                            new ResistanceEffect(value).applyEffect(null, target);
                                            feedback(ctx, target, "Resistance modifier → " + value);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── max_health <int> ────────────────────────────
                        .then(Commands.literal("max_health")
                                .then(Commands.argument(valueArgument, IntegerArgumentType.integer(-20, 100))
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            int value = IntegerArgumentType.getInteger(ctx, valueArgument);
                                            new MaxHealthEffect(value).applyEffect(null, target);
                                            feedback(ctx, target, "Max health bonus → " + value);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── lock_world <eworld> ─────────────────────────
                        // (déverrouille le monde malgré le nom "lock" — c'est l'effet existant)
                        .then(Commands.literal("lock_world")
                                .then(Commands.argument("world", WorldArgument.world())
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            MLWorld world = WorldArgument.getWorld(ctx, "world");
                                            new LockWorldEffect((EWorld) world.getType()).applyEffect(null, target);
                                            feedback(ctx, target, "Monde déverrouillé → " + world.getName());
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── unlock_armor <type> ─────────────────────────
                        .then(Commands.literal("unlock_armor")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String rem = builder.getRemaining().toLowerCase();
                                            Arrays.stream(ELockedArmorType.values())
                                                    .map(Enum::name)
                                                    .filter(s -> s.toLowerCase().startsWith(rem))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            String typeStr = StringArgumentType.getString(ctx, "type").toUpperCase();
                                            ELockedArmorType armorType;
                                            try {
                                                armorType = ELockedArmorType.valueOf(typeStr);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().getSender().sendMessage(
                                                        Component.text("Type invalide : " + typeStr, NamedTextColor.RED));
                                                return 0;
                                            }
                                            new UnlockArmorEffect(armorType).applyEffect(null, target);
                                            feedback(ctx, target, "Armure déverrouillée → " + armorType.name());
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── unlock_tool <type> ──────────────────────────
                        .then(Commands.literal("unlock_tool")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String rem = builder.getRemaining().toLowerCase();
                                            Arrays.stream(ELockedToolType.values())
                                                    .map(Enum::name)
                                                    .filter(s -> s.toLowerCase().startsWith(rem))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                            String typeStr = StringArgumentType.getString(ctx, "type").toUpperCase();
                                            ELockedToolType toolType;
                                            try {
                                                toolType = ELockedToolType.valueOf(typeStr);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().getSender().sendMessage(
                                                        Component.text("Type invalide : " + typeStr, NamedTextColor.RED));
                                                return 0;
                                            }
                                            new UnlockToolEffect(toolType).applyEffect(null, target);
                                            feedback(ctx, target, "Outil déverrouillé → " + toolType.name());
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )

                        // ── fly (pas de paramètre) ──────────────────────
                        .then(Commands.literal("fly")
                                .executes(ctx -> {
                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                    new FlyEffect().applyEffect(null, target);
                                    feedback(ctx, target, "Vol activé");
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // ── regen (pas de paramètre) ────────────────────
                        .then(Commands.literal("regen")
                                .executes(ctx -> {
                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                    new RegenEffect().applyEffect(null, target);
                                    feedback(ctx, target, "Régénération naturelle activée en Wilderness");
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // ── dispel (pas de paramètre) ───────────────────
                        .then(Commands.literal("dispel")
                                .executes(ctx -> {
                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
                                    new DispelEffect(10).applyEffect(null, target);
                                    feedback(ctx, target, "Dispel appliqué");
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        // ── world_reset <monde> ─────────────────────────
                        .then(Commands.literal("world_reset")
                                .executes(ctx -> {
                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);

                                    new WorldResetEffect(1).applyEffect(null, target);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private static void feedback(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, AlphaPlayer target, String effectDesc) {
        ctx.getSource().getSender().sendMessage(
                Component.text("[Blessing] ", NamedTextColor.YELLOW)
                        .append(Component.text(effectDesc, NamedTextColor.YELLOW))
                        .append(Component.text(" → appliqué à ", NamedTextColor.YELLOW))
                        .append(Component.text(target.getPlayer() != null ? target.getPlayer().getName() : target.getUuid().toString(), NamedTextColor.YELLOW))
        );
    }
}