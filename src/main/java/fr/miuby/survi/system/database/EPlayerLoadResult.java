package fr.miuby.survi.system.database;

/**
 * Résultat d'une tentative de rechargement d'un joueur depuis la table {@code player}.
 *
 * <p>Distingue explicitement "le joueur n'existe vraiment pas encore" de "la lecture a échoué" —
 * les deux ne doivent jamais être traités de la même façon. Confondre {@link #ERROR} avec
 * {@link #NOT_FOUND} amènerait à recréer un profil par défaut par-dessus un joueur existant.</p>
 */
public enum EPlayerLoadResult {
    /** Une ligne correspondante a été trouvée et le joueur a été réenregistré en mémoire avec ses vraies données. */
    FOUND,
    /** Aucune ligne ne correspond à cet UUID : il s'agit réellement d'un nouveau joueur. */
    NOT_FOUND,
    /** La lecture a échoué (BDD indisponible, ligne corrompue…) — état indéterminé, à ne jamais traiter comme NOT_FOUND. */
    ERROR
}