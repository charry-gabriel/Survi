# AI_DEV.md — Guide de développement Survi pour Claude

Ce fichier est destiné à Claude pour générer du code cohérent avec ce projet.
**Lire en entier avant de générer quoi que ce soit.**

---

## 0. Instructions pour Claude

- Ne jamais écrire de résumé de ce qui a été créé ou modifié. Fournir les fichiers directement, c'est suffisant.
- Mettre à jour `AI_DEV.md` et `ARCHITECTURE.md` si une modification touche grandement un système existant (nouveau sous-système, changement de convention, nouveau pattern d'architecture).

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

`AlphaPlayer.getReputation(String traderId)` est `@Deprecated(forRemoval = true)`.
Utiliser `getJobReputation(EJob)` à la place.

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

**Arguments custom disponibles** dans `system/command/argument/` :
`AlphaPlayerArgument`, `VillagerArgument`, `TraderArgument`, `RoleArgument`, `SubRoleArgument`,
`JobArgument`, `QuestArgument`, `CustomItemArgument`, `WorldArgument`.

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

SQLite via repositories :

```java
GameManager.getInstance().getDatabase().players()    // PlayerRepository
GameManager.getInstance().getDatabase().villagers()  // VillagerRepository
GameManager.getInstance().getDatabase().quests()     // QuestRepository
GameManager.getInstance().getDatabase().crops()      // CropRepository
GameManager.getInstance().getDatabase().system()     // SystemRepository (logs, serverData)
```

Le SQL va **uniquement** dans les repositories. Jamais inline dans un Listener ou Command.

---

## 9. Chargement YAML

```java
File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), "monfichier.yml");
YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
```

Les fichiers sont copiés depuis le JAR au démarrage via `YmlResourceManager.update()`.
Les sous-dossiers (`villagers/`, `traders/`) sont gérés via `updateFolderResources()` dans `Survi`.
Ne pas appeler `saveResource()` directement.

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

---

## 10. Logging

```java
LogManager.getInstance().log(Level.INFO,    LogManager.ETagLog.QUEST,    "Message");
LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.VILLAGER, "Message", exception);
```

Jamais `System.out.println` ni `plugin.getLogger().info(...)`.

Tags disponibles : `PLAYER`, `VILLAGER`, `QUEST`, `REPUTATION`, `ITEM`, `ROLE`, `JOB`,
`WORLD`, `SYSTEM`, `GRAVE`.

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
```

Accès Bukkit (entités, worlds, inventaires) = **thread principal uniquement**.
DB = peut être async.

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

---

## 15. Ce qu'il ne faut jamais faire

- Utiliser `§` ou `ChatColor` pour les textes
- Appeler `initAfterWorldsLoad()` manuellement (c'est `WorldInitializer` qui le déclenche)
- Accéder à `VillagerFactory` depuis `AlphaPlayer`
- Écrire du SQL hors d'un Repository
- `System.out.println` ou `plugin.getLogger()` — utiliser `LogManager`
- Créer un `new Manager()` hors de `GameManager.initAfterWorldsLoad()`
- Instancier un `MLVillager` avec `new` — toujours `MLVillager.spawn(() -> new ...)`
- Utiliser `player.sendMessage(String)` — toujours `player.sendMessage(Component)`
- Générer du code avec des APIs Bukkit deprecated dans Paper 26.1

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
| Métiers | `EJob`, `JobLevelConfig`, `JobListener` |
| DB | `Database`, `SQLite`, repositories dans `system/database/repository/` |

---

## 17. Règles de performance

- Jamais de `.stream()` dans un EventHandler fréquent (damage, move, tick) —
  utiliser des boucles for ou des structures pré-calculées.
- Jamais de `Bukkit.getWorld(String)` / `Bukkit.getPlayer(UUID)` en hot path —
  toujours passer par `WorldRegistry` / `AlphaPlayer.get(uuid)`.
- Jamais de scan d'entités (`world.getEntitiesByType(...)`) dans un listener
  répétitif — utiliser un registry ou un cache local.
- Les `runTaskTimer` répétitifs ne doivent jamais accéder à la DB — async si besoin.
- Ne pas allouer d'objets inutiles dans les listeners chauds (`new ArrayList<>()` à chaque event).
- `ignoreCancelled = true` sur tous les `@EventHandler` sauf cas explicite.

---

## 18. Checklist — ajouter un villageois

1. `villagers/monvillageois.yml` (valider contre `schema/villagers-schema.json`)
2. Classe `MonVillager extends AVillager` avec `loadData` / `createDefaultData` / `saveData`
3. Données : classe `MonVillagerData extends MLVillagerData` si état custom
4. Enregistrement dans `VillagerFactory` (map `nameId → Constructor`)
5. Blessings éventuels → `BlessingLoader`
6. Log `LogManager` avec `ETagLog.VILLAGER` dans `onInitialized()`

---

## 19. Checklist — ajouter une feature gameplay

1. Config YAML + schéma JSON + test (voir workflow section 9)
2. `XxxConfig` POJO (chargé par le Loader)
3. `XxxLoader` qui lit le YAML et retourne les configs
4. `XxxManager` ou `XxxService` selon qu'il y a un état runtime
5. Listener fin qui délègue au service — jamais de logique métier dans le listener
6. Persistence en repository si données joueur concernées
7. Logs via `LogManager` avec le bon `ETagLog`