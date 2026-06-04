package fr.miuby.survi.system.perf;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

/**
 * Chronomètre inline toggleable, <strong>zéro overhead quand désactivé</strong>.
 *
 * <h3>Principe</h3>
 * <ul>
 *   <li>Quand {@code enabled = false} (défaut prod), {@link #start(String)} retourne
 *       le singleton {@link #NOOP} — aucune allocation, aucun overhead visible au JIT.</li>
 *   <li>Quand {@code enabled = true}, crée un objet léger, mesure via
 *       {@link System#nanoTime()} et logue en {@code WARNING/PERF}
 *       si le temps dépasse {@link #THRESHOLD_NS}.</li>
 * </ul>
 *
 * <h3>Utilisation (n'importe quel hot path)</h3>
 * <pre>
 *   try (var t = PerfTimer.start("DamageListener.onEntityDamage")) {
 *       // code à mesurer
 *   }   // log automatique si > 0,5 ms
 * </pre>
 *
 * <h3>Activation / désactivation à chaud</h3>
 * <pre>
 *   /survi perf on      → active les timers
 *   /survi perf off     → désactive (overhead nul immédiatement)
 *   /survi perf status  → affiche l'état actuel
 * </pre>
 */
public final class PerfTimer implements AutoCloseable {

    // ─── Singleton NO_OP ─────────────────────────────────────────────────────────

    /**
     * Instance réutilisée quand les timers sont désactivés.
     * close() est un no-op : label == null.
     */
    private static final PerfTimer NOOP = new PerfTimer(null, 0L);

    // ─── Configuration ───────────────────────────────────────────────────────────

    /**
     * Seuil minimal pour qu'une mesure apparaisse dans les logs.
     * 500 000 ns = 0,5 ms : en-dessous, on suppose que le code est acceptable.
     */
    private static final long THRESHOLD_NS = 500_000L;

    /**
     * Flag volatile : chaque appel à {@link #start(String)} lit cette valeur.
     * Un volatile read coûte ~1 ns — négligeable même dans onPlayerMove.
     * -- SETTER --
     *  Active ou désactive la collecte. Prise d'effet immédiate sur tous les threads.
     *  Utiliser via
     * .
     * -- GETTER --
     *  si les métriques sont actuellement collectées.
     */
    @Getter
    @Setter
    private static volatile boolean enabled = false;

    // ─── État de l'instance ──────────────────────────────────────────────────────

    private final String label;
    private final long   startNs;

    private PerfTimer(String label, long startNs) {
        this.label   = label;
        this.startNs = startNs;
    }

    // ─── API publique ─────────────────────────────────────────────────────────────

    /**
     * Démarre un chronomètre si activé ; retourne {@link #NOOP} sinon.
     *
     * <p>Zéro allocation quand désactivé — utilisable sans garde {@code if} dans
     * tous les hot paths (move, damage, blockBreak…).</p>
     *
     * @param label identifiant lisible du point de mesure, affiché dans le log
     */
    public static PerfTimer start(String label) {
        if (!enabled) return NOOP;
        return new PerfTimer(label, System.nanoTime());
    }

    /**
     * Ferme le chronomètre (appelé automatiquement par try-with-resources).
     * Logue en {@code WARNING/PERF} si le temps dépasse {@link #THRESHOLD_NS}.
     */
    @Override
    public void close() {
        if (label == null) return;              // chemin NO_OP : rien à faire
        long elapsed = System.nanoTime() - startNs;
        if (elapsed >= THRESHOLD_NS) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.PERF,
                    String.format("[⏱] %-48s %,7d µs", label, elapsed / 1_000L));
        }
    }
}