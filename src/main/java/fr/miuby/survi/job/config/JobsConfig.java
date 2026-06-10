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

    private final MinerCfg     miner;
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

        MinerCfg(double[] dropMultiplier) {
            this.dropMultiplier = dropMultiplier;
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
        /** Amplificateur DOLPHINS_GRACE par niveau (actif à partir du niveau 3). */
        private final int[]    swimSpeedAmplifier;
        /** Amplificateur HASTE par niveau (actif à partir du niveau 7). */
        private final int[]    underwaterHasteAmplifier;
        private final int      effectDurationTicks;

        FishermanCfg(int vanillaMinWaitTicks, int vanillaMaxWaitTicks,
                     double[] fishingWaitMultiplier, double[] lootMultiplier,
                     double[] dirtChance, Material[] dirtReplacementMaterials,
                     double[] treasurePenalty, Material[] treasureReplacementMaterials,
                     int[] pressureSafeDepth, double pressureDamage,
                     int[] swimSpeedAmplifier, int[] underwaterHasteAmplifier,
                     int effectDurationTicks) {
            this.vanillaMinWaitTicks          = vanillaMinWaitTicks;
            this.vanillaMaxWaitTicks          = vanillaMaxWaitTicks;
            this.fishingWaitMultiplier        = fishingWaitMultiplier;
            this.lootMultiplier               = lootMultiplier;
            this.dirtChance                   = dirtChance;
            this.dirtReplacementMaterials     = dirtReplacementMaterials;
            this.treasurePenalty              = treasurePenalty;
            this.treasureReplacementMaterials = treasureReplacementMaterials;
            this.pressureSafeDepth            = pressureSafeDepth;
            this.pressureDamage               = pressureDamage;
            this.swimSpeedAmplifier           = swimSpeedAmplifier;
            this.underwaterHasteAmplifier     = underwaterHasteAmplifier;
            this.effectDurationTicks          = effectDurationTicks;
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