# AI_DEV.md — Guide de développement Survi pour Claude

Ce fichier est destiné à Claude pour générer du code cohérent avec ce projet.

---

## Sommaire

| Section | Quand la consulter |
|---|---|
| ⚠️ **RÈGLES CRITIQUES** | Toujours — avant chaque tâche |
| 0. Instructions | Conventions de rédaction pour Claude |
| 1. Contexte technique | Stack, versions, compatibilité Paper 26.1 |
| 3. GameManager | Accéder à un service / manager |
| 4. Conventions de nommage | Nommer une nouvelle classe |
| 5. Règles de dépendance | Ce qui peut appeler quoi |
| 6. Commandes Brigadier | Ajouter ou modifier une commande |
| 7. Textes | Envoyer un message à un joueur |
| 8. Base de données | Repositories, migrations, async |
| 9. Chargement YAML | Lire ou déployer un fichier de config |
| 10. Logging | Logger un événement |
| 11. Mondes | EWorld, convention de nommage des dimensions |
| 12. Scheduler | Tâches différées ou async |
| 13. Blessings | Ajouter ou modifier un effet villageois |
| 13b. Growth items | Ajouter ou modifier un growth item |
| 14. Schémas JSON | Table fichier YAML → schéma |
| 15. Ne jamais faire | Liste rouge — à garder en tête |
| 16. Fichiers clés | Quels fichiers lire avant de toucher un sous-système |
| 17. PerfTimer | Instrumenter un hot path |
| 18. Checklist villageois | Ajouter un villageois |
| 19. Checklist feature | Ajouter une feature gameplay |
| 20. Quêtes ActionBar/BossBar | Affichage progression quêtes |
| **Annexe MiubyLib** | API complète — consulter si besoin |

---

## ⚠️ RÈGLES CRITIQUES — lire avant tout

> Ces règles s'appliquent **à chaque tâche**, sans exception.

### ❌ Pas d'explications

Ne jamais écrire de résumé, d'explication, de "voici ce que j'ai fait", de liste de modifications. Fournir les fichiers directement — c'est suffisant. L'utilisateur lit le code.

### ❓ Poser une question si quelque chose est ambigu

Si un comportement attendu est flou AskUserQuestion. Ne pas supposer et livrer du code basé sur une mauvaise hypothèse.

### 🔴 SCHEMA + TEST — ne jamais oublier

**Toute modification de YAML, d'enum ou d'effet impose trois mises à jour simultanées :**

1. **Le fichier YAML** lui-même
2. **Le schéma JSON** (`schema/*.json`)
3. **Le test de config associé** (`*ConfigTest.java`)

Et si la modification touche un **enum du projet** (`EJob`, `ERole`, `EWorld`, `EQuestType`, `ECustomItem`, `ELockedToolType`, `ELockedArmorType`) ou un **enum Minecraft** (`Material`, `EntityType`, `PotionEffectType`) ou les **types d'effets** d'un système (blessings, growth items, récompenses de quêtes...) :

**→ Lancer `SchemaGeneratorTest.updateSchemas()` ou indiquer explicitement qu'il doit être relancé.**

`SchemaGeneratorTest` relit les enums depuis les sources Java et les YAML pour regénérer les listes dans tous les schémas. Ne jamais écrire ces listes à la main.

| Fichier YAML | Schéma | Test |
|---|---|---|
| `quests/*.yml` | `schema/quests-schema.json` | `QuestConfigTest` |
| `monsters.yml` | `schema/monsters-schema.json` | `MonstersConfigTest` |
| `roles.yml` | `schema/roles-schema.json` | `RolesConfigTest` |
| `villagers/*.yml` | `schema/villagers-schema.json` | `VillagerConfigTest` |
| `traders/*.yml` | `schema/traders-schema.json` | `TraderConfigTest` |
| `recipes.yml` | `schema/recipes-schema.json` | `RecipesConfigTest` |
| `global_quests.yml` | `schema/global-quests-schema.json` | `GlobalQuestConfigTest` |
| `growth_items/*.yml` | `schema/growth-items-schema.json` | `GrowthItemConfigTest` |
| `jobs/miner.yml` | `schema/jobs/miner-schema.json` | `JobsConfigTest` |
| `jobs/lumberjack.yml` | `schema/jobs/lumberjack-schema.json` | `JobsConfigTest` |
| `jobs/farmer.yml` | `schema/jobs/farmer-schema.json` | `JobsConfigTest` |
| `jobs/enchanter.yml` | `schema/jobs/enchanter-schema.json` | `JobsConfigTest` |
| `jobs/fisherman.yml` | `schema/jobs/fisherman-schema.json` | `JobsConfigTest` — ⚠️ `SchemaGeneratorTest` requis (enum `Material`) |
| `jobs/explorer.yml` | `schema/jobs/explorer-schema.json` | `JobsConfigTest` |

---

## 0. Instructions pour Claude

- Mettre à jour `AI_DEV.md` si une modification touche grandement un système existant (nouveau sous-système, changement de convention, nouveau pattern d'architecture).
- **Longueur de ligne** : ne pas couper les lignes de code artificiellement. L'écran est large — une ligne peut facilement dépasser 150 caractères sans problème. Ne couper que si la ligne est vraiment trop longue pour être lisible d'un bloc (180+ caractères), pas par convention "80 colonnes".
- **Commentaires** : uniquement si la logique est non évidente. Jamais de commentaire narratif : pas d'historique de bug, pas d'explication de pourquoi on n'a pas fait autrement, pas de "On ne fait plus X ici car...". Un commentaire décrit ce que fait le code, pas son contexte de modification.

---

## 1. Contexte technique

- **Plateforme** : Paper `api-version: 26.1` (version 1.21.x, notation changée en 2026)
- **Package racine** : `fr.miuby.survi`
- **Bibliothèque interne** : `fr.miuby.lib` (MiubyLib — shadowjarisée dans `fr.miuby.survi.shaded.lib` à la compilation — voir **Annexe MiubyLib** en fin de fichier)
- **Build** : Lombok est disponible (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, etc.)
- **DB** : SQLite via repositories
- **Java** : 25
- **Lombok** : 1.18.38
- **Paper API** : `26.1.2.build.66-stable`

### ⚠️ Compatibilité Paper 26.1

La structure des dimensions a changé. Les noms de dossiers/mondes suivent désormais
la convention `<worldName>_nether` et `<worldName>_the_end`. Voir `WorldInitializer` pour la convention exacte du projet.

Essayer de ne pas générer de code qui utilise des APIs marquées `@Deprecated` dans Paper 26.1.

---

## 3. Point d'entrée — GameManager

Singleton. Tout accès aux services passe par là :

```java
GameManager gm = GameManager.getInstance();

gm.getPlugin()                  // JavaPlugin
gm.getScheduler()               // BukkitScheduler
gm.getDatabase()                // Database (SQLite)
gm.getAlphaPlayerFactory()      // joueurs
gm.getVillagerFactory()         // villageois et traders
gm.getQuestManager()            // quêtes
gm.getRoleLoader()              // rôles
gm.getMobLevelManager()         // niveaux des mobs
gm.getWorldLevelManager()       // niveaux des mondes
gm.getWorldPortalManager()      // portails
gm.getWorldResetManager()       // reset des mondes
gm.getVillageZoneManager()      // zones de village
gm.getPlantedCropsManager()     // cultures
gm.getGraveManager()            // tombes
gm.getTabDisplayManager()       // tab list
gm.getTimeManager()             // temps réel ↔ Minecraft
gm.getRoleManagementService()   // attribution des rôles
```

```java
AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId()); // shortcut statique
// Lance AlphaPlayerNotFoundException si introuvable (ne retourne jamais null)
```

---

## 4. Conventions de nommage — obligatoires

| Pattern | Usage | Exemple |
|---|---|---|
| Préfixe `E` | Enum "type" utilisé comme clé | `ERole`, `EJob`, `EWorld` |
| `Manager` | État global + lifecycle (load/stop) | `QuestManager`, `MobLevelManager` |
| `Factory` | Crée et indexe des entités (registry) | `AlphaPlayerFactory`, `VillagerFactory` |
| `Service` | Logique métier sans état propre | `RoleManagementService` |
| `Repository` | Accès SQL — une table ≈ un repository | `PlayerRepository`, `QuestRepository` |
| `Listener` | Événements Bukkit — délèguent aux services | `PlayerListener`, `JobListener` |
| `Command` | Brigadier — **classe utilitaire stateless** | `RoleCommand`, `QuestCommand` |
| `Config` | POJO chargé depuis YAML | `MobTypeConfig`, `VillagerLevelConfig` |
| `Loader` | Lit un fichier YAML et retourne des objets | `RoleLoader`, `BlessingLoader` |

---

## 5. Règles de dépendance

```
Listener   → Service / Manager      ✓
Service    → Repository             ✓
Command    → Service / Manager      ✓
AlphaPlayer → GameManager          ✓  (inévitable en Minecraft)
AlphaPlayer → VillagerFactory      ✗  (couplage circulaire — interdit)
```

---

## 6. Commandes Brigadier

API Paper : `io.papermc.paper.command.brigadier`.

```java
public class MonCommand {
    private MonCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("macommande")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.literal("sub")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            return Command.SINGLE_SUCCESS;
                        }));
    }
}
```

Enregistrement dans `Survi.onEnable()` :

```java
getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
    commands.registrar().register(MonCommand.createCommand().build());
});
```

**Erreurs** : utiliser `CommandErrors` (PLAYER_NOT_FOUND, VILLAGER_NOT_FOUND, ROLE_NOT_FOUND,
QUEST_NOT_FOUND, CUSTOM_ITEM_NOT_FOUND, WORLD_NOT_FOUND, JOB_NOT_FOUND, NOT_A_LEVEL_VILLAGER).

**Arguments custom disponibles** dans `system/command/argument/` — tous étendent `MLStringArgument<T>` :
`AlphaPlayerArgument`, `VillagerArgument`, `TraderArgument`, `RoleArgument`, `SubRoleArgument`,
`JobArgument`, `QuestArgument`, `GlobalQuestArgument`, `CustomItemArgument`, `WorldArgument`.

---

## 7. Textes — Adventure + traductions

### Adventure uniquement

```java
Component.text("Message", NamedTextColor.GREEN)
player.sendMessage(Component.text("...").color(NamedTextColor.RED))

// Interdit — jamais
"§aMessage"
ChatColor.GREEN + "Message"
player.sendMessage("texte brut")
```

### Texte visible par un joueur → `LangService`

`GameManager.getInstance().getLangService()` — façade fine sur `MLMessageService`
(MiubyLib, mono-FR — voir **Annexe MiubyLib**). Les clés vivent dans
`src/main/resources/lang/fr.yml` (YAML + MiniMessage). Survi est mono-langue :
`ELang` n'a qu'une seule valeur (`FR`), conservée uniquement pour les signatures
existantes de `LangService`.

```java
LangService ls = GameManager.getInstance().getLangService();

// Simple
player.sendMessage(ls.text(player, "world.locked"));

// Placeholders positionnels {0}, {1}… (texte échappé pour MiniMessage)
player.sendMessage(ls.text(player, "grave.created", x, y, z, worldName));

// TagResolver — <name> remplacé par un Component (couleurs, etc.)
player.sendMessage(ls.text(player, "job.level_up.broadcast",
        Placeholder.unparsed("player", pseudo),
        Placeholder.component("job", job.toComponent())
));

// Tous les joueurs en ligne
ls.broadcast("world.level_up.broadcast", oldLevel, newLevel);

// CommandSender (console incluse)
ELang lang = ls.resolveOrDefault(sender);
sender.sendMessage(ls.text(lang, "cmd.role.assigned", roleName));
```

**Nouvelle clé** → l'ajouter dans `lang/fr.yml` uniquement (format `{0}`, `{1}`…
ou `<name>`). Clé absente → le joueur voit "⚠ Traduction manquante (clé)" et un
warning est loggé une seule fois côté serveur (tag `MESSAGE`).

---

## 8. Base de données

Hiérarchie : `SQLite extends Database extends MLSQLite` — voir **Annexe MiubyLib** pour `MLSQLite` et `MLRepository`.

```java
GameManager.getInstance().getDatabase().players()       // PlayerRepository
GameManager.getInstance().getDatabase().villagers()     // VillagerRepository
GameManager.getInstance().getDatabase().quests()        // QuestRepository
GameManager.getInstance().getDatabase().crops()         // CropRepository
GameManager.getInstance().getDatabase().system()        // SystemRepository (logs, serverData)
GameManager.getInstance().getDatabase().graves()        // GraveRepository
GameManager.getInstance().getDatabase().questHistory()  // QuestHistoryRepository
```

Le SQL va **uniquement** dans les repositories. Jamais inline dans un Listener ou Command.

**Tous les repositories étendent `MLRepository`** — utiliser `runAsync(conn -> { ... }, ELogTag.XXX, "message")`.

**Debug SQL admin** : `GameManager.getInstance().getDatabase().executeRaw(sql)`. Ne pas appeler depuis du code métier.

Pour ajouter une migration : incrémenter `CURRENT_DB_VERSION` dans `SQLite`, ajouter `createXxxTable()` dans `createTables()`, et `if (currentVersion < N) { ... }` dans `runMigrations()`. Ne jamais appeler `setVersion()` manuellement.

---

## 9. Chargement YAML

```java
File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), "monfichier.yml");
YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
```

Les fichiers sont déployés via `MLResourceManager.deploy()` / `deployFolder()` dans `Survi.onEnable()`. Ne pas appeler `saveResource()` directement.

Les quêtes journalières suivent le même pattern folder que `growth_items/` : chaque fichier `quests/<type>.yml` est chargé indépendamment par `QuestYamlLoader` puis fusionné. Ajouter un nouveau fichier dans `quests/` suffit — aucune modification Java requise.

### Modifier un fichier YAML — workflow obligatoire

→ Voir **⚠️ RÈGLES CRITIQUES** en début de fichier : toute modification impose la mise à jour du schéma JSON, du test de config associé, et le relancement de `SchemaGeneratorTest.updateSchemas()` si un enum est touché.

---

## 10. Logging

```java
MLLogManager.getInstance().log(Level.INFO,    ELogTag.QUEST,    "Message");
MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER, "Message", exception);
```

Jamais `System.out.println` ni `plugin.getLogger().info(...)`. Voir **Annexe MiubyLib** pour la configuration complète de `MLLogManager`.

Tags disponibles : `PLAYER`, `VILLAGER`, `QUEST`, `REPUTATION`, `ITEM`, `ROLE`, `JOB`, `WORLD`, `SYSTEM`, `GRAVE`, `PERF`.

`PERF` est réservé à `PerfTimer` (voir section 17) — ne pas l'utiliser pour du logging métier.

---

## 11. Mondes

`EWorld` : `VILLAGE`, `WILDERNESS`, `NETHER`, `END`, `ALL`

`ALL` = valeur spéciale "tous les mondes", à traiter selon contexte.

Convention de nommage (Paper 26.1) :
- Wilderness : `Wilderness_N` (N = numéro de reset, stocké en DB)
- Nether associé : `Wilderness_N_nether`
- End associé : `Wilderness_N_the_end`
- Village : `Village` (permanent)

```java
WorldRegistry.get(EWorld.VILLAGE)        // MLWorld
WorldRegistry.get(player)               // MLWorld du joueur
WorldInitializer.loadOrCreate(name, env) // jamais null (lance exception si échec)
```

---

## 12. Scheduler

```java
scheduler.runTaskLater(plugin, () -> { }, 20L);      // 1 seconde
scheduler.runTaskTimer(plugin, () -> { }, 0L, 20L);  // répété chaque seconde
// Préférer MiubyLib dans les nouveaux managers (voir Annexe MiubyLib) :
MiubyLib.runLater(task, delay);
MiubyLib.runAsync(task);
```

Accès Bukkit = **thread principal uniquement**. DB = async via `MLRepository.runAsync`.

---

## 13. Blessings (effets villageois)

```java
public class MonEffet extends BlessingEffect {
    @Override public void applyEffect(VillagerLevel villager, AlphaPlayer player) { ... }
    @Override public void resetEffect(VillagerLevel villager, AlphaPlayer player) { ... }
    // resetEffect : no-op pour les effets one-shot, implémenter pour unlock/maxHealth/etc.
}
```

Enregistrer dans `BlessingLoader` via la map `type → BlessingEffect`.

**Réapplication à la connexion** : `PlayerEffectRestoreService.restoreOnJoin(player)` réapplique tous les effets de blessings actifs et envoie un récapitulatif "État actif". C'est le point central pour tout effet persistant à rétablir à la reconnexion — pas `VillagerFactory`.

**Notifications offline** : `OfflineNotificationService` accumule les `VillagerLevelUpEvent`, `AlphaPlayerJobLevelUpEvent` et `WorldLevelUpEvent` survenus pendant l'absence d'un joueur, et les délivre groupés au join (priority HIGH, après l'application des effets). L'écoute des events est confiée à `OfflineNotificationListener` (dans `listener/`) qui délègue au service — le service n'implémente pas `Listener`.

---

## 13b. Growth items

Configuration **entièrement en YAML** — aucun code Java pour créer un nouvel item.

```
growth_items/<id>.yml → GrowthItemFileConfig → GrowthItemLoader → GrowthItemRegistry
```

### Ajouter un growth item

1. `src/main/resources/growth_items/<mon_item>.yml`
2. Entrée dans `ECustomItem` (`createGrowthItem(meta, "<MON_ITEM>")`)
3. Nouveau `eventType` → ajouter un `@EventHandler` dans `GrowthItemListener`
4. Slot armure → `GrowthItems.IncrementUsesFromHelmet()` au lieu de `IncrementUses()`

### Types d'effets YAML

| `type` | Champs requis | Description |
|---|---|---|
| `name` | `value` | Renomme l'item |
| `message` | `value` | Envoie un message |
| `haste` | `seconds` | Haste II pendant N secondes |
| `add_enchantment` | `enchantment`, `amount` | Ajoute N niveaux |
| `set_attribute` | `attribute`, `attributeValue`, `operation`, `slot` | Remplace l'attribut |

### Types d'événements

| `eventType` | Description |
|---|---|
| `BlockBreakEvent` | Tout bloc cassé (item en main) |
| `OreBreakEvent` | Minerais (item en casque) |
| `CropBreakEvent` | Cultures (item en main) |

### Clés PDC

| Clé | Type | Contenu |
|---|---|---|
| `growth_id` | `STRING` | ID du growth item |
| `growth_uses` | `INTEGER` | Utilisations cumulées |
| `growth_tier` | `INTEGER` | Palier actuel (0 = aucun) |

---

## 14. Schémas JSON

→ Voir **⚠️ RÈGLES CRITIQUES** en début de fichier pour le workflow complet (schéma + test + SchemaGeneratorTest).

| Fichier YAML | Schéma |
|---|---|
| `quests/*.yml` | `schema/quests-schema.json` |
| `monsters.yml` | `schema/monsters-schema.json` |
| `roles.yml` | `schema/roles-schema.json` |
| `villagers/*.yml` | `schema/villagers-schema.json` |
| `traders/*.yml` | `schema/traders-schema.json` |
| `recipes.yml` | `schema/recipes-schema.json` |
| `global_quests.yml` | `schema/global-quests-schema.json` |
| `growth_items/*.yml` | `schema/growth-items-schema.json` |
| `jobs/*.yml` | `schema/jobs/<metier>-schema.json` — un fichier par métier, chacun son schéma |

---

## 15. Ce qu'il ne faut jamais faire

- Utiliser `§` ou `ChatColor` pour les textes
- Appeler `initAfterWorldsLoad()` manuellement (c'est `WorldInitializer` qui le déclenche)
- Accéder à `VillagerFactory` depuis `AlphaPlayer`
- Écrire du SQL hors d'un Repository
- `System.out.println` ou `plugin.getLogger()` — utiliser `MLLogManager`
- Créer un `new Manager()` hors de `GameManager.initAfterWorldsLoad()`
- Instancier un `MLVillager` avec `new` — toujours `MLVillager.spawn(() -> new ...)`
- Utiliser `player.sendMessage(String)` — toujours `player.sendMessage(Component)`
- Générer du code avec des APIs Bukkit deprecated dans Paper 26.1
- `runTaskAsynchronously(...)` dans un repository — utiliser `runAsync(...)` de `MLRepository`
- Passer uniquement `Connection` au constructeur d'un repository — toujours `(Connection, MLSQLite)`
- Appeler `Database.Request(...)` (supprimé) — utiliser `database.executeRaw(sql)`

---

## 16. Fichiers clés par sous-système

| Sous-système | Fichiers à lire avant de modifier |
|---|---|
| Joueurs | `AlphaPlayer`, `AlphaPlayerFactory`, `PlayerPersistenceService`, `PlayerAttributeService`, `PlayerEffectRestoreService`, `OfflineNotificationService`, `OfflineNotificationListener` |
| Rôles | `ERole`, `RoleLoader`, `RoleManagementService`, `roles.yml` |
| Quêtes | `QuestManager`, `GlobalQuestManager`, `Quest`, `EQuestType`, `QuestActionBarService`, `GlobalQuestBossBarService`, `quests/*.yml` |
| Histoirique & stats quêtes | `QuestHistoryEntry`, `QuestHistoryRepository`, `QuestCommand` (`history`), `PlayerCommand` |
| Villageois | `VillagerFactory`, `VillagerLevel`, `BlessingLoader`, `villagers/*.yml` |
| Monstres | `MobLevelManager`, `MobTypeConfig`, `monsters.yml` |
| Mondes | `WorldInitializer`, `WorldLevelManager`, `WorldResetManager`, `EWorld` |
| Tombes | `GraveManager`, `GraveData`, `GraveRepository`, `GraveListener` |
| Items | `ECustomItem`, `CustomItemBuilder`, `CustomRecipeFactory`, `recipes.yml` |
| Growth items | `GrowthItems`, `GrowthItemLoader`, `GrowthItemFileConfig`, `GrowthItemListener`, `GrowthItemRegistry`, `growth_items/*.yml` |
| Métiers | `EJob`, `JobLevelConfig`, `JobListener` |
| DB | `MLSQLite` + `MLRepository` (MiubyLib — voir Annexe), `Database`, `SQLite`, repositories dans `system/database/repository/` |
| Traductions | `LangService`, `ELang`, `MLMessageService` (MiubyLib — voir Annexe), `lang/fr.yml` |

---

- Jamais de `.stream()` dans un EventHandler fréquent (damage, move, tick) — boucles for ou structures pré-calculées.
- Jamais de `Bukkit.getWorld(String)` / `Bukkit.getPlayer(UUID)` en hot path — passer par `WorldRegistry` / `AlphaPlayer.get(uuid)`.
- Jamais de scan d'entités (`world.getEntitiesByType(...)`) dans un listener répétitif.
- **DB toujours async, sans exception.** Même pour les events ponctuels (`onDailyReset`, `onPlayerQuit`…).
- Ne pas allouer d'objets inutiles dans les listeners chauds.
- `ignoreCancelled = true` sur tous les `@EventHandler` sauf cas explicite.
- **Pré-cacher les références stables** (`GameManager.getInstance().getXxxManager()`) dans des champs `private final` initialisés dans le constructeur du listener — pas à chaque appel.
- **Ne jamais cacher `WorldRegistry.get(EWorld.XXX)` dans un champ.** Les mondes Wilderness, Nether et End peuvent être réinitialisés à tout moment — la référence deviendrait invalide silencieusement. Toujours appeler `WorldRegistry.get()` inline dans le handler. Un lookup `EnumMap` coûte ~10 ns : c'est négligeable.
- **Jamais `getAlphaPlayers()` / `getAll()` dans un hot path.** Pour des états rares (joueurs avec un rôle spécifique), maintenir un `Set<UUID>` dédié mis à jour sur l'event de changement d'état (`AlphaPlayerRoleChangeEvent`, etc.).

---

## 17. PerfTimer — mesure de performance à chaud

Chronomètre inline toggleable. **Zéro overhead quand désactivé** (retourne un singleton NO_OP, aucune allocation).

```java
try (var t = PerfTimer.start("DamageListener.onEntityDamage")) {
    // code à mesurer
}   // log automatique en WARNING/PERF si > 0,5 ms
```

**Activation / désactivation à chaud (en jeu, OP uniquement) :**
```
/survi perf on      → active les timers
/survi perf off     → désactive (overhead nul immédiatement)
/survi perf status  → état actuel
```

**Règles d'usage :**
- Poser un `PerfTimer` sur les hot paths sans condition : l'overhead désactivé est ~1 ns (volatile read).
- Nommer clairement : `"ClassName.methodName"` ou `"ClassName.section-specifique"` pour les blocs imbriqués.
- Les timers imbriqués sont autorisés (ex : timer global + timer sur le seul sous-bloc suspect).
- Le tag `PERF` est géré par `MLLogManager` comme tous les autres — il peut être filtré indépendamment de l'activation du timer.

**Points instrumentés actuellement :**
| Label | Emplacement | Risque |
|---|---|---|
| `DamageListener.onEntityDamageByEntity` | Modificateur dégâts joueur | faible |
| `DamageListener.onEntityDamage` | Résistance + mécanique FÉE | moyen |
| `DamageListener.FEE-propagation` | Itération `getAlphaPlayers()` FÉE | ⚠ élevé — à remplacer par `Set<UUID>` |
| `PlayerListener.onPlayerMove` | Vérification limites monde | moyen |
| `MinerListener.dropWithMultiplier` | `block.getDrops(tool)` Bukkit — Mineur | moyen |
| `LumberjackListener.dropWithMultiplier` | `block.getDrops(tool)` Bukkit — Bûcheron | moyen |
| `LumberjackListener.treeFeller` | BFS logs + drops — Bûcheron | ⚠ élevé sur grands arbres |
| `FarmerListener.dropWithMultiplier` | `block.getDrops(tool)` Bukkit — Fermier | moyen |

---

## 18. Checklist — ajouter un villageois

1. `villagers/monvillageois.yml` (valider contre `schema/villagers-schema.json`)
2. Classe `MonVillager extends AVillager` avec `loadData` / `createDefaultData` / `saveData`
3. Données : `MonVillagerData extends MLVillagerData` si état custom
4. Enregistrement dans `VillagerFactory` (map `nameId → Constructor`)
5. Blessings éventuels → `BlessingLoader` — si nouveau type de blessing : relancer `SchemaGeneratorTest.updateSchemas()`
6. Log `MLLogManager` avec `ELogTag.VILLAGER` dans `onInitialized()`

---

## 19. Checklist — ajouter une feature gameplay

1. Config YAML + **schéma JSON** + **test** + **`SchemaGeneratorTest.updateSchemas()`** si enum touché (voir ⚠️ RÈGLES CRITIQUES)
2. `XxxConfig` POJO (chargé par le Loader)
3. `XxxLoader` qui lit le YAML
4. `XxxManager` ou `XxxService` selon qu'il y a un état runtime
5. Listener fin qui délègue au service — jamais de logique métier dans le listener
6. Persistence en repository si données joueur concernées — étendre `MLRepository`
7. Logs via `MLLogManager` avec le bon `ELogTag`

---

## 20. Affichage des quêtes — ActionBar & BossBar

### Quêtes journalières — `QuestActionBarService`

Appelé depuis `QuestManager` à chaque progression et à la complétion. La barre reste visible en
permanence grâce à une tâche de rafraîchissement par joueur (40 ticks), démarrée au premier appel
de `showProgress` et annulée par `showCompleted` ou `stopRefresh`.

```java
gm.getQuestActionBarService().showProgress(player, quest, data); // met à jour + démarre rafraîchissement
gm.getQuestActionBarService().showFinished(player, quest);        // affiche message fin + stoppe rafraîchissement
gm.getQuestActionBarService().stopRefresh(uuid);                  // à appeler : déco, reset journalier, reset admin, reload
```

`stopRefresh` est appelé dans : `ServerListener.onPlayerQuit`, `ServerListener.onDailyReset`,
`QuestManager.resetQuest`, `QuestManager.assignSpecificQuest` (suppression ancienne quête),
`QuestManager.reload` (quête en cours supprimée).

### Quêtes globales — `GlobalQuestBossBarService`

Appelé depuis `GlobalQuestManager`. La barre reste visible **en permanence** pendant toute la quête ;
elle se masque uniquement à la fin (annulation immédiate, complétion après 10 s).
Les joueurs qui se connectent en cours de quête reçoivent la barre via `showToPlayer`, appelé depuis
`ServerListener.onPlayerJoin`.

```java
gm.getGlobalQuestBossBarService().onQuestStarted(quest);             // au démarrage (0 %, barre permanente)
gm.getGlobalQuestBossBarService().onProgressUpdate(quest, progress);  // à chaque progression (pas de palier)
gm.getGlobalQuestBossBarService().onQuestFinished(quest);             // 100 % barre verte, masque après 10 s
gm.getGlobalQuestBossBarService().onQuestEnded();                     // annulation / timeout → masquage immédiat
gm.getGlobalQuestBossBarService().showToPlayer(player);              // join en cours de quête
```

Ne jamais appeler directement `showBossBar` / `hideBossBar` sur les joueurs pour les quêtes globales — passer uniquement par ce service.

---

## Annexe — MiubyLib API complète

MiubyLib est la bibliothèque interne (`fr.miuby.lib`). Elle n'est pas fournie en contexte de base, mais peut l'être si besoin et peut être modifiée — tout ce qui suit est la référence autoritaire.

### `MiubyLib` (point d'entrée)

```java
MiubyLib.init(plugin);                        // appelé une seule fois dans GameManager.init()
MiubyLib.runLater(Runnable task, long delay); // equivalent BukkitScheduler.runTaskLater
MiubyLib.runAsync(Runnable task);             // equivalent BukkitScheduler.runTaskAsynchronously
MiubyLib.getLogger();
MiubyLib.callEvent(Event event);              // Bukkit.getPluginManager().callEvent()
```

### `MLPlayer` — base d'un joueur custom

```java
public class MLPlayer {
    protected final UUID uuid;
    protected String pseudo;   // @Getter @Setter
    protected Player player;   // @Getter @Setter (joueur Bukkit en ligne)
    public void onJoinServer() {} // hook override possible
}
```

`AlphaPlayer extends MLPlayer`. Ne jamais réimplémenter cette base.

### `MLPlayerRegistry<T extends MLPlayer>` — registry typé

```java
registry.register(player);  // indexé par UUID + pseudo
registry.get(UUID uuid);
registry.get(String pseudo);
registry.getAll();           // Collection<T>
```

Utilisé en interne par `AlphaPlayerFactory`.

### `MultiKeyRegistry<V>` — map multi-clés générique

```java
INSTANCE.register(value, key1, key2, ...); // autant de clés que voulu
INSTANCE.get(Object key);
INSTANCE.getAll();                          // Collection<V> (dédupliqué)
INSTANCE.contains(Object key);
INSTANCE.clear();
```

Utilisé comme backing store de tous les registries (VillagerRegistry, WorldRegistry, etc.).

### `Rect` — zone 3D

```java
record Rect(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin)
rect.isOut(int x, int y, int z); // true si le point est hors zone
```

### `MLVillager` — base d'un villageois custom

Pattern obligatoire : **toujours créer via `MLVillager.spawn(Constructor::new)`**, jamais `new`.

```java
MLVillager.spawn(() -> new MonVillager(...));

// Cycle de vie automatique :
// 1. loadData()          → null si premier spawn
// 2. createDefaultData() → appelé si loadData() == null
// 3. spawnVillager()     → crée l'entité Bukkit (AI off, collidable off, persistent on)
// 4. saveData()
// 5. onInitialized()     → applique displayName, crée inventaire, fire VillagerLoadedEvent
// Si déjà existant : findVillager() avec retry (10 × 10 ticks), puis respawn auto
```

Méthodes abstraites :

```java
protected abstract @Nullable MLVillagerData loadData();
protected abstract void saveData();
protected abstract MLVillagerData createDefaultData();
protected void onInitialized() { super.onInitialized(); }

villager.getNameId()    // identifiant stable (ex: "survivant")
villager.getVillager()  // Villager Bukkit — null-safe
villager.getDisplayName()
villager.getInventory()
```

### `MLVillagerData`

```java
@AllArgsConstructor
public class MLVillagerData {
    protected UUID uuid;
    protected final String nameId;
    @NotNull protected Location location;
}
```

### `VillagerLoadedEvent`

Firé automatiquement par `onInitialized()`. Écouter pour agir après qu'un villageois soit prêt.

```java
@EventHandler
public void onVillagerLoaded(VillagerLoadedEvent event) {
    MLVillager villager = event.getVillager();
}
```

### `VillagerRegistry`

```java
VillagerRegistry.register(mlVillager);  // clés : UUID Bukkit + nameId
VillagerRegistry.get(UUID uuid);
VillagerRegistry.get(String nameId);
VillagerRegistry.getAll();
VillagerRegistry.contains(UUID uuid);
VillagerRegistry.contains(String nameId);
```

### `MLWorld`

```java
mlWorld.setLimit(Rect rect);
mlWorld.setLocked(boolean);
mlWorld.isPlayerInWorld(Player);
mlWorld.isPlayerOutOfLimit(Player); // false si pas de limit ou pas dans le monde
mlWorld.getUUID();
mlWorld.getWorld();                 // World Bukkit
```

### `WorldRegistry`

```java
WorldRegistry.register(mlWorld);         // clés : UUID monde + name + WorldType
WorldRegistry.get(UUID uuid);
WorldRegistry.get(String name);
WorldRegistry.get(WorldType type);       // ex: WorldRegistry.get(EWorld.VILLAGE)
WorldRegistry.getAll();
WorldRegistry.isPlayerInRegisteredWorld(Player);
WorldRegistry.get(Player player);        // retourne le MLWorld du joueur
```

`EWorld` implémente `WorldType`. Toujours utiliser `EWorld` côté Survi.

### `MLLogManager` — logging par tags et par niveaux

Singleton. Un message est émis seulement si son tag **et** son level sont tous les deux activés.

**Setup (ordre obligatoire dans `GameManager.init()`) :**
```java
MiubyLib.init(plugin);
MLLogManager.getInstance().registerTags(ELogTag.values());
MLLogManager.getInstance().initialize(new LogPersistence(db)); // ou .initialize() sans DB
```

**Logging :**
```java
MLLogManager.getInstance().log(Level.INFO,    ELogTag.SYSTEM, "Message");
MLLogManager.getInstance().log(Level.SEVERE,  ELogTag.QUEST,  "Message", exception);
```

**Gestion runtime :**
```java
MLLogManager.getInstance().toggleTag(ELogTag.QUEST);     // ou toggleTag("QUEST")
MLLogManager.getInstance().setProductionMode();           // WARNING + SEVERE uniquement
MLLogManager.getInstance().setDebugMode();                // tout activé
MLLogManager.getInstance().getAllTagStates();             // Map<String, Boolean>
```

**`MLLogPersistence`** — brancher la DB :
```java
public class LogPersistence implements MLLogPersistence {
    // 4 méthodes : getTagState, saveTagState, getLevelState, saveLevelState
    // branchées sur SystemRepository (table server_data)
}
```

### `MLLogCommand`

Sous-arbre Brigadier prêt à l'emploi (`/xxx log status|tag|level|mode`).

```java
return Commands.literal("monplugin")
        .then(MLLogCommand.create())  // ajoute /monplugin log ...
        .then(...);
```

### `Cooldown<K>`

```java
private final Cooldown<UUID> cd = new Cooldown<>(6_000L); // 6 secondes

if (!cd.isOnCooldown(uuid)) { cd.set(uuid); /* action */ }
cd.remaining(uuid); // ms restantes
cd.reset(uuid);
cd.clear();
```

### `MLBrigadierHelper`

```java
MLBrigadierHelper.notFound("Joueur")      // → DynamicCommandExceptionType "Joueur introuvable : {name}"
MLBrigadierHelper.simpleError("Message")  // → SimpleCommandExceptionType
```

### `MLStringArgument<T>` — arguments custom Brigadier

```java
public class MonArgument extends MLStringArgument<MonType> {
    public static MonArgument monType() { return new MonArgument(); }

    @Override
    public MonType convert(String value) throws CommandSyntaxException {
        MonType r = registry.get(value);
        if (r == null) throw CommandErrors.MON_ERREUR.create(value);
        return r;
    }

    @Override
    protected Collection<String> suggestions() {
        return registry.getAll().stream().map(MonType::getId).toList();
    }
}
```

Si suggestions dépendent du contexte → surcharger `listSuggestions` directement.

### `MLResourceManager` — déploiement YAML et chargement POJO

```java
// Déploiement JAR → disque (avant MiubyLib.init())
MLResourceManager.deploy(this, "config.yml");
MLResourceManager.deployFolder(this, "villagers");

// Chargement POJO (résultats mis en cache)
TraderConfig cfg = MLResourceManager.loadPojo(plugin, "traders", "barman", TraderConfig.class);
List<TraderConfig> all = MLResourceManager.loadPojoAll(plugin, "traders", TraderConfig.class);
MLResourceManager.clearCache(); // dans onDisable() si rechargement à chaud
```

`VillagerLevelLoader.load(id)` et `TraderLoader.loadAll()` sont de simples délégations vers `MLResourceManager`.

### `MLMessageService` — traductions YAML + MiniMessage (mono- ou multi-langue)

Templates dans `<resourceFolder>/<locale>.yml` (déployés via `MLResourceManager.deployFolder`),
format YAML + MiniMessage — placeholders `{0}`, `{1}`… (texte échappé) et `<name>` (`TagResolver`).

```java
MLMessageService msg = new MLMessageService(plugin, "lang", List.of("fr"), true);
// locales.get(0) = locale par défaut. locales.size() == 1 → mono-langue :
// resolveLanguage() retourne toujours cette locale, forceDefault ignoré.

msg.resolveLanguage(player);    // code de locale ("fr", "en"...)
msg.resolveOrDefault(sender);   // idem, locale par défaut si pas un Player
msg.getDefaultLocale();         // locales.get(0)

msg.text(player, "key");
msg.text(player, "key", arg0, arg1);                            // {0}, {1}…
msg.text(player, "key", Placeholder.unparsed("name", value));   // <name>
msg.text("fr", "key", ...);     // variantes par code de locale explicite

msg.broadcast("key", arg0, arg1);  // tous les joueurs en ligne, dans leur langue
msg.getString("fr", "key");        // chaîne brute, pour l'insérer dans un autre template
```

**Clé manquante** : message visible (rouge/gras, contient la clé — template overridable
via le constructeur à 5 arguments) + log `MLLogManager` (`WARNING`, tag `MESSAGE`) une
seule fois par clé. Fallback : locale demandée → locale par défaut → "clé manquante".

`LangService` (Survi) est une façade fine dessus — voir section 7.

### `MLSQLite` — base abstraite SQLite

**Chaîne d'héritage dans Survi :**
```
MLSQLite  (MiubyLib) — connexion, PRAGMA user_version, load() final
   ↑
Database  (Survi, abstract) — repositories + délégués
   ↑
SQLite    (Survi, concrete) — SQL des tables, runMigrations(), getTargetVersion()
```

**`load()` est finale.** Séquence : `createTables()` → vérif version → `runMigrations()` → `setVersion()` → `onLoaded()`.

**Implémenter :**
```java
protected abstract int getTargetVersion();
protected abstract void createTables() throws SQLException;   // CREATE TABLE IF NOT EXISTS
protected abstract void runMigrations(int currentVersion) throws SQLException;

@Override
protected void onLoaded() {
    super.onLoaded(); // toujours en premier
    myRepo = new MyRepository(getConnection(), this);
}
```

**Utilitaires protégés :** `getConnection()`, `getCurrentVersion()`, `hasColumn(table, col)`, `executeRaw(sql)`.

### `MLRepository` — repositories SQLite

```java
public class MyRepository extends MLRepository {
    public MyRepository(Connection connection, MLSQLite db) { super(connection, db); }

    // Lecture sync (thread principal) — try-with-resources sur PreparedStatement uniquement
    public String load(String id) { /* connection.prepareStatement(...) */ }

    // Écriture async — toujours via runAsync, jamais runTaskAsynchronously
    public void save(String id, String name) {
        runAsync(conn -> { /* conn.prepareStatement(...) */ }, ELogTag.SYSTEM, "Failed to save");
    }
}
```

**Règle :** toujours passer `(Connection, MLSQLite)` au constructeur — `db` est nécessaire pour `runAsync`.