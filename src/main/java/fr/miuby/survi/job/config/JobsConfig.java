package fr.miuby.survi.job.config;

import lombok.Getter;

/**
 * Singleton contenant tous les paramètres numériques des métiers,
 * chargés depuis {@code jobs.yml} par {@link JobsLoader}.
 *
 * <p>Toutes les listes ont 11 entrées (index = niveau 0 à 10).</p>
 *
 * <p>Usage dans un listener :</p>
 * <pre>{@code
 *   JobsConfig cfg = JobsConfig.getInstance();
 *   double charcoalChance = cfg.getLumberjack().getCharcoalChance()[level];
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

    // ─── Partagé ─────────────────────────────────────────────────────────────────

    /** Multiplicateur de drops partagé (MINER, LUMBERJACK, FARMER). Index = niveau. */
    private final double[] dropMultiplier;

    // ─── Par métier ──────────────────────────────────────────────────────────────

    private final LumberjackCfg lumberjack;
    private final FarmerCfg     farmer;
    private final EnchanterCfg  enchanter;
    private final FishermanCfg  fisherman;
    private final ExplorerCfg explorer;

    JobsConfig(double[] dropMultiplier,
               LumberjackCfg lumberjack,
               FarmerCfg farmer,
               EnchanterCfg enchanter,
               FishermanCfg fisherman,
               ExplorerCfg explorer) {
        this.dropMultiplier = dropMultiplier;
        this.lumberjack     = lumberjack;
        this.farmer         = farmer;
        this.enchanter      = enchanter;
        this.fisherman      = fisherman;
        this.explorer = explorer;
    }

    // ─── Sous-configs ────────────────────────────────────────────────────────────

    @Getter
    public static final class LumberjackCfg {
        private final double[] charcoalChance;
        private final double[] appleLeafChance;
        private final double[] fireDamageMultiplier;
        private final int[]    treeFellerExtraLogs;
        private final int[]    fireResistanceTicks;

        LumberjackCfg(double[] charcoalChance, double[] appleLeafChance,
                      double[] fireDamageMultiplier,
                      int[] treeFellerExtraLogs, int[] fireResistanceTicks) {
            this.charcoalChance        = charcoalChance;
            this.appleLeafChance       = appleLeafChance;
            this.fireDamageMultiplier  = fireDamageMultiplier;
            this.treeFellerExtraLogs   = treeFellerExtraLogs;
            this.fireResistanceTicks   = fireResistanceTicks;
        }
    }

    @Getter
    public static final class FarmerCfg {
        private final double[] cropGrowthAllowChance;
        private final double[] cropExtraGrowthChance;
        private final double   cropThirdTickChanceAtMax;

        FarmerCfg(double[] cropGrowthAllowChance, double[] cropExtraGrowthChance,
                  double cropThirdTickChanceAtMax) {
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
        /** Probabilité de remplacer tout item pêché par une dirt. Tombe à 0 au niveau 7. */
        private final double[] dirtChance;
        /** Probabilité supplémentaire de convertir un trésor en dirt, après dirt-chance. Tombe à 0 au niveau 6. */
        private final double[] treasurePenalty;
        /** -1 = aucun dégât de pression à ce niveau. */
        private final int[]    pressureSafeDepth;
        private final double   pressureDamage;
        /** Amplificateur DOLPHINS_GRACE par niveau (actif à partir du niveau 3). */
        private final int[]    swimSpeedAmplifier;
        /** Amplificateur HASTE par niveau (actif à partir du niveau 5). */
        private final int[]    underwaterHasteAmplifier;
        private final int      effectDurationTicks;

        FishermanCfg(int vanillaMinWaitTicks, int vanillaMaxWaitTicks,
                     double[] fishingWaitMultiplier, double[] lootMultiplier,
                     double[] dirtChance, double[] treasurePenalty,
                     int[] pressureSafeDepth, double pressureDamage,
                     int[] swimSpeedAmplifier, int[] underwaterHasteAmplifier,
                     int effectDurationTicks) {
            this.vanillaMinWaitTicks      = vanillaMinWaitTicks;
            this.vanillaMaxWaitTicks      = vanillaMaxWaitTicks;
            this.fishingWaitMultiplier    = fishingWaitMultiplier;
            this.lootMultiplier           = lootMultiplier;
            this.dirtChance               = dirtChance;
            this.treasurePenalty          = treasurePenalty;
            this.pressureSafeDepth        = pressureSafeDepth;
            this.pressureDamage           = pressureDamage;
            this.swimSpeedAmplifier       = swimSpeedAmplifier;
            this.underwaterHasteAmplifier = underwaterHasteAmplifier;
            this.effectDurationTicks      = effectDurationTicks;
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