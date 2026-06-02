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
la convention `<worldName>_nether` et `<worldName>_the_end` (et non plus `<worldName>_nether`
avec structure de fichier distincte). Voir `WorldInitializer` pour la convention exacte du projet.

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
MiubyLib.getLogger();                         // Logger du plugin
MiubyLib.callEvent(Event event);              // Bukkit.getPluginManager().callEvent()
```

### `MLPlayer` — base d'un joueur custom

```java
public class MLPlayer {
    protected final UUID uuid;
    protected String pseudo;   // @Getter @Setter
    protected Player player;   // @Getter @Setter (joueur Bukkit en ligne)

    public MLPlayer(UUID uuid) { ... }
    public void onJoinServer() {} // hook override possible
}
```

`AlphaPlayer extends MLPlayer`. Ne jamais réimplémenter cette base.

### `MLPlayerRegistry<T extends MLPlayer>` — registry typé

```java
registry.register(player);       // indexé par UUID + pseudo
registry.get(UUID uuid);
registry.get(String pseudo);
registry.getAll();                // Collection<T>
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

Utilisé pour les limites de mondes (`MLWorld.setLimit(rect)`).

### `MLVillager` — base d'un villageois custom

Pattern obligatoire : **toujours créer via `MLVillager.spawn(Constructor::new)`**, jamais `new`.

```java
// Création
MLVillager.spawn(() -> new MonVillager(...));

// Cycle de vie géré automatiquement :
// 1. loadData()        → charge depuis DB/fichier (retourne null si premier spawn)
// 2. createDefaultData() → appelé si loadData() retourne null
// 3. spawnVillager()   → crée l'entité Bukkit (AI off, collidable off, persistent on)
// 4. saveData()        → persiste
// 5. onInitialized()   → hook final (applique displayName, crée inventaire, fire VillagerLoadedEvent)

// Si déjà existant : findVillager() avec retry automatique (10 tentatives × 10 ticks)
// Si toujours introuvable : respawn automatique
```

Méthodes abstraites à implémenter :

```java
protected abstract @Nullable MLVillagerData loadData();
protected abstract void saveData();
protected abstract MLVillagerData createDefaultData();
// Override optionnel :
protected void onInitialized() { super.onInitialized(); /* custom */ }
```

Champs accessibles :

```java
villager.getNameId()     // String — identifiant stable (ex: "survivant")
villager.getVillager()   // Villager Bukkit — null-safe, relookup par UUID si besoin
villager.getDisplayName()
villager.getInventory()
```

### `MLVillagerData` — données persistées d'un villageois

```java
@AllArgsConstructor
public class MLVillagerData {
    protected UUID uuid;
    protected final String nameId;
    @NotNull protected Location location;
}
```

### `VillagerLoadedEvent` — événement Paper

Firé automatiquement par `onInitialized()`. Écouter pour agir après qu'un villageois soit prêt.

```java
@EventHandler
public void onVillagerLoaded(VillagerLoadedEvent event) {
    MLVillager villager = event.getVillager();
}
```

### `VillagerRegistry` — registry global des villageois

```java
VillagerRegistry.register(mlVillager);     // clés : UUID Bukkit + nameId
VillagerRegistry.get(UUID uuid);
VillagerRegistry.get(String nameId);
VillagerRegistry.getAll();                 // Collection<MLVillager>
VillagerRegistry.contains(UUID uuid);
VillagerRegistry.contains(String nameId);
```

### `MLWorld` — monde wrappé

```java
@RequiredArgsConstructor
public class MLWorld {
    private final World world;       // World Bukkit
    private final String name;       // nom d'affichage (ex: "Wilderness")
    private final NamedTextColor color;
    private final WorldType type;    // EWorld

    mlWorld.setLimit(Rect rect);
    mlWorld.setLocked(boolean);
    mlWorld.isPlayerInWorld(Player);
    mlWorld.isPlayerOutOfLimit(Player); // false si pas de limit ou pas dans le monde
    mlWorld.getUUID();
    mlWorld.getWorld();                 // World Bukkit
}
```

### `WorldRegistry` — registry global des mondes

```java
WorldRegistry.register(mlWorld);         // clés : UUID monde + name + WorldType
WorldRegistry.get(UUID uuid);
WorldRegistry.get(String name);
WorldRegistry.get(WorldType type);       // ex: WorldRegistry.get(EWorld.VILLAGE)
WorldRegistry.getAll();
WorldRegistry.isPlayerInRegisteredWorld(Player);
WorldRegistry.get(Player player);        // retourne le MLWorld du joueur
```

### `WorldType` — interface marqueur

```java
public interface WorldType {}
```

`EWorld` implémente `WorldType`. Toujours utiliser `EWorld` côté Survi.

### `MLLogManager` — logging par tags et par niveaux

Singleton. Un message est émis seulement si son tag **et** son level sont tous les deux activés.

**Setup (ordre obligatoire dans `GameManager.init()`) :**
```java
MiubyLib.init(plugin);                                         // 1. MiubyLib d'abord
MLLogManager.getInstance().registerTags(ELogTag.values());     // 2. enregistrer les tags du plugin
// ... initialiser la DB ...
MLLogManager.getInstance().initialize(new LogPersistence(db)); // 3. initialiser avec persistence
// ou sans persistence :
MLLogManager.getInstance().initialize();
```

**Logging :**
```java
MLLogManager.getInstance().log(Level.INFO,    ELogTag.SYSTEM,  "Message");
MLLogManager.getInstance().log(Level.WARNING, ELogTag.PLAYER,  "Message");
MLLogManager.getInstance().log(Level.SEVERE,  ELogTag.QUEST,   "Message", exception); // avec stacktrace
```

**Gestion runtime — overloads `ILogTag` et `String` disponibles :**
```java
MLLogManager.getInstance().toggleTag(ELogTag.QUEST);           // via ILogTag
MLLogManager.getInstance().toggleTag("QUEST");                  // via String (utilisé par MLLogCommand)
MLLogManager.getInstance().setTagEnabled(ELogTag.WORLD, false);
MLLogManager.getInstance().setProductionMode();                 // seulement WARNING + SEVERE
MLLogManager.getInstance().setDebugMode();                      // tout activé
MLLogManager.getInstance().setQuietMode();                      // seulement SEVERE
MLLogManager.getInstance().getAllTagStates();                    // Map<String, Boolean>
MLLogManager.getInstance().getAllLevelStates();                  // Map<Level, Boolean>
```

**Sorties fichiers** (dans `plugins/<Plugin>/logs/`) :
- `*-debug.log` — tout, stacktraces incluses
- `*-info.log` — INFO+
- `*-warn.log` — WARNING+
- Console — WARNING+ uniquement

**`ILogTag`** — interface marqueur pour les enums de tags :
```java
public enum ELogTag implements ILogTag { PLAYER, VILLAGER, QUEST, ... }
```

**`MLLogPersistence`** — interface à implémenter pour brancher la DB :
```java
public class LogPersistence implements MLLogPersistence {
    @Override public Boolean getTagState(String name) { return repo.getLogTagState(name); }
    @Override public void saveTagState(String name, boolean e) { repo.saveLogTagState(name, e); }
    @Override public Boolean getLevelState(String name) { return repo.getLogLevelState(name); }
    @Override public void saveLevelState(String name, boolean e) { repo.saveLogLevelState(name, e); }
}
```
`LogPersistence` (Survi) branche sur `SystemRepository` (table `server_data`).

---

### `MLLogCommand` — sous-commande générique de gestion des logs

Sous-arbre Brigadier prêt à l'emploi. Expose `/xxx log status|tag|level|mode` pour n'importe quel plugin.

```java
// Dans la commande principale du plugin :
return Commands.literal("monplugin")
        .requires(s -> s.getSender().isOp())
        .then(MLLogCommand.create())   // ajoute /monplugin log ...
        .then(...);
```

Survi l'utilise dans `SystemCommand` : `.then(MLLogCommand.create())` remplace les ~200 lignes de gestion des logs qui s'y trouvaient.

---

### `Cooldown<K>` — cooldown générique basé sur le temps

```java
private final Cooldown<UUID> warnCooldown = new Cooldown<>(6_000L); // 6 secondes

if (!warnCooldown.isOnCooldown(player.getUniqueId())) {
    warnCooldown.set(player.getUniqueId());
    player.sendMessage("...");
}

warnCooldown.remaining(key); // millisecondes restantes (0 si pas en cooldown)
warnCooldown.reset(key);     // force la fin du cooldown
warnCooldown.clear();        // efface tous les cooldowns
```

---

### `MLBrigadierHelper` — utilitaire Brigadier

```java
// Conversion Component → Message (usage bas niveau)
MLBrigadierHelper.message(Component.text("Joueur introuvable : " + name))

// Factory methods (usage recommandé — voir CommandErrors)
MLBrigadierHelper.notFound("Joueur")     // → DynamicCommandExceptionType "Joueur introuvable : {name}"
MLBrigadierHelper.simpleError("Message") // → SimpleCommandExceptionType
```

---

### `MLStringArgument<T>` — base générique pour les arguments custom Brigadier

Élimine le boilerplate des arguments basés sur une correspondance `String → T`.

```java
public class MonArgument extends MLStringArgument<MonType> {
    public static MonArgument monType() { return new MonArgument(); }

    @Override
    public MonType convert(String value) throws CommandSyntaxException {
        MonType result = registry.get(value);
        if (result == null) throw CommandErrors.MON_ERREUR.create(value);
        return result;
    }

    @Override
    protected Collection<String> suggestions() {
        return registry.getAll().stream().map(MonType::getId).toList();
    }

    public static MonType get(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, MonType.class);
    }
}
```

Si les suggestions dépendent du contexte (ex : `SubRoleArgument`), surcharger directement `listSuggestions` — `suggestions()` peut être ignoré.

---

### `MLResourceManager` — déploiement YAML et chargement POJO

Classe utilitaire (`fr.miuby.lib.resource`) pour tout ce qui touche aux fichiers YAML d'un plugin.

**Déploiement (JAR → disque)** — peut être appelé *avant* `MiubyLib.init()` :
```java
// Un fichier unique (MD5 check : crée si absent, écrase si modifié, rien si identique)
MLResourceManager.deploy(this, "config.yml");
MLResourceManager.deploy(this, "villagers/bob.yml");

// Tous les .yml d'un dossier embarqué dans le JAR
MLResourceManager.deployFolder(this, "villagers");
MLResourceManager.deployFolder(this, "traders");
```

**Chargement POJO via SnakeYAML (résultats mis en cache)** :
```java
// Un seul fichier : traders/barman.yml → TraderConfig
TraderConfig cfg = MLResourceManager.loadPojo(plugin, "traders", "barman", TraderConfig.class);

// Tout le dossier traders/ → List<TraderConfig>
List<TraderConfig> all = MLResourceManager.loadPojoAll(plugin, "traders", TraderConfig.class);

// Cache global — appels répétés retournent la même instance sans relire le disque
MLResourceManager.clearCache(); // à appeler dans onDisable() si rechargement à chaud
```

Pattern dans `onEnable()` de Survi (et de tout nouveau plugin MiubyLib) :
```java
// 1. Déploiement (avant MiubyLib.init())
MLResourceManager.deployFolder(this, "villagers");
MLResourceManager.deploy(this, "config.yml");

// 2. Chargement (après MiubyLib.init(), dans les factories/managers)
MyConfig cfg = MLResourceManager.loadPojo(plugin, "villagers", id, MyConfig.class);
```

`VillagerLevelLoader` et `TraderLoader` dans Survi sont désormais de simples délégations vers `MLResourceManager` — les appels existants (`VillagerLevelLoader.load(id)`, `TraderLoader.loadAll()`) restent inchangés.

---

### `MLSQLite` — base abstraite SQLite

Classe abstraite dans `fr.miuby.lib.sqlite`. Gère l'ouverture du fichier `.db`, le versionnage
du schéma via `PRAGMA user_version` et l'orchestration des migrations.
Chaque plugin implémente seulement `createTables()`, `runMigrations(int)` et `getTargetVersion()`.

**Chaîne d'héritage dans Survi :**
```
MLSQLite  (MiubyLib) — connexion, PRAGMA user_version, load() final
   ↑
Database  (Survi, abstract) — repositories + délégués, onLoaded() init repos
   ↑
SQLite    (Survi, concrete) — SQL des tables, runMigrations(), getTargetVersion()
```

**Méthodes abstraites à implémenter :**
```java
protected abstract int getTargetVersion();                          // version cible du schéma
protected abstract void createTables() throws SQLException;        // CREATE TABLE IF NOT EXISTS
protected abstract void runMigrations(int currentVersion) throws SQLException; // migrations
```

**Hook :**
```java
@Override
protected void onLoaded() {
    super.onLoaded();   // toujours appeler super en premier (init repos du parent)
    myRepo = new MyRepository(getConnection(), this); // passer this (MLSQLite) au repo
}
```

**Utilitaires protégés disponibles dans les sous-classes :**
```java
getConnection()                           // connexion (cache server thread, new si async)
getCurrentVersion()                       // lit PRAGMA user_version
setVersion(int version)                   // écrit PRAGMA user_version — appelé auto par load()
hasColumn(String table, String column)    // utile pour ALTER TABLE idempotent dans runMigrations()
executeRaw(String sql)                    // debug/admin uniquement — retourne String formatée
```

**`load()` est finale** — ne pas surcharger. Séquence fixe : `createTables()` → vérif version →
si outdated : `runMigrations()` → `setVersion()` → `onLoaded()`.

**Créer un nouveau plugin avec SQLite :**
```java
public class MyDatabase extends MLSQLite {
    private static final int TARGET_VERSION = 1;
    private MyRepository myRepo;

    public MyDatabase(JavaPlugin plugin) {
        super(plugin.getConfig().getString("db", "myplugin"));
    }

    @Override protected int getTargetVersion() { return TARGET_VERSION; }

    @Override
    protected void createTables() throws SQLException {
        try (Statement s = getConnection().createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player (uuid TEXT PRIMARY KEY, name TEXT NOT NULL)");
        }
    }

    @Override
    protected void runMigrations(int currentVersion) throws SQLException {
        // try (Statement s = getConnection().createStatement()) { if (currentVersion < 2) { ... } }
    }

    @Override
    protected void onLoaded() {
        myRepo = new MyRepository(getConnection(), this); // toujours passer this
    }

    public MyRepository myRepo() { return myRepo; }
}
```

---

### `MLRepository` — classe de base pour les repositories SQLite

Classe abstraite dans `fr.miuby.lib.sqlite`. Fournit le pattern async sans boilerplate.

```java
public class MyRepository extends MLRepository {
    public MyRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // Lecture sync — utilise this.connection
    public String load(String id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM my_table WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Failed to load", ex);
            return null;
        }
    }

    // Écriture async — runAsync ouvre une connexion fraîche et la ferme automatiquement
    public void save(String id, String name) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO my_table (id, name) VALUES (?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }, ELogTag.SYSTEM, "Failed to save");
    }
}
```

**Règles :**
- Toujours passer `(Connection connection, MLSQLite db)` au constructeur — `db` est utilisé par `runAsync` pour ouvrir les connexions async.
- Les lectures qui ont lieu au démarrage (thread principal) utilisent `this.connection` directement avec try-with-resources sur le `PreparedStatement` uniquement (la connexion reste ouverte).
- Les écritures sont toujours via `runAsync` — jamais de `GameManager.getInstance().getScheduler().runTaskAsynchronously(...)` dans un repository.

---

## 3. Point d'entrée — GameManager

Singleton. Tout accès aux services passe par là :

```java
GameManager gm = GameManager.getInstance();

gm.getPlugin()                  // JavaPlugin
gm.getScheduler()               // BukkitScheduler
gm.getDatabase()                // Database (SQLite, voir section DB)
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

Récupérer un `AlphaPlayer` :

```java
AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId()); // shortcut statique
// ou
AlphaPlayer ap = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
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

API Paper : `io.papermc.paper.command.brigadier`. Pattern systématique :

```java
public class MonCommand {
    private MonCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("macommande")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.literal("sub")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            // logique ici
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

**Créer un nouvel argument** → étendre `MLStringArgument<T>` (voir section 2 MiubyLib).

**Erreurs Brigadier** → `MLBrigadierHelper.notFound("Label")` ou `MLBrigadierHelper.simpleError("Message")`.
`CommandErrors` utilise ces factory methods directement.

**Autocomplétion** (exemple depuis `SystemCommand`) :

```java
.then(Commands.argument("tag", StringArgumentType.word())
    .suggests((context, builder) -> {
        EnumSet.allOf(MonEnum.class).forEach(v -> builder.suggest(v.name()));
        return builder.buildFuture();
    })
    .executes(ctx -> { ... }))
```

---

## 7. Textes — Adventure uniquement

```java
// Correct
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

Accès via repositories :

```java
GameManager.getInstance().getDatabase().players()    // PlayerRepository
GameManager.getInstance().getDatabase().villagers()  // VillagerRepository
GameManager.getInstance().getDatabase().quests()     // QuestRepository
GameManager.getInstance().getDatabase().crops()      // CropRepository
GameManager.getInstance().getDatabase().system()     // SystemRepository (logs, serverData)
```

Le SQL va **uniquement** dans les repositories. Jamais inline dans un Listener ou Command.

**Tous les repositories étendent `MLRepository`** — ne jamais copier le boilerplate `GameManager.getInstance().getScheduler().runTaskAsynchronously(...)`. Utiliser `runAsync(conn -> { ... }, ELogTag.XXX, "message")`.

**Debug SQL admin** : `GameManager.getInstance().getDatabase().executeRaw(sql)` — retourne une String formatée. Utilisé par `SqlCommand`. Ne pas appeler depuis du code métier.

Pour ajouter une migration : incrémenter `CURRENT_DB_VERSION` dans `SQLite`, ajouter
`createXxxTable()` dans `createTables()`, et ajouter `if (currentVersion < N) { ... }` dans
`runMigrations()`. Ne jamais appeler `setVersion()` manuellement — c'est géré par `MLSQLite.load()`.

---

## 9. Chargement YAML

```java
File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), "monfichier.yml");
YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
```

Les fichiers sont déployés depuis le JAR au démarrage via `MLResourceManager.deploy()` / `MLResourceManager.deployFolder()`
(appelés dans `Survi.onEnable()` avant `GameManager.init()`). Ne pas appeler `saveResource()` directement.

### Modifier un fichier YAML — workflow obligatoire

Toute modification d'un fichier YAML implique **trois mises à jour simultanées** :

1. **Le schéma JSON** (`schema/*.json`) — mettre à jour les champs ajoutés, supprimés ou modifiés.
2. **Le test de config associé** (`*ConfigTest.java`) — adapter les assertions aux nouvelles données.
3. **`SchemaGeneratorTest.java`** — si le changement touche un champ qui référence un enum Minecraft
   (`Material`, `EntityType`, `PotionEffectType`…) ou un enum du projet (`EWorld`, `EJob`, `ERole`…),
   ajouter la mise à jour correspondante dans `SchemaGeneratorTest`.

`SchemaGeneratorTest.updateSchemas()` regénère automatiquement les listes d'enums dans tous les schémas
à partir de l'API Bukkit réelle. Ne jamais écrire ces listes à la main — elles sont sensibles à la
version Paper et seront écrasées au prochain run du test de toute façon.

| Fichier YAML | Schéma | Test |
|---|---|---|
| `quests.yml` | `schema/quests.schema.json` | `MonstersConfigTest` |
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

Tags disponibles (`ELogTag implements ILogTag`) : `PLAYER`, `VILLAGER`, `QUEST`, `REPUTATION`,
`ITEM`, `ROLE`, `JOB`, `WORLD`, `SYSTEM`, `GRAVE`.

Voir section 2 MiubyLib pour le setup complet (`registerTags`, `initialize`, `MLLogPersistence`).

---

## 11. Mondes

`EWorld` : `VILLAGE`, `WILDERNESS`, `NETHER`, `END`, `ALL`

`ALL` = valeur spéciale "tous les mondes", à traiter selon contexte.

Convention de nommage des dimensions (Paper 26.1) :
- Wilderness : `Wilderness_N` (N = numéro de reset, stocké en DB)
- Nether associé : `Wilderness_N_nether`
- End associé : `Wilderness_N_the_end`
- Village : `Village` (permanent, ne reset jamais)

Accéder à un monde :

```java
WorldRegistry.get(EWorld.VILLAGE)       // MLWorld
WorldRegistry.get(player)              // MLWorld du joueur
WorldInitializer.loadOrCreate(name, env) // charge ou crée, jamais null (lance exception si échec)
```

---

## 12. Scheduler

```java
BukkitScheduler scheduler = GameManager.getInstance().getScheduler();
JavaPlugin plugin = GameManager.getInstance().getPlugin();

scheduler.runTaskLater(plugin, () -> { }, 20L);        // 1 seconde
scheduler.runTaskTimer(plugin, () -> { }, 0L, 20L);    // répété chaque seconde
scheduler.runTaskAsynchronously(plugin, () -> { });    // I/O, DB
// ou via MiubyLib :
MiubyLib.runLater(task, delay);
MiubyLib.runAsync(task);   // préférer MiubyLib.runAsync dans les nouveaux managers
```

Accès Bukkit (entités, worlds, inventaires) = **thread principal uniquement**.
DB = peut être async. Dans les repositories, toujours via `runAsync(...)` de `MLRepository`.

---

## 13. Blessings (effets villageois)

```java
public class MonEffet extends BlessingEffect {
    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) { ... }

    @Override
    public void resetEffect(VillagerLevel villager, AlphaPlayer player) {
        // implémenter si l'effet est persistant (unlock, maxHealth, etc.)
        // no-op par défaut suffit pour les effets one-shot
    }
}
```

Enregistrer dans `BlessingLoader` via la map `type → BlessingEffect`.

---

## 13b. Growth items

Les growth items sont des items qui accumulent des « uses » au fil des événements et gagnent
en puissance par paliers. La configuration est **entièrement en YAML** — aucun code Java
n'est nécessaire pour créer un nouvel item.

### Architecture

```
growth_items/<id>.yml       → GrowthItemFileConfig  (POJO SnakeYAML)
                                      ↓
                            GrowthItemLoader.loadAll()
                                      ↓
                            GrowthConfig / GrowthTier / PeriodicEffect  (types runtime)
                                      ↓
                            GrowthItemRegistry.register(id, config)
```

`GrowthItems.init()` est appelé par `GameManager` — il délègue entièrement à `GrowthItemLoader`.

### Ajouter un nouveau growth item

1. Créer `src/main/resources/growth_items/<mon_item>.yml` (valider contre `schema/growth-items-schema.json`)
2. Ajouter l'entrée dans `ECustomItem` (appeler `createGrowthItem(meta, "<MON_ITEM>")` avant le builder)
3. Ajouter la gestion de l'événement dans `GrowthItemListener` si le `eventType` est nouveau
4. Si l'item est en slot d'armure (HEAD, etc.) : appeler `GrowthItems.IncrementUsesFromHelmet()` au lieu de `IncrementUses()`

### Types d'effets YAML disponibles

| `type`            | Champs requis                                                     | Description |
|---|---|---|
| `name`            | `value`                                                           | Renomme l'item |
| `message`         | `value`                                                           | Envoie un message au joueur |
| `haste`           | `seconds`                                                         | Donne Haste II pendant N secondes |
| `add_enchantment` | `enchantment` (clé minecraft), `amount`                          | Ajoute N niveaux à l'enchantement |
| `set_attribute`   | `attribute`, `attributeValue`, `operation`, `slot`               | Remplace l'attribut (retire tous les anciens modifiers de cet attribut) |

### Types d'événements disponibles

| `eventType`             | Listener                     | Description |
|---|---|---|
| `BlockBreakEvent`       | `GrowthItemListener`         | Tout bloc cassé (item en main) |
| `OreBreakEvent`         | `GrowthItemListener`         | Minerais uniquement (item en casque) |
| `CropBreakEvent`        | `GrowthItemListener`         | Cultures uniquement (item en main) |

Pour ajouter un `eventType` : créer un `@EventHandler` dans `GrowthItemListener` avec le filtre approprié,
puis appeler `GrowthItems.IncrementUses(player, "MonNouvelEventType")` ou `IncrementUsesFromHelmet`.

### Clés PDC d'un growth item

| Clé NamespacedKey | Type | Contenu |
|---|---|---|
| `growth_id`    | `STRING`  | ID du growth item (ex. `GROWTH_CASQUE_MINEUR`) |
| `growth_uses`  | `INTEGER` | Nombre d'utilisations cumulées |
| `growth_tier`  | `INTEGER` | Palier actuel (0 = aucun palier atteint) |

---

## 14. Schémas JSON pour les YAML de contenu

Respecter le schéma correspondant quand on génère du contenu :

| Fichier YAML | Schéma |
|---|---|
| `quests.yml` | `schema/quests.schema.json` |
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
- Utiliser `GameManager.getInstance().getScheduler().runTaskAsynchronously(...)` dans un repository — utiliser `runAsync(...)` de `MLRepository`
- Passer uniquement `Connection` au constructeur d'un repository — toujours passer `(Connection, MLSQLite)` pour que `runAsync` fonctionne
- Appeler `Database.Request(...)` (supprimé) — utiliser `database.executeRaw(sql)` pour le debug admin

---

## 16. Fichiers clés par sous-système

| Sous-système | Fichiers à lire avant de modifier |
|---|---|
| Joueurs | `AlphaPlayer`, `AlphaPlayerFactory`, `PlayerPersistenceService`, `PlayerAttributeService` |
| Rôles | `ERole`, `RoleLoader`, `RoleManagementService`, `roles.yml` |
| Quêtes | `QuestManager`, `Quest`, `EQuestType`, `EQuestDifficulty`, `quests.yml` |
| Villageois | `VillagerFactory`, `VillagerLevel`, `BlessingLoader`, `villagers/*.yml` |
| Monstres | `MobLevelManager`, `MobTypeConfig`, `monsters.yml` |
| Mondes | `WorldInitializer`, `WorldLevelManager`, `WorldResetManager`, `EWorld` |
| Items | `ECustomItem`, `CustomItemBuilder`, `CustomRecipeFactory`, `recipes.yml` |
| Growth items | `GrowthItems`, `GrowthItemLoader`, `GrowthItemFileConfig`, `GrowthItemListener`, `GrowthItemRegistry`, `growth_items/*.yml` |
| Métiers | `EJob`, `JobLevelConfig`, `JobListener` |
| DB | `MLSQLite` + `MLRepository` (MiubyLib), `Database`, `SQLite`, repositories dans `system/database/repository/` |

---

## 17. Règles de performance

- Jamais de `.stream()` dans un EventHandler fréquent (damage, move, tick) —
  utiliser des boucles for ou des structures pré-calculées.
- Jamais de `Bukkit.getWorld(String)` / `Bukkit.getPlayer(UUID)` en hot path —
  toujours passer par `WorldRegistry` / `AlphaPlayer.get(uuid)`.
- Jamais de scan d'entités (`world.getEntitiesByType(...)`) dans un listener
  répétitif — utiliser un registry ou un cache local.
- **DB toujours async, sans exception.** Toute I/O base de données depuis n'importe quel
  contexte (listener, command, timer) doit passer par `runAsync` de `MLRepository`. La règle vaut
  aussi pour les events ponctuels (`onDailyReset`, `onPlayerQuit`…) — le thread principal
  ne doit jamais attendre SQLite.
- Ne pas allouer d'objets inutiles dans les listeners chauds (`new ArrayList<>()` à chaque event).
- `ignoreCancelled = true` sur tous les `@EventHandler` sauf cas explicite.
- **Pré-cacher les références stables dans les champs du listener.** Les appels
  `WorldRegistry.get(EWorld.XXX)`, `GameManager.getInstance().getXxxManager()`, etc. dans
  `onPlayerMove` ou `onEntityDamage` s'exécutent des milliers de fois par seconde — stocker
  ces références en champs privés `final`, initialisés dans le constructeur du listener.
  ```java
  // ❌ Interdit dans un handler chaud
  public void onPlayerMove(PlayerMoveEvent e) {
      MLWorld village = WorldRegistry.get(EWorld.VILLAGE); // lookup à chaque move
  }

  // ✓ Correct
  public class PlayerListener implements Listener {
      private final MLWorld villageWorld;
      private final MLWorld wildernessWorld;

      public PlayerListener() {
          this.villageWorld   = WorldRegistry.get(EWorld.VILLAGE);
          this.wildernessWorld = WorldRegistry.get(EWorld.WILDERNESS);
      }
  }
  ```
- **Jamais `getAlphaPlayers()` / `getAll()` dans un hot path.** Itérer le registry complet
  à chaque event de damage ou de move est équivalent à `world.getEntitiesByType()` — même
  interdiction. Pour des états rares (joueurs ayant un rôle spécifique, un flag actif…),
  maintenir un `Set<UUID>` dédié mis à jour sur l'event de changement d'état correspondant
  (`AlphaPlayerRoleChangeEvent`, etc.), pas recalculé à chaque event.
  ```java
  // ❌ Interdit
  for (AlphaPlayer p : factory.getAlphaPlayers()) { // scan complet à chaque damage
      if (p.getRole().type() == ERole.FEE) { ... }
  }

  // ✓ Correct — cache maintenu sur AlphaPlayerRoleChangeEvent
  private final Set<UUID> feePlayers = new HashSet<>();
  ```

---

## 18. Checklist — ajouter un villageois

1. `villagers/monvillageois.yml` (valider contre `schema/villagers-schema.json`)
2. Classe `MonVillager extends AVillager` avec `loadData` / `createDefaultData` / `saveData`
3. Données : classe `MonVillagerData extends MLVillagerData` si état custom
4. Enregistrement dans `VillagerFactory` (map `nameId → Constructor`)
5. Blessings éventuels → `BlessingLoader`
6. Log `MLLogManager` avec `ELogTag.VILLAGER` dans `onInitialized()`

---

## 19. Checklist — ajouter une feature gameplay

1. Config YAML + schéma JSON + test (voir workflow section 9)
2. `XxxConfig` POJO (chargé par le Loader)
3. `XxxLoader` qui lit le YAML et retourne les configs
4. `XxxManager` ou `XxxService` selon qu'il y a un état runtime
5. Listener fin qui délègue au service — jamais de logique métier dans le listener
6. Persistence en repository si données joueur concernées — étendre `MLRepository`
7. Logs via `MLLogManager` avec le bon `ELogTag`