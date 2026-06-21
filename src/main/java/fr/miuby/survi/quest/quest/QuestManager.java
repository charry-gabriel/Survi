package fr.miuby.survi.quest.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.AbstractQuestManager;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.quest.QuestYamlLoader;
import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.world.zone.VillageZoneManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class QuestManager extends AbstractQuestManager<Quest> {

    /**
     * Quêtes journalières supplémentaires débloquées à chaque reset journalier réel
     * ({@code SurviConfig#getDailyResetHour()}, 6h par défaut).
     * La capacité totale d'un joueur = {@link #DAILY_QUEST_BONUS} × {@link #getQuestDayCount()}.
     */
    public static final int DAILY_QUEST_BONUS = 2;

    /**
     * Slots de quêtes journalières supplémentaires accordés manuellement à tout le monde via {@code /quest extraslot add}.
     * Persisté dans {@code server_data} (clé {@code quest_extra_global_slots}).
     */
    @Getter
    private int extraGlobalSlots = 0;

    /**
     * Capacité totale de quêtes journalières disponibles pour tous les joueurs au moment de l'appel.
     * Vaut 0 si la partie n'a pas encore démarré (avant le premier levelup de villageois).
     * Inclut les slots bonus accordés manuellement.
     */
    public int getTotalCapacity() {
        return getQuestDayCount() * DAILY_QUEST_BONUS + extraGlobalSlots;
    }

    /**
     * Numéro du "jour de quête" courant (1-based), calé sur le reset journalier réel du serveur
     * ({@code SurviConfig#getDailyResetHour()}, 6h par défaut) plutôt que sur des tranches de 24h
     * glissantes depuis le démarrage du village.
     *
     * <p>Le jour 1 démarre dès {@link VillageZoneManager#start()}. Le jour passe à 2 dès que l'horloge
     * réelle franchit le prochain {@code dailyResetHour:00} (c'est exactement l'instant où
     * {@code TimeManager} déclenche le {@code DailyResetEvent} → {@link #performDailyQuestReset()}),
     * puis +1 à chaque reset réel suivant. Le calcul est purement basé sur l'horloge (pas de compteur
     * persisté à part) : il reste correct même après un redémarrage serveur ou un reset manqué/rattrapé.</p>
     *
     * @return 0 si la partie n'a pas encore démarré, sinon le numéro du jour de quête courant.
     */
    private int getQuestDayCount() {
        VillageZoneManager zoneManager = GameManager.getInstance().getVillageZoneManager();
        if (!zoneManager.isStarted()) return 0;

        ZoneId zone = (GameManager.getInstance().getTimeManager() != null)
                ? GameManager.getInstance().getTimeManager().getServerTimezone()
                : ZoneId.systemDefault();
        int resetHour = SurviConfig.getInstance().getDailyResetHour();

        long startDayIndex = resetAlignedDayIndex(zoneManager.getStartTimestamp(), zone, resetHour);
        long nowDayIndex   = resetAlignedDayIndex(System.currentTimeMillis(), zone, resetHour);

        return (int) (nowDayIndex - startDayIndex) + 1;
    }

    /**
     * Index de jour "décalé" : l'horloge est reculée de {@code resetHour} heures avant de prendre
     * la date locale, si bien que cet index change exactement au moment où l'horloge réelle franchit
     * {@code resetHour:00} (au lieu de minuit). Permet de compter combien de resets de {@code resetHour}h
     * séparent deux instants par simple soustraction.
     */
    private static long resetAlignedDayIndex(long epochMillis, ZoneId zone, int resetHour) {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).minusHours(resetHour).toLocalDate().toEpochDay();
    }

    private final Random random = new Random();

    public QuestManager() {
        loadQuests();
        loadExtraSlots();
    }

    // =========================================================================
    // Pool — source YAML
    // =========================================================================

    @Override
    protected List<Quest> fetchPool() {
        return QuestYamlLoader.loadQuests();
    }

    // =========================================================================
    // Slots bonus globaux
    // =========================================================================

    private static final String EXTRA_SLOTS_KEY = "quest_extra_global_slots";

    private void loadExtraSlots() {
        String raw = GameManager.getInstance().getDatabase().system().getServerData(EXTRA_SLOTS_KEY);
        if (raw != null) {
            try { extraGlobalSlots = Integer.parseInt(raw); } catch (NumberFormatException ignored) {}
        }
    }

    /** Ajoute {@code amount} slots bonus globaux et les persiste. */
    public void addExtraSlots(int amount) {
        extraGlobalSlots += amount;
        GameManager.getInstance().getDatabase().system().saveExtraGlobalSlots(extraGlobalSlots);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST, "[ExtraSlots] +" + amount + " → total bonus=" + extraGlobalSlots);
    }

    /** Retire {@code amount} slots bonus globaux (minimum 0) et les persiste. */
    public void removeExtraSlots(int amount) {
        extraGlobalSlots = Math.max(0, extraGlobalSlots - amount);
        GameManager.getInstance().getDatabase().system().saveExtraGlobalSlots(extraGlobalSlots);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST, "[ExtraSlots] -" + amount + " → total bonus=" + extraGlobalSlots);
    }

    // =========================================================================
    // Reset journalier des slots (joueurs)
    // =========================================================================

    /**
     * Nettoie les quêtes {@code claimed} de la veille (annulation des effets de potion,
     * suppression de {@code player_quest}) et notifie chaque joueur connecté des nouveaux
     * créneaux disponibles. Ne touche pas au reset des mondes.
     *
     * <p>Appelé depuis {@code ServerListener.onDailyReset} (reset normal de 6h) et depuis
     * {@code /quest dailyreset} (rattrapage manuel si le reset de 6h n'a pas pu s'exécuter).</p>
     *
     * @return la capacité totale de quêtes journalières au moment de l'appel
     */
    public int performDailyQuestReset() {
        int capacity = getTotalCapacity();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "Reset journalier — annulation des effets des quêtes réclamées. Capacité cumulée : " + capacity + ".");

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();

        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {

            List<PlayerQuestData> claimedQuests = player.getActiveQuests().stream()
                    .filter(PlayerQuestData::isClaimed)
                    .toList();

            if (claimedQuests.isEmpty()) {
                if (player.getPlayer() != null && capacity > 0) notifyNewSlots(player, capacity);
                continue;
            }

            boolean isOnline = player.getPlayer() != null;

            if (isOnline) {
                for (PlayerQuestData data : claimedQuests) {
                    Quest quest = getQuest(data.getQuestId());
                    if (quest == null) continue;
                    for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effect.resetEffect(player);
                    }
                }
            }

            for (PlayerQuestData data : claimedQuests) {
                player.removeQuest(data.getSlot());
                GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
            }

            if (player.getActiveQuests().stream().noneMatch(q -> !q.isClaimed())) {
                GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
                if (glowService != null && isOnline) glowService.disableGlow(player);
            }

            if (isOnline) notifyNewSlots(player, capacity);
        }

        return capacity;
    }

    private void notifyNewSlots(AlphaPlayer player, int capacity) {
        int used      = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();
        int remaining = capacity - used;
        if (remaining <= 0) return;
        LangService ls = GameManager.getInstance().getLangService();
        player.getPlayer().sendMessage(ls.text(player.getPlayer(), "quest.new_slots", remaining, used, capacity));
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge le pool de quêtes à chaud, sans redémarrage.
     *
     * <h3>Comportement sur les quêtes en cours (joueurs connectés)</h3>
     *
     * <p>Le nouveau pool est d'abord chargé en mémoire temporaire avant de remplacer l'actuel,
     * de sorte que les définitions des quêtes orphelines sont encore accessibles pour
     * appliquer les récompenses éventuelles.
     *
     * <ul>
     *   <li><b>Quête dont l'id existe encore</b> — le joueur continue normalement.</li>
     *   <li><b>Quête terminée (completed) mais non réclamée, id supprimé</b> — les récompenses sont
     *       accordées immédiatement (blessing effects + réputation métier si le trader est encore
     *       disponible). La quête est marquée comme réclamée en DB.</li>
     *   <li><b>Quête en cours (non terminée), id supprimé</b> — la quête est supprimée et le slot
     *       est libéré : le joueur peut en accepter une nouvelle dès sa prochaine interaction avec
     *       un Trader. Un message d'information lui est envoyé.</li>
     * </ul>
     *
     * @return le nombre de quêtes présentes dans le pool après rechargement
     */
    public int reload() {
        // 1. Charger le nouveau pool sans encore remplacer l'actuel
        //    (le pool courant reste lisible pour traiter les quêtes orphelines)
        List<Quest> incoming = QuestYamlLoader.loadQuests();
        Set<String> incomingIds = incoming.stream().map(Quest::getId).collect(Collectors.toSet());

        // 2. Traiter les joueurs connectés qui ont une quête orpheline
        int rewardedCount = 0;
        int droppedCount  = 0;
        for (AlphaPlayer player : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            if (player.getPlayer() == null) continue;

            for (PlayerQuestData data : new ArrayList<>(player.getActiveQuests())) {
                if (incomingIds.contains(data.getQuestId())) continue; // quête encore valide — rien à faire

                Quest oldQuest = getQuest(data.getQuestId()); // pool encore intact à ce stade

                if (data.isCompleted() && !data.isClaimed() && oldQuest != null) {
                    // Le joueur avait terminé la quête avant le reload → récompense immédiate
                    applyOrphanRewards(player, data, oldQuest);
                    rewardedCount++;
                } else if (!data.isCompleted()) {
                    // Quête en cours supprimée → libérer le slot
                    player.removeQuest(data.getSlot());
                    GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), data.getSlot());
                    GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
                    player.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(player.getPlayer(), "quest.dropped_on_reload"));
                    droppedCount++;
                }
                // Cas data.isCompleted() && data.isClaimed() : déjà réclamée → rien à faire
            }
        }

        // 3. Remplacer le pool
        questPool.clear();
        questPool.addAll(incoming);

        if (rewardedCount > 0) MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] " + rewardedCount + " quête(s) terminée(s) récompensée(s) automatiquement (id supprimé du nouveau fichier).");
        if (droppedCount > 0) MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] " + droppedCount + " quête(s) en cours supprimée(s) — slots libérés.");

        return questPool.size();
    }

    /**
     * Applique les récompenses d'une quête orpheline directement, sans passer par un Trader.
     * Utilisé dans {@link #reload()} pour les quêtes terminées dont l'id a été supprimé du YAML.
     * La quête est marquée {@code claimed=true} pour que le reset de 6h gère les effets normalement.
     */
    private void applyOrphanRewards(AlphaPlayer player, PlayerQuestData data, Quest quest) {
        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            effect.applyEffect(player);
        }

        if (data.getTraderId() != null) {
            MLVillager villager = VillagerRegistry.get(data.getTraderId());
            if (villager instanceof Trader trader && trader.getJob() != null
                    && SurviConfig.getInstance().getQuestCompletionReputation() > 0) {
                player.addJobReputation(trader.getJob(), SurviConfig.getInstance().getQuestCompletionReputation());
            }
        }

        // Marque claimed=true — le reset de 6h se chargera de supprimer la quête et d'annuler les effets
        data.setClaimed(true);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        player.setTotalDailyQuestsClaimed(player.getTotalDailyQuestsClaimed() + 1);

        GameManager.getInstance().getDatabase().questHistory().insert(
                player.getUuid(),
                player.getPseudo(),
                quest.getId(),
                LocalDate.now(),
                quest.getDifficulty(),
                null,
                "daily",
                0
        );

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(GameManager.getInstance().getLangService()
                    .text(player.getPlayer(), "quest.orphan_rewarded", quest.getName()));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[Reload] Récompenses accordées à " + player.getPseudo() + " pour la quête orpheline : " + quest.getId());
    }

    // =========================================================================
    // Sélection
    // =========================================================================

    /**
     * Calcule la difficulté applicable pour un joueur auprès d'un trader donné, à partir
     * du niveau de métier du joueur pour le métier de ce trader :
     * <ul>
     *   <li>Difficulté 0 — niveau de métier 0 (aucune réputation).</li>
     *   <li>Difficulté 1 — niveau de métier 1 à 5.</li>
     *   <li>Difficulté 2 — niveau de métier 6 à 8.</li>
     *   <li>Difficulté 3 — niveau de métier 9 ou 10.</li>
     * </ul>
     * Si le trader n'a pas de métier, le niveau du monde sert de difficulté.
     */
    public int computeDifficulty(AlphaPlayer player, Trader trader) {
        if (trader.getJob() == null) return GameManager.getInstance().getWorldLevelManager().getLevel();

        int jobLevel = player.getJobLevel(trader.getJob());
        if (jobLevel <= 0) return 0;
        if (jobLevel <= 5) return 1;
        if (jobLevel <= 8) return 2;
        return 3;
    }

    /**
     * Indique si ce trader refuse de donner une quête car la difficulté débloquée par le
     * niveau de métier du joueur n'est pas encore accessible au niveau du monde actuel :
     * la difficulté 2 requiert un monde niveau 2 minimum, la difficulté 3 un niveau 3 minimum.
     */
    public boolean isLockedByWorldLevel(AlphaPlayer player, Trader trader) {
        int difficulty = computeDifficulty(player, trader);
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();
        return (difficulty == 2 && worldLevel < 2) || (difficulty == 3 && worldLevel < 3);
    }

    /**
     * Indique si ce trader a au moins une quête disponible dans le pool
     * pour le métier et la difficulté donnés.
     * Si {@code job} est null, toutes les quêtes de la bonne difficulté sont éligibles.
     */
    public boolean hasAvailableQuestFor(EJob job, int difficulty) {
        return questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .anyMatch(q -> job == null || q.getJobs().contains(job));
    }

    /**
     * Retourne une quête aléatoire du niveau de difficulté donné en évitant de répéter
     * la dernière quête attribuée au joueur (persistée en DB, résiste aux restarts).
     */
    public Quest getRandomQuest(int difficulty, UUID playerUuid) {
        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .toList();
        if (filtered.isEmpty()) return null;
        if (filtered.size() == 1) return filtered.getFirst();

        String lastId = GameManager.getInstance().getDatabase().quests().getLastQuestId(playerUuid);
        List<Quest> candidates = filtered.stream()
                .filter(q -> !q.getId().equals(lastId))
                .toList();

        return candidates.isEmpty()
                ? filtered.get(random.nextInt(filtered.size()))
                : candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Variante avec filtre métier. Si aucune quête ne correspond au métier,
     * retourne null (pas de fallback sur toutes les difficultés).
     *
     * @param job métier pour filtrer (null = pas de filtre métier)
     */
    public Quest getRandomQuest(int difficulty, EJob job, UUID playerUuid) {
        if (job == null) return getRandomQuest(difficulty, playerUuid);

        List<Quest> filtered = questPool.stream()
                .filter(q -> q.getDifficulty() == difficulty)
                .filter(q -> q.getJobs().contains(job))
                .toList();

        if (filtered.isEmpty()) return null;
        if (filtered.size() == 1) return filtered.getFirst();

        String lastId = GameManager.getInstance().getDatabase().quests().getLastQuestId(playerUuid);
        List<Quest> candidates = filtered.stream()
                .filter(q -> !q.getId().equals(lastId))
                .toList();

        return candidates.isEmpty()
                ? filtered.get(random.nextInt(filtered.size()))
                : candidates.get(random.nextInt(candidates.size()));
    }

    // =========================================================================
    // État joueur
    // =========================================================================

    /**
     * Appelé à la connexion d'un joueur : restaure ses quêtes depuis la DB.
     *
     * <h3>Règles de traitement</h3>
     * <ul>
     *   <li><b>Quête réclamée, datée d'aujourd'hui</b> — le joueur s'est reconnecté avant le reset de 6h ;
     *       la quête est gardée en mémoire et ses effets de potion sont réappliqués.</li>
     *   <li><b>Quête réclamée, datée d'un jour précédent</b> — le joueur était hors-ligne lors du reset ;
     *       la quête est supprimée de player_quest (le reset ne l'a pas nettoyée) et ses effets ne
     *       sont PAS réappliqués.</li>
     *   <li><b>Quête non réclamée (en cours ou terminée-non-validée)</b> — conservée quel que soit son
     *       âge (système cumulatif : le joueur peut la continuer le lendemain). Le glow du Trader cible
     *       est activé si la quête est terminée.</li>
     * </ul>
     */
    public void restoreQuestsOnJoin(AlphaPlayer player, List<PlayerQuestData> loaded) {
        player.getActiveQuests().clear();

        LocalDate today = LocalDate.now();

        List<BlessingEffect> effectsToReapply = new ArrayList<>();

        for (PlayerQuestData q : loaded) {
            if (q.isClaimed()) {
                if (today.isEqual(q.getLastAccepted())) {
                    // Reconnexion avant le reset de 6h — garder en mémoire et réappliquer les effets
                    player.getActiveQuests().add(q);
                    Quest questDef = getQuest(q.getQuestId());
                    if (questDef != null) {
                        for (BlessingEffect effect : questDef.getRewards().blessingEffects()) {
                            if (effect instanceof PotionsEffect) effectsToReapply.add(effect);
                        }
                    }
                } else {
                    // Joueur hors-ligne lors du reset — nettoyer le résidu en DB, pas d'effet
                    GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), q.getSlot());
                    MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                            "Résidu post-reset (slot " + q.getSlot() + ") nettoyé pour " + player.getPseudo());
                }
            } else {
                // Quête non réclamée : toujours conservée (système cumulatif)
                player.getActiveQuests().add(q);
            }
        }

        if (!effectsToReapply.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                if (player.getPlayer() == null || !player.getPlayer().isOnline()) return;
                for (BlessingEffect effect : effectsToReapply) effect.applyEffect(player);
            }, 5L);
        }

        // Active le glow du Trader cible si le joueur a une quête terminée non réclamée
        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) {
            for (PlayerQuestData q : player.getActiveQuests()) {
                if (q.isCompleted() && !q.isClaimed() && q.getTraderId() != null) {
                    glowService.enableGlow(player, q.getTraderId());
                    break;
                }
            }
        }

        // Relance l'ActionBar de la quête en cours (non terminée) : stopRefresh a été appelé
        // au quit, donc sans cela le joueur n'a plus aucune indication de sa quête active.
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null && !current.isCompleted()) {
            Quest quest = getQuest(current.getQuestId());
            if (quest != null) {
                GameManager.getInstance().getQuestActionBarService().showProgress(player, quest, current);
            }
        }
    }

    // =========================================================================
    // Attribution
    // =========================================================================

    /**
     * Reset admin : supprime la quête en cours et retire les effets de potion
     * si elle était déjà complétée. Libère un slot pour une nouvelle quête.
     */
    public boolean resetQuest(AlphaPlayer player) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current == null) return false;

        if (current.isCompleted() && player.getPlayer() != null) {
            Quest quest = getQuest(current.getQuestId());
            if (quest != null) {
                for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                    if (effect instanceof PotionsEffect) effect.resetEffect(player);
                }
            }
        }

        player.removeQuest(current.getSlot());
        GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(player);
        GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(player.getPlayer(), "quest.reset_by_admin"));
        }
        return true;
    }

    public void assignQuest(AlphaPlayer player, Trader trader) {
        assignQuest(player, trader, false);
    }

    /**
     * Attribue une nouvelle quête au joueur si la capacité cumulative n'est pas atteinte.
     */
    public void assignQuest(AlphaPlayer player, Trader trader, boolean force) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        LangService langService = GameManager.getInstance().getLangService();
        if (current != null) {
            if (current.isCompleted()) {
                player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.already_claimed"));
            } else {
                player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.already_active"));
            }
            return;
        }

        int capacity  = getTotalCapacity();
        int usedSlots = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();

        if (capacity == 0) {
            player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.not_available"));
            return;
        }

        if (!force && usedSlots >= capacity) {
            player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.capacity_full", usedSlots, capacity));
            return;
        }

        if (isLockedByWorldLevel(player, trader)) {
            player.getPlayer().sendMessage(Component.text("<", NamedTextColor.AQUA)
                    .append(trader.getDisplayName())
                    .append(Component.text("> ", NamedTextColor.AQUA))
                    .append(langService.text(player.getPlayer(), "quest.locked_world_level")));
            return;
        }

        int difficulty = computeDifficulty(player, trader);
        Quest quest = getRandomQuest(difficulty, trader.getJob(), player.getUuid());
        if (quest == null) return;

        int nextSlot = usedSlots; // slot global unique, croissant
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, trader.getNameId(), false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[AssignQuest] " + player.getPseudo() + " → " + quest.getId() + " (diff=" + difficulty + ") slot=" + nextSlot + " [" + (usedSlots + 1) + "/" + capacity + "]");

        player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.accepted",
                Placeholder.component("trader", trader.getDisplayName()),
                Placeholder.unparsed("quest", quest.getName())));
        player.getPlayer().sendMessage(Component.text(quest.getFormattedDescription(), NamedTextColor.GRAY));
        player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.accepted_slots", usedSlots + 1, capacity));
    }

    /**
     * Attribue une quête spécifique au joueur (mode test admin).
     */
    public void assignSpecificQuest(AlphaPlayer player, Quest quest) {
        PlayerQuestData current = player.getCurrentActiveQuest();
        if (current != null) {
            player.removeQuest(current.getSlot());
            GameManager.getInstance().getDatabase().quests().deletePlayerQuestSlot(player.getUuid(), current.getSlot());
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService != null) glowService.disableGlow(player);
            GameManager.getInstance().getQuestActionBarService().stopRefresh(player.getUuid());
        }

        int nextSlot = player.getTotalDailyQuestsClaimed() + player.countActiveUnclaimedQuests();
        PlayerQuestData data = new PlayerQuestData(nextSlot, quest.getId(), 0, LocalDate.now(), false, null, false);
        player.putQuest(data);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
        GameManager.getInstance().getDatabase().quests().setLastQuestId(player.getUuid(), quest.getId());

        if (player.getPlayer() != null) {
            String jobsStr = quest.getJobs().isEmpty() ? "tous" : quest.getJobs().stream().map(EJob::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("tous");
            LangService langService = GameManager.getInstance().getLangService();
            player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.test.assigned", quest.getName()));
            player.getPlayer().sendMessage(Component.text(quest.getFormattedDescription(), NamedTextColor.GRAY));
            player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.test.details", quest.getGoal(), quest.getDifficulty(), jobsStr));
        }
    }

    // =========================================================================
    // Progression
    // =========================================================================

    /**
     * Réclame la récompense de la quête complétée auprès du bon Trader.
     *
     * <p>La quête est marquée {@code claimed=true} dans {@code player_quest} et conservée jusqu'au
     * prochain reset de 6h — cela permet à {@code onDailyReset} de retrouver les effets de potion
     * à annuler. Elle est également enregistrée dans {@code quest_history} et le compteur en mémoire
     * {@link AlphaPlayer#getTotalDailyQuestsClaimed()} est incrémenté immédiatement.</p>
     */
    public boolean claimQuest(AlphaPlayer player, Trader trader, boolean force) {
        PlayerQuestData data = player.getCurrentActiveQuest();

        if (data == null || (!force && !data.isCompleted()) || data.isClaimed())
            return false;

        if (data.getTraderId() != null && !data.getTraderId().equals(trader.getNameId())) {
            MLVillager target = VillagerRegistry.get(data.getTraderId());
            Component traderName = (target != null) ? target.getDisplayName() : Component.text(data.getTraderId(), NamedTextColor.AQUA);
            player.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(player.getPlayer(), "quest.validate_trader",
                    Placeholder.component("trader", traderName)));
            return false;
        }

        Quest quest = getQuest(data.getQuestId());
        if (quest == null) return false;

        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            effect.applyEffect(player);
        }
        if (trader.getJob() != null && SurviConfig.getInstance().getQuestCompletionReputation() > 0) {
            player.addJobReputation(trader.getJob(), SurviConfig.getInstance().getQuestCompletionReputation());
        }

        // Marque claimed=true en DB — la quête reste dans player_quest jusqu'au reset de 6h
        // (nécessaire pour que onDailyReset puisse annuler les effets de potion)
        data.setClaimed(true);
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);

        // Incrémente le compteur cumulatif en mémoire et insère dans l'historique
        int usedAfter = player.getTotalDailyQuestsClaimed() + 1;
        player.setTotalDailyQuestsClaimed(usedAfter);

        GameManager.getInstance().getDatabase().questHistory().insert(
                player.getUuid(),
                player.getPseudo(),
                quest.getId(),
                LocalDate.now(),
                quest.getDifficulty(),
                trader.getJob() != null ? trader.getJob().name() : null,
                "daily",
                0
        );

        int capacity = getTotalCapacity();
        LangService langService = GameManager.getInstance().getLangService();
        player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.rewards_received", usedAfter, capacity));

        QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
        if (glowService != null) glowService.disableGlow(player);

        return true;
    }

    /**
     * Met à jour la progression d'une quête CRAFT en comptant les items correspondants
     * dans l'inventaire du joueur. Appelé avec 1 tick de délai après {@code CraftItemEvent}
     * pour que l'item soit déjà dans l'inventaire au moment du scan.
     *
     * <p>La progression est uniquement avancée (jamais réduite) : si le joueur consomme
     * des items après avoir atteint un certain seuil, la progression reste acquise.</p>
     */
    public void syncCraftProgress(AlphaPlayer player, Material material) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null || data.isCompleted()) return;

        Quest quest = getQuest(data.getQuestId());
        if (quest == null || !quest.matchesAction(EQuestType.CRAFT, material)) return;

        int count = 0;
        for (ItemStack item : player.getPlayer().getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        int newProgress = Math.min(count, quest.getGoal());
        if (newProgress <= data.getProgress()) return;

        data.setProgress(newProgress);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[CraftProgress] " + player.getPseudo() + " — " + data.getQuestId() + " : " + data.getProgress() + "/" + quest.getGoal());

        if (data.getProgress() >= quest.getGoal()) {
            finishQuest(player, quest);
        } else {
            GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
            GameManager.getInstance().getQuestActionBarService().showProgress(player, quest, data);
        }
    }

    /**
     * Incrémente la progression de la quête active en cours (non complétée).
     */
    public void progressQuest(AlphaPlayer player, EQuestType type, Object target, int amount) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null || data.isCompleted()) return;

        Quest quest = getQuest(data.getQuestId());
        if (quest == null || !quest.matchesAction(type, target)) return;

        data.setProgress(data.getProgress() + amount);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                "[QuestProgress] " + player.getPseudo() + " — " + data.getQuestId() + " : " + data.getProgress() + "/" + quest.getGoal());

        if (data.getProgress() >= quest.getGoal()) {
            finishQuest(player, quest);
        } else {
            GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);
            GameManager.getInstance().getQuestActionBarService().showProgress(player, quest, data);
        }
    }

    private void finishQuest(AlphaPlayer player, Quest quest) {
        PlayerQuestData data = player.getCurrentActiveQuest();
        if (data == null) return;

        data.setProgress(quest.getGoal());
        data.setCompleted(true);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[QuestFinished] " + player.getPseudo() + " — " + quest.getId());
        GameManager.getInstance().getDatabase().quests().updatePlayerQuest(player.getUuid(), data);

        SoundService.play(player.getPlayer(), ESound.QUEST_COMPLETE);
        LangService langService = GameManager.getInstance().getLangService();
        player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.completed", quest.getName()));
        player.getPlayer().sendMessage(langService.text(player.getPlayer(), "quest.completed_go_trader"));
        GameManager.getInstance().getQuestActionBarService().showFinished(player, quest);

        // Active le glow du Trader cible pour guider le joueur
        if (data.getTraderId() != null) {
            QuestGlowService glowService = GameManager.getInstance().getQuestGlowService();
            if (glowService != null) glowService.enableGlow(player, data.getTraderId());
        }
    }
}