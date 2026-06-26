package fr.miuby.survi.job.config;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Singleton contenant tous les paramètres numériques des métiers,
 * chargés depuis les fichiers {@code jobs/*.yml} par {@link JobsLoader}.
 *
 * <p>Toutes les listes ont 11 entrées (index = niveau 0 à 10).</p>
 *
 * <p>Usage dans un listener :</p>
 * <pre>{@code
 *   JobsConfig cfg = JobsConfig.getInstance();
 *   double charcoalChance = cfg.getLumberjack().getCharcoalChance()[level];
 *   double minerMult      = cfg.getMiner().getDropMultiplier()[level];
 * }</pre>
 */
@Getter
public final class JobsConfig {

    private static JobsConfig instance;

    public static JobsConfig getInstance() {
        if (instance == null)
            throw new IllegalStateException("JobsConfig non chargé — appelez JobsLoader.load() d'abord.");
        return instance;
    }

    static void setInstance(JobsConfig cfg) {
        instance = cfg;
    }

    // ─── Par métier ──────────────────────────────────────────────────────────────

    private final MinerCfg      miner;
    private final LumberjackCfg lumberjack;
    private final FarmerCfg     farmer;
    private final EnchanterCfg  enchanter;
    private final FishermanCfg  fisherman;
    private final ExplorerCfg   explorer;

    JobsConfig(MinerCfg miner, LumberjackCfg lumberjack, FarmerCfg farmer,
               EnchanterCfg enchanter, FishermanCfg fisherman, ExplorerCfg explorer) {
        this.miner      = miner;
        this.lumberjack = lumberjack;
        this.farmer     = farmer;
        this.enchanter  = enchanter;
        this.fisherman  = fisherman;
        this.explorer   = explorer;
    }

    // ─── Sous-configs ────────────────────────────────────────────────────────────

    @Getter
    public static final class MinerCfg {
        /** Multiplicateur de drops sur les minerais. Index = niveau. */
        private final double[] dropMultiplier;

        /**
         * Coordonnée Y sous laquelle l'effet NIGHT_VISION est retiré (toujours retiré au-dessus de Y=63,
         * seuil fixe non configurable). {@code -1} = illimité (night vision jusqu'au bedrock). Index = niveau.
         */
        private final int[] caveNightVisionThresholdY;

        /**
         * Coordonnée Y sous laquelle l'effet DARKNESS est appliqué (sans icône ni minuteur). {@code -1} = immunisé. Index = niveau.
         */
        private final int[] caveDarknessThresholdY;

        /**
         * Coordonnée Y, spécifique au Nether, sous laquelle l'effet DARKNESS est appliqué.
         * Aucune night vision n'est jamais appliquée au Nether. {@code -1} = immunisé. Index = niveau.
         */
        private final int[] netherDarknessThresholdY;

        /** Marge (blocs Y) avant la sortie de l'effet DARKNESS (grotte ET Nether). */
        private final int caveDarknessHysteresis;

        /** Marge (blocs Y) avant la sortie de l'effet NIGHT_VISION. */
        private final int caveNightVisionHysteresis;

        /** Marge (niveau de lumière du ciel, 0-15) avant de considérer qu'on n'est plus en situation de "grotte". */
        private final int caveLightHysteresis;

        /**
         * Nombre de minerais supplémentaires cassés par le vein miner (BFS sur le filon connecté),
         * débloqué par l'ability {@code vein_miner} du casque ({@code GROWTH_MINER_HELMET}). Index = niveau.
         * 0 = vein miner inactif à ce niveau même si l'ability est débloquée sur l'item.
         */
        private final int[] veinMinerExtraOres;

        MinerCfg(double[] dropMultiplier, int[] caveNightVisionThresholdY, int[] caveDarknessThresholdY,
                 int[] netherDarknessThresholdY, int caveDarknessHysteresis, int caveNightVisionHysteresis,
                 int caveLightHysteresis, int[] veinMinerExtraOres) {
            this.dropMultiplier            = dropMultiplier;
            this.caveNightVisionThresholdY = caveNightVisionThresholdY;
            this.caveDarknessThresholdY    = caveDarknessThresholdY;
            this.netherDarknessThresholdY  = netherDarknessThresholdY;
            this.caveDarknessHysteresis    = caveDarknessHysteresis;
            this.caveNightVisionHysteresis = caveNightVisionHysteresis;
            this.caveLightHysteresis       = caveLightHysteresis;
            this.veinMinerExtraOres        = veinMinerExtraOres;
        }
    }

    @Getter
    public static final class LumberjackCfg {
        /** Multiplicateur de drops sur les bûches. Index = niveau. */
        private final double[] dropMultiplier;
        private final double[] charcoalChance;
        private final double[] appleLeafChance;
        private final double[] fireDamageMultiplier;
        private final int[]    treeFellerExtraLogs;
        private final int[]    fireResistanceTicks;
        /** Probabilité d'autoriser un tick de croissance sur un sapling planté (niv.0–4 = ralentissement). Index = niveau. */
        private final double[] saplingGrowthAllowChance;
        /** Probabilité d'un tick de croissance bonus après un tick normal (niv.6–10). Index = niveau. */
        private final double[] saplingExtraGrowthChance;
        /** Probabilité d'un 3e tick bonus au niveau maximum. */
        private final double   saplingThirdTickChanceAtMax;
        /** Probabilité que la farine d'os fonctionne sur un sapling planté par un bûcheron. Index = niveau. */
        private final double[] saplingBoneMealChance;

        LumberjackCfg(double[] dropMultiplier, double[] charcoalChance, double[] appleLeafChance,
                      double[] fireDamageMultiplier, int[] treeFellerExtraLogs, int[] fireResistanceTicks,
                      double[] saplingGrowthAllowChance, double[] saplingExtraGrowthChance,
                      double saplingThirdTickChanceAtMax, double[] saplingBoneMealChance) {
            this.dropMultiplier             = dropMultiplier;
            this.charcoalChance             = charcoalChance;
            this.appleLeafChance            = appleLeafChance;
            this.fireDamageMultiplier       = fireDamageMultiplier;
            this.treeFellerExtraLogs        = treeFellerExtraLogs;
            this.fireResistanceTicks        = fireResistanceTicks;
            this.saplingGrowthAllowChance   = saplingGrowthAllowChance;
            this.saplingExtraGrowthChance   = saplingExtraGrowthChance;
            this.saplingThirdTickChanceAtMax = saplingThirdTickChanceAtMax;
            this.saplingBoneMealChance      = saplingBoneMealChance;
        }
    }

    @Getter
    public static final class FarmerCfg {
        /** Multiplicateur de drops sur les cultures et mobs passifs. Index = niveau. */
        private final double[] dropMultiplier;
        private final double[] cropGrowthAllowChance;
        private final double[] cropExtraGrowthChance;
        private final double   cropThirdTickChanceAtMax;
        /** Probabilité que la farine d'os fonctionne. Index = niveau. */
        private final double[] boneMealChance;

        FarmerCfg(double[] dropMultiplier, double[] cropGrowthAllowChance,
                  double[] cropExtraGrowthChance, double cropThirdTickChanceAtMax,
                  double[] boneMealChance) {
            this.dropMultiplier          = dropMultiplier;
            this.cropGrowthAllowChance   = cropGrowthAllowChance;
            this.cropExtraGrowthChance   = cropExtraGrowthChance;
            this.cropThirdTickChanceAtMax = cropThirdTickChanceAtMax;
            this.boneMealChance          = boneMealChance;
        }
    }

    @Getter
    public static final class EnchanterCfg {
        private final double[] durabilityLossMultiplier;
        /** -1 = illimité. */
        private final int[]    anvilMaxXpCost;
        /** Somme maximale des niveaux de tous les enchantements sur l'item résultant d'une opération d'enclume. -1 = illimité. */
        private final int[]    anvilMaxEnchantSum;
        /** Coût XP maximum pour la table d'enchantement. -1 = illimité. */
        private final int[]    enchantMaxXpCost;
        /** Niveau d'enchantement maximum autorisé (table et enclume). -1 = illimité. */
        private final int[]    enchantMaxLevel;
        /** Durabilité réparée par XP gagné (0 = pas de réparation à ce niveau). */
        private final int[]    repairPerXp;

        EnchanterCfg(double[] durabilityLossMultiplier, int[] anvilMaxXpCost, int[] anvilMaxEnchantSum,
                     int[] enchantMaxXpCost, int[] enchantMaxLevel, int[] repairPerXp) {
            this.durabilityLossMultiplier = durabilityLossMultiplier;
            this.anvilMaxXpCost           = anvilMaxXpCost;
            this.anvilMaxEnchantSum       = anvilMaxEnchantSum;
            this.enchantMaxXpCost         = enchantMaxXpCost;
            this.enchantMaxLevel          = enchantMaxLevel;
            this.repairPerXp              = repairPerXp;
        }
    }

    @Getter
    public static final class FishermanCfg {
        private final int      vanillaMinWaitTicks;
        private final int      vanillaMaxWaitTicks;
        private final double[] fishingWaitMultiplier;
        private final double[] lootMultiplier;
        /** Probabilité de remplacer tout item pêché. Tombe à 0 au niveau 7. */
        private final double[] dirtChance;
        /** Matériaux tirés aléatoirement quand dirt-chance se déclenche. */
        private final Material[] dirtReplacementMaterials;
        /** Probabilité supplémentaire sur les trésors après dirt-chance. Tombe à 0 au niveau 6. */
        private final double[] treasurePenalty;
        /** Matériaux tirés aléatoirement quand treasure-penalty se déclenche. */
        private final Material[] treasureReplacementMaterials;
        /** -1 = aucun dégât de pression à ce niveau. Valeurs très négatives = dégâts permanents. */
        private final int[]    pressureSafeDepth;
        private final double   pressureDamage;
        /** Modificateur ADD_NUMBER sur {@code WATER_MOVEMENT_EFFICIENCY} par niveau. Valeur 0.0 = vitesse vanilla. */
        private final double[] underwaterSpeedModifier;
        /**
         * Valeur ADD_NUMBER appliquée sur {@code OXYGEN_BONUS} par niveau.
         * L'attribut est probabiliste : chaque tick sous l'eau, 1/(1+bonus) chance de perdre 1 air.
         * Durée totale = 300 × (1 + bonus) ticks. Formule inverse : bonus = durée_cible / 300 − 1.
         */
        private final double[] oxygenBonus;
        /** Modificateur ADD_NUMBER sur {@code SUBMERGED_MINING_SPEED} par niveau. Valeur 0.0 = vitesse vanilla. */
        private final double[] submergedMiningSpeedModifier;
        /** Dégâts infligés par tick de pluie acide aux joueurs sous le seuil (demi-cœurs). */
        private final double   acidRainDamage;
        /** Niveau Pêcheur minimum pour être immunisé à la pluie acide. */
        private final int      acidRainFishermanLevelThreshold;

        FishermanCfg(int vanillaMinWaitTicks, int vanillaMaxWaitTicks,
                     double[] fishingWaitMultiplier, double[] lootMultiplier,
                     double[] dirtChance, Material[] dirtReplacementMaterials,
                     double[] treasurePenalty, Material[] treasureReplacementMaterials,
                     int[] pressureSafeDepth, double pressureDamage,
                     double[] underwaterSpeedModifier, double[] oxygenBonus,
                     double[] submergedMiningSpeedModifier,
                     double acidRainDamage, int acidRainFishermanLevelThreshold) {
            this.vanillaMinWaitTicks              = vanillaMinWaitTicks;
            this.vanillaMaxWaitTicks              = vanillaMaxWaitTicks;
            this.fishingWaitMultiplier            = fishingWaitMultiplier;
            this.lootMultiplier                   = lootMultiplier;
            this.dirtChance                       = dirtChance;
            this.dirtReplacementMaterials         = dirtReplacementMaterials;
            this.treasurePenalty                  = treasurePenalty;
            this.treasureReplacementMaterials     = treasureReplacementMaterials;
            this.pressureSafeDepth                = pressureSafeDepth;
            this.pressureDamage                   = pressureDamage;
            this.underwaterSpeedModifier          = underwaterSpeedModifier;
            this.oxygenBonus                      = oxygenBonus;
            this.submergedMiningSpeedModifier     = submergedMiningSpeedModifier;
            this.acidRainDamage                   = acidRainDamage;
            this.acidRainFishermanLevelThreshold  = acidRainFishermanLevelThreshold;
        }
    }

    @Getter
    public static final class ExplorerCfg {
        /**
         * Distance de chute sécurisée en blocs par niveau.
         * Le modifier ADD_NUMBER appliqué = valeur - 3.0 (base vanilla).
         */
        private final double[] safeFallDistance;

        /**
         * Rayon Wilderness (en blocs XZ) autorisé par niveau. Le Nether utilise ce rayon ÷ 8.
         * L'End n'a aucune limite.
         */
        private final int[] wildernessRadiusPerLevel;

        ExplorerCfg(double[] safeFallDistance, int[] wildernessRadiusPerLevel) {
            this.safeFallDistance = safeFallDistance;
            this.wildernessRadiusPerLevel = wildernessRadiusPerLevel;
        }

        /**
         * Rayon d'exploration autorisé pour {@code level} (clampé à l'index maximum du tableau),
         * divisé par 8 si {@code isNether}.
         */
        public int wildernessRadiusForLevel(int level, boolean isNether) {
            int idx = Math.min(level, wildernessRadiusPerLevel.length - 1);
            int radius = wildernessRadiusPerLevel[idx];
            return isNether ? radius / 8 : radius;
        }
    }
}