package fr.miuby.survi.mob;

import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * Cœur du système de niveaux des monstres.
 *
 * <h3>Principe</h3>
 * <ul>
 *   <li>Chaque palier du monde (world level N) correspond à une tranche de niveaux mob :
 *       {@code [N * levelsPerTier + 1 … (N+1) * levelsPerTier]}.</li>
 *   <li>Un tirage pondéré favorise les bas niveaux du palier (les élites sont rares).</li>
 *   <li>Les stats évoluent linéairement : {@code valeur = base + (level - 1) * perLevel}.</li>
 * </ul>
 *
 * <h3>Mécaniques spéciales</h3>
 * <ul>
 *   <li><b>Creeper</b> : rayon d'explosion ET durée de mèche (fuse-ticks) scalés.</li>
 *   <li><b>EnderDragon</b> : reçoit le niveau MAX du palier actuel.</li>
 *   <li><b>Enderman</b> : peut ramasser des blocs plus fréquemment à haut niveau.</li>
 * </ul>
 *
 * <h3>Chargement</h3>
 * Appeler {@link #init()} après que le plugin est initialisé.
 * Le fichier {@code monsters.yml} est copié depuis le jar s'il n'existe pas.
 */
public class MobLevelManager {
    public MobLevelManager() {}

    // ─── Clé PDC ─────────────────────────────────────────────────────────────────

    /** Clé PersistentDataContainer pour stocker le niveau d'un mob. */
    public static final NamespacedKey MOB_LEVEL_KEY = new NamespacedKey("survi", "mob_level");

    // ─── État ─────────────────────────────────────────────────────────────────────

    private final Map<EntityType, MobTypeConfig> configs        = new EnumMap<>(EntityType.class);
    private final Map<EntityType, String>        cachedMobNames = new EnumMap<>(EntityType.class);
    private final Random random = new Random();

    @Getter
    private int    levelsPerTier       = 10;
    private double spawnWeightExponent = 1.8;

    /**
     * Poids pré-calculés pour {@link #rollMobLevel()}.
     * {@code cachedWeights[i] = (levelsPerTier - i) ^ spawnWeightExponent}
     * Ne dépend pas du world level — recalculé uniquement dans {@link #loadConfig()}.
     */
    private double[] cachedWeights;
    private double   cachedWeightsTotal;

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Charge la configuration depuis {@code monsters.yml} et démarre la tâche de nametag.
     * Appeler une seule fois au démarrage — pour les rechargements à chaud, utiliser {@link #reload()}.
     */
    public void init() {
        loadConfig();
        new MobNametagTask().runTaskTimer(GameManager.getInstance().getPlugin(), 0L, MobNametagTask.PERIOD_TICKS);
    }

    /**
     * Recharge la configuration depuis {@code monsters.yml} à chaud, sans redémarrage.
     * Le timer de nametag existant n'est pas recréé.
     *
     * <p>Les mobs déjà spawnés conservent leurs stats actuelles ; seuls les nouveaux spawns
     * appliqueront la configuration mise à jour.</p>
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Lit {@code monsters.yml} et reconstruit {@link #configs} et {@link #cachedMobNames}.
     * Commun à {@link #init()} et {@link #reload()}.
     */
    private void loadConfig() {
        File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), "monsters.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        levelsPerTier       = cfg.getInt("levels-per-world-tier", 10);
        spawnWeightExponent = cfg.getDouble("spawn-weight-exponent", 1.8);

        // Pré-calcul des poids de tirage — indépendants du world level, donc stables
        // jusqu'au prochain loadConfig(). Évite d'allouer un double[] à chaque spawn.
        cachedWeights = new double[levelsPerTier];
        cachedWeightsTotal = 0;
        for (int i = 0; i < levelsPerTier; i++) {
            cachedWeights[i] = Math.pow(levelsPerTier - (double) i, spawnWeightExponent);
            cachedWeightsTotal += cachedWeights[i];
        }

        configs.clear();

        ConfigurationSection mobsSection = cfg.getConfigurationSection("mobs");
        if (mobsSection == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                    "[MobLevel] Aucune section 'mobs' dans monsters.yml — aucun mob scalé.");
            return;
        }

        for (String key : mobsSection.getKeys(false)) {
            EntityType type = parseEntityType(key);
            if (type == null) continue;

            ConfigurationSection mobSec = mobsSection.getConfigurationSection(key);
            if (mobSec == null) continue;

            MobTypeConfig typeConfig = new MobTypeConfig();

            // ── Stats classiques (attributs Bukkit) ──────────────────────────────
            ConfigurationSection statsSec = mobSec.getConfigurationSection("stats");
            if (statsSec != null) {
                for (EMobStat stat : EMobStat.values()) {
                    ConfigurationSection ss = statsSec.getConfigurationSection(stat.getConfigKey());
                    if (ss == null) continue;
                    typeConfig.addStat(stat, new MobTypeConfig.LinearStat(
                            ss.getDouble("base", 0),
                            ss.getDouble("per-level", 0)
                    ));
                }
            }

            // ── Rayon d'explosion Creeper ─────────────────────────────────────────
            ConfigurationSection expSec = mobSec.getConfigurationSection("explosion-radius");
            if (expSec != null) {
                typeConfig.setExplosionRadius(new MobTypeConfig.LinearStat(
                        expSec.getDouble("base", 3),
                        expSec.getDouble("per-level", 0)
                ));
            }

            // ── Durée de mèche Creeper (fuse-ticks) ──────────────────────────────
            ConfigurationSection fuseSec = mobSec.getConfigurationSection("fuse-ticks");
            if (fuseSec != null) {
                typeConfig.setFuseTicks(
                        fuseSec.getDouble("base", 30),
                        fuseSec.getDouble("per-level", 0),
                        fuseSec.getInt("min", 5)
                );
            }

            // ── Effets de potion à l'attaque (araignée, etc.) ────────────────────
            List<?> potionList = mobSec.getList("potion-effects");
            if (potionList != null) {
                for (Object obj : potionList) {
                    if (!(obj instanceof Map<?, ?> map)) continue;
                    MobPotionEffectConfig pec = parsePotionEffect(map);
                    if (pec != null) typeConfig.addPotionEffect(pec);
                }
            }

            configs.put(type, typeConfig);
        }

        // Pré-calcul des noms de mobs — pur, déterministe, une fois suffit par type.
        cachedMobNames.clear();
        for (EntityType type : configs.keySet()) {
            cachedMobNames.put(type, buildMobName(type));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[MobLevel] " + configs.size() + " types de mobs chargés depuis monsters.yml (levelsPerTier="
                        + levelsPerTier + ", weightExp=" + spawnWeightExponent + ")");
    }

    // ─── Tirage de niveau ─────────────────────────────────────────────────────────

    /**
     * Tire aléatoirement un niveau de mob pour le palier actuel du monde.
     * <p>Distribution pondérée : niveau bas = plus fréquent, niveau haut = élite rare.
     * Formule du poids pour l'offset {@code i} (0 = plus commun) :
     * {@code weight(i) = (levelsPerTier - i) ^ spawnWeightExponent}
     * <p>Les poids sont pré-calculés dans {@link #loadConfig()} — aucune allocation ici.
     */
    public int rollMobLevel() {
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();
        int tierStart  = (worldLevel - 1) * levelsPerTier + 1;

        double roll       = random.nextDouble() * cachedWeightsTotal;
        double cumulative = 0;
        for (int i = 0; i < cachedWeights.length; i++) {
            cumulative += cachedWeights[i];
            if (roll <= cumulative) return tierStart + i;
        }
        return tierStart;
    }

    /**
     * Renvoie le niveau maximum du palier actuel (utilisé pour l'EnderDragon).
     */
    public int getMaxLevelForCurrentTier() {
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();
        return worldLevel * levelsPerTier;
    }

    // ─── Application des stats ────────────────────────────────────────────────────

    /**
     * Applique le niveau à un mob vivant :
     * <ol>
     *   <li>Attributs Bukkit (vie, dégâts, vitesse, taille…)</li>
     *   <li>Mécaniques spéciales selon le type (Creeper, Enderman…)</li>
     *   <li>Nametag coloré avec le niveau</li>
     *   <li>Stockage du niveau dans le PDC</li>
     * </ol>
     *
     * <p>Pour l'EnderDragon, utilise toujours le niveau max du palier.
     */
    public void applyLevel(LivingEntity entity, int level) {
        EntityType type = entity.getType();
        MobTypeConfig typeConfig = configs.get(type);
        if (typeConfig == null) return;

        // L'EnderDragon est un boss : il reçoit toujours le niveau MAX du palier
        int effectiveLevel = (entity instanceof EnderDragon)
                ? getMaxLevelForCurrentTier()
                : level;

        // ── Attributs Bukkit ─────────────────────────────────────────────────────
        for (EMobStat stat : EMobStat.values()) {
            double value = typeConfig.getStatValue(stat, effectiveLevel);
            if (value < 0) continue;

            try {
                AttributeInstance inst = entity.getAttribute(stat.getAttribute());
                if (inst == null) continue;

                // Clamp pour éviter de dépasser les limites internes de Bukkit
                double maxAllowed = inst.getDefaultValue() * 25;
                double clamped    = Math.min(value, maxAllowed);
                inst.setBaseValue(clamped);

                // Synchronise la vie actuelle après avoir changé la vie max
                if (stat == EMobStat.MAX_HEALTH) {
                    entity.setHealth(clamped);
                }
            } catch (Exception _) {
                // L'attribut n'existe pas pour ce type de mob — ignoré silencieusement
            }
        }

        // ── Mécaniques spéciales ─────────────────────────────────────────────────
        applySpecialMechanics(entity, typeConfig, effectiveLevel);

        // ── PDC : stockage du niveau ──────────────────────────────────────────────
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(MOB_LEVEL_KEY, PersistentDataType.INTEGER, effectiveLevel);

        // ── Nametag ───────────────────────────────────────────────────────────────
        applyNametag(entity, effectiveLevel);
    }

    /**
     * Applique les comportements spéciaux selon le type de mob :
     * <ul>
     *   <li><b>Creeper</b> : rayon d'explosion + durée de mèche</li>
     *   <li><b>Enderman</b> : peut ramasser des blocs plus souvent à haut niveau</li>
     * </ul>
     */
    private void applySpecialMechanics(LivingEntity entity, MobTypeConfig typeConfig, int level) {

        // ── Creeper : explosion radius ────────────────────────────────────────────
        if (entity instanceof Creeper creeper) {
            MobTypeConfig.LinearStat expCfg = typeConfig.getExplosionRadius();
            if (expCfg != null) {
                int radius = (int) Math.round(expCfg.compute(level));
                creeper.setExplosionRadius(Math.clamp(radius, 1, 15));
            }

            // ── Creeper : fuse-ticks (vitesse d'explosion) ────────────────────────
            MobTypeConfig.FuseStat fuseCfg = typeConfig.getFuseTicks();
            if (fuseCfg != null) {
                int ticks = (int) Math.round(fuseCfg.compute(level));
                ticks = Math.max(fuseCfg.min(), ticks);
                creeper.setFuseTicks(ticks);
            }
        }
    }

    // ─── Nametag ─────────────────────────────────────────────────────────────────

    private void applyNametag(LivingEntity entity, int level) {
        NamedTextColor levelColor = getLevelColor(level);
        String mobName = formatMobName(entity.getType());

        Component name = Component.text()
                .append(Component.text("[Niv. ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(level), levelColor, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(mobName, NamedTextColor.WHITE))
                .build();

        entity.customName(name);
        entity.setCustomNameVisible(false); // géré dynamiquement par MobNametagTask
    }

    /**
     * Couleur du niveau basée sur la rareté relative dans le palier courant.
     * <pre>
     *   offset 0–29 %  → Vert   (commun)
     *   offset 30–59 % → Jaune  (peu commun)
     *   offset 60–84 % → Or     (rare)
     *   offset 85+ %   → Rouge  (élite)
     * </pre>
     */
    private NamedTextColor getLevelColor(int level) {
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();
        int tierStart  = (worldLevel - 1) * levelsPerTier + 1;
        int offset     = Math.max(0, level - tierStart);
        double ratio   = levelsPerTier > 1 ? (double) offset / (levelsPerTier - 1) : 0;

        if (ratio < 0.30) return NamedTextColor.GREEN;
        if (ratio < 0.60) return NamedTextColor.YELLOW;
        if (ratio < 0.85) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }

    // ─── Accesseurs publics ───────────────────────────────────────────────────────

    /** @return {@code true} si le type est configuré et activé. */
    public boolean isManaged(EntityType type) {
        return configs.get(type) != null;
    }

    /** @return le niveau stocké dans le PDC du mob, ou {@code -1} s'il n'en a pas. */
    public int getStoredLevel(LivingEntity entity) {
        return entity.getPersistentDataContainer()
                .getOrDefault(MOB_LEVEL_KEY, PersistentDataType.INTEGER, -1);
    }

    /** @return la config du type, ou {@code null} s'il n'est pas géré. */
    public MobTypeConfig getConfig(EntityType type) {
        return configs.get(type);
    }

    /**
     * @return l'ensemble des {@link EntityType} configurés dans {@code monsters.yml}
     *         (activés ou non). Utilisé par l'auto-complétion des commandes.
     */
    public java.util.Set<EntityType> getConfiguredTypes() {
        return java.util.Collections.unmodifiableSet(configs.keySet());
    }

    /**
     * Component compact pour les messages de mort : {@code Zombie[5]}.
     * La couleur du niveau est la même que sur le nametag.
     * Si le mob n'a pas de niveau en PDC, retourne simplement le nom du type.
     */
    public Component buildDeathName(LivingEntity entity) {
        String mobName = formatMobName(entity.getType());
        int level = getStoredLevel(entity);
        if (level < 0) return Component.text(mobName, NamedTextColor.WHITE);
        NamedTextColor levelColor = getLevelColor(level);
        return Component.text(mobName, NamedTextColor.WHITE)
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(level), levelColor, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.GRAY));
    }

    // ─── Utilitaires privés ───────────────────────────────────────────────────────

    private EntityType parseEntityType(String key) {
        try {
            return EntityType.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException _) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                    "[MobLevel] Type de mob inconnu dans monsters.yml : '" + key + "' — ignoré.");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private MobPotionEffectConfig parsePotionEffect(Map<?, ?> rawMap) {
        try {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            PotionEffectType pet = PotionEffectType.getByName(String.valueOf(map.get("type")));
            if (pet == null) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                        "[MobLevel] PotionEffectType inconnu : " + map.get("type"));
                return null;
            }
            return new MobPotionEffectConfig(
                    pet,
                    toInt   (map.get("min-mob-level"),           1),
                    toInt   (map.get("duration-base"),           40),
                    toDouble(map.get("duration-per-level"),      3.0),
                    toInt   (map.get("amplifier-upgrade-every"), 30),
                    toDouble(map.get("chance-base"),             0.10),
                    toDouble(map.get("chance-per-level"),        0.004),
                    toDouble(map.get("max-chance"),              0.80)
            );
        } catch (Exception e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM,
                    "[MobLevel] Erreur lors du parsing d'un effet de potion", e);
            return null;
        }
    }

    /**
     * Formate un EntityType en nom lisible (ex. CAVE_SPIDER → "Cave Spider").
     * Retourne le nom pré-calculé dans {@link #cachedMobNames} — aucune allocation.
     */
    private String formatMobName(EntityType type) {
        return cachedMobNames.getOrDefault(type, type.name());
    }

    /** Construit le nom lisible d'un EntityType. Appelé uniquement dans {@link #loadConfig()}. */
    private String buildMobName(EntityType type) {
        String[] parts = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String p = parts[i];
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private int    toInt   (Object v, int    def) { return v instanceof Number n ? n.intValue()    : def; }
    private double toDouble(Object v, double def) { return v instanceof Number n ? n.doubleValue() : def; }
}