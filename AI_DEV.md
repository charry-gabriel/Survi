# AI_DEV.md — Guide de développement Survi pour Claude

Ce fichier est destiné à Claude pour générer du code cohérent avec ce projet.
**Lire en entier avant de générer quoi que ce soit.**

---

## 0. Instructions pour Claude

- Ne jamais écrire de résumé de ce qui a été créé ou modifié. Fournir les fichiers directement, c'est suffisant.
- Mettre à jour `AI_DEV.md` si une modification touche grandement un système existant (nouveau sous-système, changement de convention, nouveau pattern d'architecture).
- **Longueur de ligne** : ne pas couper les lignes de code artificiellement. L'écran est large — une ligne peut facilement dépasser 120 caractères sans problème. Ne couper que si la ligne est vraiment trop longue pour être lisible d'un bloc (150+ caractères), pas par convention "80 colonnes".

---

## 1. Contexte technique

- **Plateforme** : Paper `api-version: 26.1` (version 1.21.x, notation changée en 2026)
- **Package racine** : `fr.miuby.survi`
- **Bibliothèque interne** : `fr.miuby.lib` (MiubyLib — voir section dédiée)
- **Build** : Lombok est disponible (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, etc.)
- **DB** : SQLite via repositories
- **Java** : 25
- **Lombok** : 1.18.38
- **Paper API** : `26.1.2.build.66-stable`
- MiubyLib est shadowjarisée et relocalisée dans `fr.miuby.survi.shaded.lib` à la compilation

### ⚠️ Compatibilité Paper 26.1

La structure des dimensions a changé. Les noms de dossiers/mondes suivent désormais
la convention `<worldName>_nether` et `<worldName>_the_end`. Voir `WorldInitializer` pour la convention exacte du projet.

Essayer de ne pas générer de code qui utilise des APIs marquées `@Deprecated` dans Paper 26.1.

---

## 2. MiubyLib — documentation complète

MiubyLib est la bibliothèque interne (`fr.miuby.lib`). Elle n'est **jamais fournie en contexte**,
donc tout ce qui suit est la référence autoritaire.

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

---

### `MLLogCommand`

Sous-arbre Brigadier prêt à l'emploi (`/xxx log status|tag|level|mode`).

```java
return Commands.literal("monplugin")
        .then(MLLogCommand.create())  // ajoute /monplugin log ...
        .then(...);
```

---

### `Cooldown<K>`

```java
private final Cooldown<UUID> cd = new Cooldown<>(6_000L); // 6 secondes

if (!cd.isOnCooldown(uuid)) { cd.set(uuid); /* action */ }
cd.remaining(uuid); // ms restantes
cd.reset(uuid);
cd.clear();
```

---

### `MLBrigadierHelper`

```java
MLBrigadierHelper.notFound("Joueur")      // → DynamicCommandExceptionType "Joueur introuvable : {name}"
MLBrigadierHelper.simpleError("Message")  // → SimpleCommandExceptionType
```

---

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

---

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

---

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

---

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

## 7. Textes — Adventure uniquement

```java
Component.text("Message", NamedTextColor.GREEN)
player.sendMessage(Component.text("...").color(NamedTextColor.RED))

// Interdit — jamais
"§aMessage"
ChatColor.GREEN + "Message"
player.sendMessage("texte brut")
```

---

## 8. Base de données

Hiérarchie : `SQLite extends Database extends MLSQLite` (voir section 2 MiubyLib).

```java
GameManager.getInstance().getDatabase().players()    // PlayerRepository
GameManager.getInstance().getDatabase().villagers()  // VillagerRepository
GameManager.getInstance().getDatabase().quests()     // QuestRepository
GameManager.getInstance().getDatabase().crops()      // CropRepository
GameManager.getInstance().getDatabase().system()     // SystemRepository (logs, serverData)
GameManager.getInstance().getDatabase().graves()     // GraveRepository
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

### Modifier un fichier YAML — workflow obligatoire

Toute modification implique **trois mises à jour simultanées** :

1. **Le schéma JSON** (`schema/*.json`)
2. **Le test de config associé** (`*ConfigTest.java`)
3. **`SchemaGeneratorTest.java`** — si le changement touche un enum Minecraft ou du projet

`SchemaGeneratorTest.updateSchemas()` regénère les listes d'enums automatiquement. Ne jamais les écrire à la main.

| Fichier YAML | Schéma | Test |
|---|---|---|
| `quests.yml` | `schema/quests-schema.json` | `QuestConfigTest` |
| `monsters.yml` | `schema/monsters-schema.json` | `MonstersConfigTest` |
| `roles.yml` | `schema/roles-schema.json` | `RolesConfigTest` |
| `villagers/*.yml` | `schema/villagers-schema.json` | `VillagerConfigTest` |
| `traders/*.yml` | `schema/traders-schema.json` | `TraderConfigTest` |
| `recipes.yml` | `schema/recipes-schema.json` | `RecipesConfigTest` |
| `global_quests.yml` | `schema/global-quests-schema.json` | `GlobalQuestConfigTest` |
| `growth_items/*.yml` | `schema/growth-items-schema.json` | `GrowthItemConfigTest` |

---

## 10. Logging

```java
MLLogManager.getInstance().log(Level.INFO,    ELogTag.QUEST,    "Message");
MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER, "Message", exception);
```

Jamais `System.out.println` ni `plugin.getLogger().info(...)`.

Tags disponibles : `PLAYER`, `VILLAGER`, `QUEST`, `REPUTATION`, `ITEM`, `ROLE`, `JOB`, `WORLD`, `SYSTEM`, `GRAVE`.

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
// Préférer MiubyLib dans les nouveaux managers :
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

| Fichier YAML | Schéma |
|---|---|
| `quests.yml` | `schema/quests-schema.json` |
| `monsters.yml` | `schema/monsters-schema.json` |
| `roles.yml` | `schema/roles-schema.json` |
| `villagers/*.yml` | `schema/villagers-schema.json` |
| `traders/*.yml` | `schema/traders-schema.json` |
| `recipes.yml` | `schema/recipes-schema.json` |
| `growth_items/*.yml` | `schema/growth-items-schema.json` |

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
| Quêtes | `QuestManager`, `GlobalQuestManager`, `Quest`, `EQuestType`, `QuestActionBarService`, `GlobalQuestBossBarService`, `quests.yml` |
| Villageois | `VillagerFactory`, `VillagerLevel`, `BlessingLoader`, `villagers/*.yml` |
| Monstres | `MobLevelManager`, `MobTypeConfig`, `monsters.yml` |
| Mondes | `WorldInitializer`, `WorldLevelManager`, `WorldResetManager`, `EWorld` |
| Tombes | `GraveManager`, `GraveData`, `GraveRepository`, `GraveListener` |
| Items | `ECustomItem`, `CustomItemBuilder`, `CustomRecipeFactory`, `recipes.yml` |
| Growth items | `GrowthItems`, `GrowthItemLoader`, `GrowthItemFileConfig`, `GrowthItemListener`, `GrowthItemRegistry`, `growth_items/*.yml` |
| Métiers | `EJob`, `JobLevelConfig`, `JobListener` |
| DB | `MLSQLite` + `MLRepository` (MiubyLib), `Database`, `SQLite`, repositories dans `system/database/repository/` |

---

## 20. Affichage des quêtes — ActionBar & BossBar

### Quêtes journalières — `QuestActionBarService`

Appelé depuis `QuestManager` à chaque progression et à la complétion. Durée native Paper (~3 s).

```java
gm.getQuestActionBarService().showProgress(player, quest, data);  // avec cooldown 2s anti-spam
gm.getQuestActionBarService().showCompleted(player, quest);        // toujours affiché, reset cooldown
```

### Quêtes globales — `GlobalQuestBossBarService`

Appelé depuis `GlobalQuestManager`. La barre s'affiche 5 s (100 ticks) puis se masque automatiquement.

```java
gm.getGlobalQuestBossBarService().onQuestStarted(quest);           // au démarrage (0 %)
gm.getGlobalQuestBossBarService().onProgressUpdate(quest, progress);// à chaque palier de 10 %
gm.getGlobalQuestBossBarService().onQuestCompleted(quest);          // à 100 % (barre verte)
gm.getGlobalQuestBossBarService().onQuestEnded();                   // annulation / timeout
```

Ne jamais appeler directement `showBossBar` / `hideBossBar` sur les joueurs pour les quêtes globales — passer uniquement par ce service.

---

- Jamais de `.stream()` dans un EventHandler fréquent (damage, move, tick) — boucles for ou structures pré-calculées.
- Jamais de `Bukkit.getWorld(String)` / `Bukkit.getPlayer(UUID)` en hot path — passer par `WorldRegistry` / `AlphaPlayer.get(uuid)`.
- Jamais de scan d'entités (`world.getEntitiesByType(...)`) dans un listener répétitif.
- **DB toujours async, sans exception.** Même pour les events ponctuels (`onDailyReset`, `onPlayerQuit`…).
- Ne pas allouer d'objets inutiles dans les listeners chauds.
- `ignoreCancelled = true` sur tous les `@EventHandler` sauf cas explicite.
- **Pré-cacher les références stables** (`WorldRegistry.get(EWorld.XXX)`, `GameManager.getInstance().getXxxManager()`) dans des champs `private final` initialisés dans le constructeur du listener — pas à chaque appel.
- **Jamais `getAlphaPlayers()` / `getAll()` dans un hot path.** Pour des états rares (joueurs avec un rôle spécifique), maintenir un `Set<UUID>` dédié mis à jour sur l'event de changement d'état (`AlphaPlayerRoleChangeEvent`, etc.).

---

## 18. Checklist — ajouter un villageois

1. `villagers/monvillageois.yml` (valider contre `schema/villagers-schema.json`)
2. Classe `MonVillager extends AVillager` avec `loadData` / `createDefaultData` / `saveData`
3. Données : `MonVillagerData extends MLVillagerData` si état custom
4. Enregistrement dans `VillagerFactory` (map `nameId → Constructor`)
5. Blessings éventuels → `BlessingLoader`
6. Log `MLLogManager` avec `ELogTag.VILLAGER` dans `onInitialized()`

---

## 19. Checklist — ajouter une feature gameplay

1. Config YAML + schéma JSON + test (voir workflow section 9)
2. `XxxConfig` POJO (chargé par le Loader)
3. `XxxLoader` qui lit le YAML
4. `XxxManager` ou `XxxService` selon qu'il y a un état runtime
5. Listener fin qui délègue au service — jamais de logique métier dans le listener
6. Persistence en repository si données joueur concernées — étendre `MLRepository`
7. Logs via `MLLogManager` avec le bon `ELogTag`