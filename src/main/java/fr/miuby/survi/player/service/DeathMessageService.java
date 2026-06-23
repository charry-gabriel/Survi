package fr.miuby.survi.player.service;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Random;

/**
 * Construit un message de mort rigolo pour {@link PlayerDeathEvent}.
 *
 * <p>Logique de sélection :</p>
 * <ol>
 *   <li>Si {@code lastJobDamageCause == FISHERMAN} et que la dernière cause est {@code CUSTOM}
 *       → message pêcheur (pression aquatique).</li>
 *   <li>Sinon, switch sur {@link EntityDamageEvent.DamageCause} du dernier dégât.</li>
 *   <li>Pour {@code FALL} → message explorateur incluant le niveau actuel.</li>
 *   <li>Pour les kills par entité → message avec le nom de l'attaquant.</li>
 * </ol>
 *
 * <p>Appelé depuis {@link fr.miuby.survi.listener.DamageListener#onPlayerDeath}.</p>
 */
public final class DeathMessageService {

    private DeathMessageService() {}

    private static final Random RANDOM = new Random();

    // ─── Pools de messages ───────────────────────────────────────────────────────
    // Convention : %s = nom du joueur (PvP/PvE distincts — voir buildEntityKillMessage)

    private static final String[] FALL_MESSAGES = {
            "%s s'est foulé la cheville et en est mort.",
            "%s a raté son atterrissage. Définitivement.",
            "%s a fait la démonstration qu'un explorateur peut encore mourir d'une chute stupide.",
            "Le sol et %s se sont rencontrés violemment. Son niveau d'explorateur n'a pas vraiment aidé.",
            "%s voulait explorer le sol de plus près. Mission accomplie."
    };

    private static final String[] FISHERMAN_MESSAGES = {
            "%s est mort noyé dans une flaque.",
            "%s a voulu devenir un poisson à la place d'un pêcheur",
            "%s a mieux réussi à couler qu'à pêcher.",
            "La pression aquatique a eu raison de %s. Un pêcheur qui avait encore beaucoup à apprendre."
    };

    private static final String[] DROWNING_MESSAGES = {
            "%s a passé trop de temps la tête sous l'eau. C'était facultatif.",
            "%s voulait voir la vie aquatique de l'intérieur. Elle n'en a pas voulu.",
            "%s s'est souvenu trop tard qu'on ne respire pas sous l'eau."
    };

    private static final String[] STARVATION_MESSAGES = {
            "%s est mort d'avoir oublié l'heure du repas. Un classique.",
            "%s a refusé de manger. La mort n'a pas refusé.",
            "%s avait faim mais pas la motivation de manger. Ce n'est plus un problème."
    };

    private static final String[] VOID_MESSAGES = {
            "%s a sauté dans le vide pour voir ce qu'il y avait en dessous. La réponse : rien.",
            "%s a trouvé la sortie du monde. Elle est sans retour.",
            "%s a voulu toucher le fond. Il y est parvenu."
    };

    private static final String[] FIRE_MESSAGES = {
            "%s a voulu vérifier si le feu brûle vraiment. Verdict : oui.",
            "%s a pris feu et n'a pas pu s'éteindre.",
            "%s s'était convaincu que les flammes, c'était pas si grave. Il avait tort."
    };

    private static final String[] LAVA_MESSAGES = {
            "%s a pris un bain de lave. Le spa était définitif.",
            "%s a nagé dans la lave. Brièvement.",
            "%s a trouvé que l'eau était trop froide et a cherché une alternative. C'était un peu trop chaud."
    };

    private static final String[] LIGHTNING_MESSAGES = {
            "%s a attiré la foudre. Littéralement.",
            "%s s'est trouvé au mauvais endroit lors d'un orage. Très mauvais endroit.",
            "%s a décidé de tester la conductivité de son équipement par temps d'orage."
    };

    private static final String[] CONTACT_MESSAGES = {
            "%s a câliné un cactus. Le cactus n'a pas rendu le câlin.",
            "%s pensait que les cactus n'étaient que décoratifs.",
            "%s et un cactus se sont rencontrés. Seul le cactus s'en est sorti."
    };

    private static final String[] SUFFOCATION_MESSAGES = {
            "%s s'est retrouvé dans un espace trop étroit pour sa survie.",
            "%s a décidé de s'incruster dans un mur. Le mur a refusé.",
            "%s a exploré l'intérieur d'un bloc. C'était une mauvaise idée."
    };

    private static final String[] FREEZE_MESSAGES = {
            "%s a décidé de s'allonger dans de la poudre de neige. Pour de bon.",
            "%s a trouvé que la neige était douce. Trop douce.",
            "%s a testé sa résistance au froid. Résultat : insuffisant."
    };

    private static final String[] SONIC_BOOM_MESSAGES = {
            "%s a reçu un choc sonique de la vigile. Il a vibré un peu trop fort.",
            "%s a réveillé quelque chose qu'il n'aurait pas dû. La vigile a répondu.",
            "%s a découvert que le silence n'est pas la seule arme des vigiles."
    };

    private static final String[] HOT_FLOOR_MESSAGES = {
            "%s a marché sur de la roche en fusion. Il n'avait pas les bonnes bottes.",
            "%s a testé la danse sur magma. C'était sa dernière.",
            "%s pensait que ça ne brûlerait pas à travers ses chaussures. Il avait tort."
    };

    private static final String[] EXPLOSION_MESSAGES = {
            "%s a été soufflé par une explosion. Ça ne pardonne pas.",
            "%s s'est retrouvé trop proche d'une explosion. Beaucoup trop proche.",
            "%s a découvert que la poudre à canon est très réactive."
    };

    private static final String[] MAGIC_MESSAGES = {
            "%s a reçu un sort malveillant. La magie jouait contre lui.",
            "%s a été empoisonné. Il aurait dû lire l'étiquette.",
            "%s a sous-estimé le danger de la magie noire."
    };

    private static final String[] WITHER_MESSAGES = {
            "%s a été rongé par le Flétrissement. Ça prend du temps, mais c'est efficace.",
            "%s a laissé le Flétrissement faire son travail. Il l'a fait.",
            "%s n'a pas trouvé de lait à temps. Dommage."
    };

    private static final String[] GENERIC_MESSAGES = {
            "%s a trouvé une façon originale de quitter ce monde.",
            "%s a succombé à ses blessures.",
            "Et c'est ainsi que %s rendit l'âme."
    };

    // ─── Templates PvP (deux noms String : joueur vs joueur) ─────────────────────
    // %1$s = victime, %2$s = tueur

    private static final String[] PVP_MESSAGES = {
            "%1$s a rencontré %2$s. %2$s avait une épée.",
            "%1$s s'est battu contre %2$s. %2$s a gagné.",
            "%1$s a défié %2$s en duel. %2$s a accepté malheureusement."
    };

    // ─── Templates PvE (mob name = Component) ────────────────────────────────────
    // Chaque entrée : [texte avant le mob, texte après le mob]
    // %s dans la partie "avant" = nom du joueur victime

    private record MobTemplate(String before, String after) {}

    private static final MobTemplate[] MOB_TEMPLATES = {
            new MobTemplate("%s s'est courageusement battu contre ", ". Le courage n'était pas suffisant."),
            new MobTemplate("%s a tenu tête à ", ". Pas assez longtemps."),
            new MobTemplate("%s a voulu affronter ", ". La rencontre fut brève.")
    };

    // ─── Point d'entrée ──────────────────────────────────────────────────────────

    public static Component build(PlayerDeathEvent event, AlphaPlayer alpha) {
        String name = event.getPlayer().getName();
        EntityDamageEvent lastDamage = event.getPlayer().getLastDamageCause();
        EJob jobCause = alpha.getLastJobDamageCause();

        if (jobCause == EJob.FISHERMAN
                && lastDamage != null
                && lastDamage.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            return format(pick(FISHERMAN_MESSAGES), name);
        }

        if (lastDamage == null) return format(pick(GENERIC_MESSAGES), name);

        return switch (lastDamage.getCause()) {
            case FALL                                          -> format(pick(FALL_MESSAGES),        name);
            case DROWNING                                      -> format(pick(DROWNING_MESSAGES),    name);
            case STARVATION                                    -> format(pick(STARVATION_MESSAGES),  name);
            case VOID                                          -> format(pick(VOID_MESSAGES),        name);
            case FIRE, FIRE_TICK                               -> format(pick(FIRE_MESSAGES),        name);
            case LAVA                                          -> format(pick(LAVA_MESSAGES),        name);
            case LIGHTNING                                     -> format(pick(LIGHTNING_MESSAGES),   name);
            case CONTACT                                       -> format(pick(CONTACT_MESSAGES),     name);
            case SUFFOCATION, FLY_INTO_WALL, CRAMMING          -> format(pick(SUFFOCATION_MESSAGES), name);
            case FREEZE                                        -> format(pick(FREEZE_MESSAGES),      name);
            case SONIC_BOOM                                    -> format(pick(SONIC_BOOM_MESSAGES),  name);
            case HOT_FLOOR                                     -> format(pick(HOT_FLOOR_MESSAGES),   name);
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION             -> format(pick(EXPLOSION_MESSAGES),   name);
            case MAGIC, POISON                                 -> format(pick(MAGIC_MESSAGES),       name);
            case WITHER                                        -> format(pick(WITHER_MESSAGES),      name);
            case ENTITY_ATTACK, PROJECTILE, ENTITY_SWEEP_ATTACK -> buildEntityKillMessage(lastDamage, name);
            default                                            -> format(pick(GENERIC_MESSAGES),     name);
        };
    }

    // ─── Kill par entité ─────────────────────────────────────────────────────────

    private static Component buildEntityKillMessage(EntityDamageEvent lastDamage, String playerName) {
        if (!(lastDamage instanceof EntityDamageByEntityEvent byEntity))
            return format(pick(GENERIC_MESSAGES), playerName);

        Entity damager = byEntity.getDamager();

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity shooter) damager = shooter;
        }

        if (damager instanceof Player killer)
            return format(pick(PVP_MESSAGES), playerName, killer.getName());

        if (!(damager instanceof LivingEntity living))
            return format(pick(GENERIC_MESSAGES), playerName);

        MobTemplate t = MOB_TEMPLATES[RANDOM.nextInt(MOB_TEMPLATES.length)];
        return Component.text(String.format(t.before(), playerName), NamedTextColor.GRAY)
                .append(GameManager.getInstance().getMobLevelManager().buildDeathName(living))
                .append(Component.text(t.after(), NamedTextColor.GRAY));
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────────

    private static String pick(String[] pool) {
        return pool[RANDOM.nextInt(pool.length)];
    }

    private static Component format(String template, Object... args) {
        return Component.text(String.format(template, args), NamedTextColor.GRAY);
    }
}