# Architecture du plugin Survi

## Vue d'ensemble

Plugin Paper/Bukkit pour un serveur Minecraft Survie custom.

```
Survi (JavaPlugin)
 └─ GameManager          ← point d'entrée global (singleton)
     ├─ system/          ← infrastructure transverse (DB, config, logs, temps)
     ├─ player/          ← entité joueur + services + events
     ├─ role/            ← rôles et sous-rôles
     ├─ job/             ← métiers et réputation
     ├─ quest/           ← quêtes journalières
     ├─ villager/        ← villageois, traders, blessings
     ├─ mob/             ← niveaux des monstres
     ├─ world/           ← portails, reset, zones, culture
     ├─ item/            ← items custom, recettes, items de croissance
     ├─ grave/           ← tombes à la mort
     ├─ display/         ← tab list
     └─ listener/        ← événements Bukkit (délèguent aux services)
```

## Ordre d'initialisation obligatoire

`GameManager.init()` suit une séquence en 3 étapes garantie par `InitState` :

1. **`NOT_STARTED` → `DATABASE_READY`** — `initDatabase()` : SQLite chargé.
2. **`DATABASE_READY` → `FULLY_LOADED`** — `initWorlds()` : création des mondes si besoin.
3. **`initAfterWorldsLoad()`** — appelé par `WorldInitializer` une fois les mondes prêts :
   charge tout le reste (joueurs, villageois, items, quêtes, temps…).

Ne jamais appeler `initAfterWorldsLoad()` manuellement depuis `Survi.onEnable()`.

## Conventions de nommage

| Pattern              | Usage                                      |
|----------------------|--------------------------------------------|
| `E` prefix (ERole, EJob, EWorld) | Enums "type" utilisés comme clés |
| `Manager`            | Gère un état global + lifecycle (load/stop)|
| `Factory`            | Crée et indexe des entités (registry)      |
| `Service`            | Logique sans état propre                   |
| `Repository`         | Accès SQL (une table ≈ un repository)      |
| `Listener`           | Événements Bukkit — délèguent aux services |
| `Command`            | Commandes Brigadier — stateless            |
| `Config`             | POJO chargé depuis YAML                    |
| `Loader`             | Lit un fichier YAML et retourne des objets |

## Règles de dépendance

```
Listener → Service / Manager  ✓
Service  → Repository         ✓
AlphaPlayer → GameManager     ✓ (inévitable en Minecraft)
AlphaPlayer → VillagerFactory ✗ (éviter : couplage circulaire)
```

La méthode `AlphaPlayer.getReputation(String traderId)` est marquée
`@Deprecated(forRemoval = true)` car elle viole cette règle.
Migrer les appelants vers `getJobReputation(EJob)`.

## Ajouter un rôle

1. `ERole` — ajouter la valeur enum.
2. `RoleRegistry` — ajouter la `RoleDefinition` dans le constructeur.

Les rôles sont intentionnellement en Java (pas en YAML) pour bénéficier
de la vérification à la compilation sur les `Attribute` Bukkit.
