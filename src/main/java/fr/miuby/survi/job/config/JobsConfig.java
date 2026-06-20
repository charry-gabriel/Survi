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

        MinerCfg(double[] dropMultiplier, int[] caveNightVisionThresholdY, int[] caveDarknessThresholdY,
                 int[] netherDarknessThresholdY, int caveDarknessHysteresis, int caveNightVisionHysteresis,
                 int caveLightHysteresis) {
            this.dropMultiplier            = dropMultiplier;
            this.caveNightVisionThresholdY = caveNightVisionThresholdY;
            this.caveDarknessThresholdY    = caveDarknessThresholdY;
            this.netherDarknessThresholdY  = netherDarknessThresholdY;
            this.caveDarknessHysteresis    = caveDarknessHysteresis;
            this.caveNightVisionHysteresis = caveNightVisionHysteresis;
            this.caveLightHysteresis       = caveLightHysteresis;
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

        LumberjackCfg(double[] dropMultiplier, double[] charcoalChance, double[] appleLeafChance,
                      double[] fireDamageMultiplier, int[] treeFellerExtraLogs, int[] fireResistanceTicks) {
            this.dropMultiplier       = dropMultiplier;
            this.charcoalChance       = charcoalChance;
            this.appleLeafChance      = appleLeafChance;
            this.fireDamageMultiplier = fireDamageMultiplier;
            this.treeFellerExtraLogs  = treeFellerExtraLogs;
            this.fireResistanceTicks  = fireResistanceTicks;
        }
    }

    @Getter
    public static final class FarmerCfg {
        /** Multiplicateur de drops sur les cultures et mobs passifs. Index = niveau. */
        private final double[] dropMultiplier;
        private final double[] cropGrowthAllowChance;
        private final double[] cropExtraGrowthChance;
        private final double   cropThirdTickChanceAtMax;

        FarmerCfg(double[] dropMultiplier, double[] cropGrowthAllowChance,
                  double[] cropExtraGrowthChance, double cropThirdTickChanceAtMax) {
            this.dropMultiplier          = dropMultiplier;
            this.cropGrowthAllowChance   = cropGrowthAllowChance;
            this.cropExtraGrowthChance   = cropExtraGrowthChance;
            this.cropThirdTickChanceAtMax = cropThirdTickChanceAtMax;
        }
    }

    @Getter
    public static final class EnchanterCfg {
        private final double[] durabilityLossMultiplier;
        /** -1 = illimité. */
        private final int[]    anvilMaxXpCost;
        /** Durabilité réparée par XP gagné (0 = pas de réparation à ce niveau). */
        private final int[]    repairPerXp;

        EnchanterCfg(double[] durabilityLossMultiplier, int[] anvilMaxXpCost, int[] repairPerXp) {
            this.durabilityLossMultiplier = durabilityLossMultiplier;
            this.anvilMaxXpCost           = anvilMaxXpCost;
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
        /** Bonus de ticks de respiration (ADD_NUMBER sur {@code OXYGEN_BONUS}) par niveau. Base vanilla = 300 ticks. */
        private final int[]    oxygenBonusTicks;
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
                     double[] underwaterSpeedModifier, int[] oxygenBonusTicks,
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
            this.oxygenBonusTicks                 = oxygenBonusTicks;
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

        ExplorerCfg(double[] safeFallDistance) {
            this.safeFallDistance = safeFallDistance;
        }
    }
}