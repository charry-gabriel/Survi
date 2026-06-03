package fr.miuby.survi.quest;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestion générique d'un pool de quêtes : stockage, accès par id et rechargement.
 * Partagé entre {@link QuestManager} (quêtes journalières attribuées par Trader)
 * et {@link GlobalQuestManager} (quêtes d'événement lancées par un admin).
 *
 * <p>Chaque sous-classe fournit {@link #fetchPool()} qui lit sa source YAML
 * via {@link QuestYamlLoader}. Tout le reste (liste, getter, lookup, reload)
 * est hérité.</p>
 */
@Getter
public abstract class AbstractQuestManager<T extends BaseQuest> {

    protected final List<T> questPool = new ArrayList<>();

    /** Charge et retourne la liste fraîche depuis la source de données (YAML). */
    protected abstract List<T> fetchPool();

    /** Recharge le pool depuis {@link #fetchPool()}. Appelé au constructeur et sur reload. */
    protected void loadQuests() {
        questPool.clear();
        questPool.addAll(fetchPool());
    }

    /** Recherche une quête par id dans le pool courant. */
    public T getQuest(String id) {
        return questPool.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}