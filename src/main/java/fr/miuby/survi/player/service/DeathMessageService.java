package fr.miuby.survi.player.service;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
    // Conventions :  %1$s = nom du joueur,
    //                %2$s = nom de l'entité tueuse (pour les kills PvE/PvP)

    /** Chute — Explorer (niv. bas → seuil très faible, niv. haut → grosse chute quand même) */
    private static final String[] FALL_MESSAGES = {
            "%1$s s'est foulé la cheville et en est mort.",
            "%1$s a raté son atterrissage. Définitivement.",
            "%1$s a fait la démonstration qu'un explorateur peut encore mourir d'une chute stupide.",
            "Le sol et %1$s se sont rencontrés violemment. Son niveau d'explorateur n'a pas vraiment aidé.",
            "%1$s voulait explorer le sol de plus près. Mission accomplie."
    };

    /** Pression aquatique — Pêcheur */
    private static final String[] FISHERMAN_MESSAGES = {
            "%1$s est mort noyé dans une flaque.",
            "%1$s a voulu devenir un poisson à la place d'un pêcheur",
            "%1$s a mieux réussi à couler qu'à pêcher.",
            "La pression aquatique a eu raison de %1$s. Un pêcheur qui avait encore beaucoup à apprendre."
    };

    /** Noyade (sans lien direct avec le métier) */
    private static final String[] DROWNING_MESSAGES = {
            "%s a passé trop de temps la tête sous l'eau. C'était facultatif.",
            "%s voulait voir la vie aquatique de l'intérieur. Elle n'en a pas voulu.",
            "%s s'est souvenu trop tard qu'on ne respire pas sous l'eau."
    };

    /** Famine */
    private static final String[] STARVATION_MESSAGES = {
            "%s est mort d'avoir oublié l'heure du repas. Un classique.",
            "%s a refusé de manger. La mort n'a pas refusé.",
            "%s avait faim mais pas la motivation de manger. Ce n'est plus un problème."
    };

    /** Vide */
    private static final String[] VOID_MESSAGES = {
            "%s a sauté dans le vide pour voir ce qu'il y avait en dessous. La réponse : rien.",
            "%s a trouvé la sortie du monde. Elle est sans retour.",
            "%s a voulu toucher le fond. Il y est parvenu."
    };

    /** Feu / flammes */
    private static final String[] FIRE_MESSAGES = {
            "%s a voulu vérifier si le feu brûle vraiment. Verdict : oui.",
            "%s a pris feu et n'a pas pu s'éteindre.",
            "%s s'était convaincu que les flammes, c'était pas si grave. Il avait tort."
    };

    /** Lave */
    private static final String[] LAVA_MESSAGES = {
            "%s a pris un bain de lave. Le spa était définitif.",
            "%s a nagé dans la lave. Brièvement.",
            "%s a trouvé que l'eau était trop froide et a cherché une alternative. C'était un peu trop chaud."
    };

    /** Foudre */
    private static final String[] LIGHTNING_MESSAGES = {
            "%s a attiré la foudre. Littéralement.",
            "%s s'est trouvé au mauvais endroit lors d'un orage. Très mauvais endroit.",
            "%s a décidé de tester la conductivité de son équipement par temps d'orage."
    };

    /** Contact (cactus, buisson de baies...) */
    private static final String[] CONTACT_MESSAGES = {
            "%s a câliné un cactus. Le cactus n'a pas rendu le câlin.",
            "%s pensait que les cactus n'étaient que décoratifs.",
            "%s et un cactus se sont rencontrés. Seul le cactus s'en est sorti."
    };

    /** Suffocation / écrasement / mur */
    private static final String[] SUFFOCATION_MESSAGES = {
            "%s s'est retrouvé dans un espace trop étroit pour sa survie.",
            "%s a décidé de s'incruster dans un mur. Le mur a refusé.",
            "%s a exploré l'intérieur d'un bloc. C'était une mauvaise idée."
    };

    /** Gel (poudre de neige) */
    private static final String[] FREEZE_MESSAGES = {
            "%s a décidé de s'allonger dans de la poudre de neige. Pour de bon.",
            "%s a trouvé que la neige était douce. Trop douce.",
            "%s a testé sa résistance au froid. Résultat : insuffisant."
    };

    /** Choc sonique (Vigile) */
    private static final String[] SONIC_BOOM_MESSAGES = {
            "%s a reçu un choc sonique de la vigile. Il a vibré un peu trop fort.",
            "%s a réveillé quelque chose qu'il n'aurait pas dû. La vigile a répondu.",
            "%s a découvert que le silence n'est pas la seule arme des vigiles."
    };

    /** Sol chaud (magma) */
    private static final String[] HOT_FLOOR_MESSAGES = {
            "%s a marché sur de la roche en fusion. Il n'avait pas les bonnes bottes.",
            "%s a testé la danse sur magma. C'était sa dernière.",
            "%s pensait que ça ne brûlerait pas à travers ses chaussures. Il avait tort."
    };

    /** Explosion (TNT, creeper, cristal) */
    private static final String[] EXPLOSION_MESSAGES = {
            "%s a été soufflé par une explosion. Ça ne pardonne pas.",
            "%s s'est retrouvé trop proche d'une explosion. Beaucoup trop proche.",
            "%s a découvert que la poudre à canon est très réactive."
    };

    /** Poison / magie */
    private static final String[] MAGIC_MESSAGES = {
            "%s a reçu un sort malveillant. La magie jouait contre lui.",
            "%s a été empoisonné. Il aurait dû lire l'étiquette.",
            "%s a sous-estimé le danger de la magie noire."
    };

    /** Flétrissement */
    private static final String[] WITHER_MESSAGES = {
            "%s a été rongé par le Flétrissement. Ça prend du temps, mais c'est efficace.",
            "%s a laissé le Flétrissement faire son travail. Il l'a fait.",
            "%s n'a pas trouvé de lait à temps. Dommage."
    };

    /** Tué par une créature */
    private static final String[] MOB_MESSAGES = {
            "%1$s s'est courageusement battu contre %2$s. Le courage n'était pas suffisant.",
            "%1$s a tenu tête à %2$s. Pas assez longtemps.",
            "%1$s a voulu affronter %2$s. La rencontre fut brève."
    };

    /** Tué par un autre joueur */
    private static final String[] PVP_MESSAGES = {
            "%1$s a rencontré %2$s. %2$s avait une épée.",
            "%1$s s'est battu contre %2$s. %2$s a gagné.",
            "%1$s a défié %2$s en duel. %2$s a accepté l'invitation."
    };

    /** Cause inconnue ou non couverte */
    private static final String[] GENERIC_MESSAGES = {
            "%s a trouvé une façon originale de quitter ce monde.",
            "%s a succombé à ses blessures.",
            "Et c'est ainsi que %s rendit l'âme."
    };

    // ─── Point d'entrée ──────────────────────────────────────────────────────────

    public static Component build(PlayerDeathEvent event, AlphaPlayer alpha) {
        String name = event.getPlayer().getName();
        EntityDamageEvent lastDamage = event.getPlayer().getLastDamageCause();
        EJob jobCause = alpha.getLastJobDamageCause();

        // Mort liée au pêcheur : dégâts CUSTOM + tag pêcheur actif
        if (jobCause == EJob.FISHERMAN
                && lastDamage != null
                && lastDamage.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            return gray(pick(FISHERMAN_MESSAGES), name);
        }

        if (lastDamage == null) return gray(pick(GENERIC_MESSAGES), name);

        return switch (lastDamage.getCause()) {
            case FALL                                          -> gray(pick(FALL_MESSAGES),        name);
            case DROWNING                                      -> gray(pick(DROWNING_MESSAGES),    name);
            case STARVATION                                    -> gray(pick(STARVATION_MESSAGES),  name);
            case VOID                                          -> gray(pick(VOID_MESSAGES),        name);
            case FIRE, FIRE_TICK                               -> gray(pick(FIRE_MESSAGES),        name);
            case LAVA                                          -> gray(pick(LAVA_MESSAGES),        name);
            case LIGHTNING                                     -> gray(pick(LIGHTNING_MESSAGES),   name);
            case CONTACT                                       -> gray(pick(CONTACT_MESSAGES),     name);
            case SUFFOCATION, FLY_INTO_WALL, CRAMMING          -> gray(pick(SUFFOCATION_MESSAGES), name);
            case FREEZE                                        -> gray(pick(FREEZE_MESSAGES),      name);
            case SONIC_BOOM                                    -> gray(pick(SONIC_BOOM_MESSAGES),  name);
            case HOT_FLOOR                                     -> gray(pick(HOT_FLOOR_MESSAGES),   name);
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION             -> gray(pick(EXPLOSION_MESSAGES),   name);
            case MAGIC, POISON                                 -> gray(pick(MAGIC_MESSAGES),       name);
            case WITHER                                        -> gray(pick(WITHER_MESSAGES),      name);
            case ENTITY_ATTACK, PROJECTILE, ENTITY_SWEEP_ATTACK -> buildEntityKillMessage(lastDamage, name);
            default                                            -> gray(pick(GENERIC_MESSAGES),     name);
        };
    }

    // ─── Kill par entité ─────────────────────────────────────────────────────────

    private static Component buildEntityKillMessage(EntityDamageEvent lastDamage, String name) {
        if (!(lastDamage instanceof EntityDamageByEntityEvent byEntity))
            return gray(pick(GENERIC_MESSAGES), name);

        Entity damager = byEntity.getDamager();

        // Projectile : remonter à la source (archer, blaze...)
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity shooter) damager = shooter;
        }

        if (damager instanceof Player killer)
            return gray(pick(PVP_MESSAGES), name, killer.getName());

        return gray(pick(MOB_MESSAGES), name, getEntityName(damager));
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────────

    private static String getEntityName(Entity entity) {
        Component customName = entity.customName();
        if (customName != null)
            return PlainTextComponentSerializer.plainText().serialize(customName);
        return formatMobName(entity.getType());
    }

    private static String formatMobName(EntityType type) {
        return switch (type) {
            case ZOMBIE            -> "zombie";
            case SKELETON          -> "squelette";
            case CREEPER           -> "creeper";
            case SPIDER            -> "araignée";
            case CAVE_SPIDER       -> "araignée des cavernes";
            case ENDERMAN          -> "enderman";
            case WITCH             -> "sorcière";
            case SLIME             -> "slime";
            case BLAZE             -> "blaze";
            case GHAST             -> "ghast";
            case WITHER_SKELETON   -> "squelette du Nether";
            case PIGLIN            -> "piglin";
            case HOGLIN            -> "hoglin";
            case DROWNED           -> "noyé";
            case ZOMBIE_VILLAGER   -> "zombie-villageois";
            case HUSK              -> "husk";
            case STRAY             -> "stray";
            case PILLAGER          -> "pillard";
            case RAVAGER           -> "ravageur";
            case VINDICATOR        -> "vindicateur";
            case EVOKER            -> "évocateur";
            case VEX               -> "vex";
            case PHANTOM           -> "fantôme";
            case ENDERMITE         -> "endermite";
            case SILVERFISH        -> "poisson d'argent";
            case GUARDIAN          -> "gardien";
            case ELDER_GUARDIAN    -> "gardien ancien";
            case SHULKER           -> "shulker";
            case WARDEN            -> "vigile";
            case WITHER            -> "wither";
            case ENDER_DRAGON      -> "dragon de l'End";
            case ZOGLIN            -> "zoglin";
            case PIGLIN_BRUTE      -> "piglin brute";
            case ZOMBIFIED_PIGLIN  -> "piglin zombifié";
            case MAGMA_CUBE        -> "cube de magma";
            case WOLF              -> "loup";
            case BEE               -> "abeille";
            case IRON_GOLEM        -> "golem de fer";
            case LLAMA             -> "lama";
            case POLAR_BEAR        -> "ours polaire";
            case GOAT              -> "chèvre";
            case BREEZE            -> "bourrasque";
            case BOGGED            -> "bourbeux";
            case TRADER_LLAMA      -> "lama de marchand";
            default                -> type.name().toLowerCase().replace('_', ' ');
        };
    }

    private static String pick(String[] pool) {
        return pool[RANDOM.nextInt(pool.length)];
    }

    /** Construit un {@link Component} gris à partir d'un template {@link String#format}. */
    private static Component gray(String template, Object... args) {
        return Component.text(String.format(template, args), NamedTextColor.WHITE);
    }
}