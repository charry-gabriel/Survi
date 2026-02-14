# Survi

Plugin Minecraft hardcore survival avec système de progression par villageois, rôles, items verrouillés et mécaniques de difficulté.

## Vue d'ensemble

Survi transforme Minecraft en une expérience de survie difficile et progressive. Les joueurs doivent collecter des ressources pour débloquer des outils/armures via des villageois, gérer des rôles avec attributs spéciaux, et faire face à une difficulté croissante.

## Architecture du code

### Classe principale : GameManager

Le `GameManager` (singleton) gère toute l'initialisation et les composants du plugin :

```java
GameManager.getInstance().init(plugin);        // Init de base
GameManager.getInstance().initAfterWorldsLoad(); // Init complète après chargement des mondes
```

**Composants gérés :**
- Database (SQLite)
- VillagerFactory
- RoleFactory
- AlphaPlayerFactory
- LockedItemsFactory
- CustomRecipeFactory
- GrowthItems
- PlantedCropsManager
- TabDisplayManager
- PlayerAttributeService

### Initialisation (ordre important)

1. `onEnable()` : Enregistre les listeners et commandes
2. `GameManager.init()` : Init database + MiubyLib
3. `WorldFactory.initializeIfNeeded()` : Prépare les mondes
4. Après chargement des chunks → `initAfterWorldsLoad()` :
   - Init des mondes
   - Création des rôles et joueurs Alpha
   - Init des villageois
   - Enregistrement des recipes
   - Chargement des crops
   - Démarrage du timer

## Systèmes principaux

### 1. AlphaPlayer

Wrapper autour d'un joueur avec des données persistantes.

**Attributs importants :**
- `role` : Rôle principal
- `subRoles` : Liste de sous-rôles
- `mort` : Nombre de morts
- `success` : Nombre de succès
- `world` : Monde actuel (MLWorld)
- `alphaLife` : Gestion de la vie/santé
- `scoreboard` : Scoreboard personnalisé

**Méthodes clés :**
- `joinServer()` : Appelé à la connexion, setup tout le joueur
- `addRoleAttribute()` : Applique les attributs du rôle + sous-rôles
- `clearAllRoleAttributes()` : Retire tous les attributs de rôles
- `addAttribute(RoleAttribute)` : Ajoute un attribut avec modifier
- `gainOneSuccess(boolean)` : Ajoute un succès si défi réussi
- `addMort(int)` : Ajoute des morts

**Notes :**
- Les attributs de rôle sont world-specific (voir `EWorld`)
- Utilise des `AttributeModifier` avec NamespacedKey pour éviter les doublons
- La vie est gérée via `AlphaLife` qui recalcule la santé max selon succès/morts

### 2. Système de Rôles

Chaque joueur a 1 rôle principal + plusieurs sous-rôles possibles.

**Rôles disponibles (ERole) :**
- DRAGON, LOUP_GAROU, FEE, NAIN, GEANT
- COMBATANT, MINEUR, NOVICE
- ALCHIMISTE, ENCHANTEUR, FERMIER

**RoleAttribute :**
Définit un modificateur d'attribut pour un rôle :
- `attributeType` : Type d'attribut (MAX_HEALTH, ATTACK_DAMAGE, etc.)
- `value` : Valeur du modifier
- `operation` : ADD, MULTIPLY, REMOVE
- `world` : Monde où l'attribut s'applique (ALL, SPAWN, WILDERNESS, NETHER, END)

**Exemple de création de rôle :**
```java
RoleDefinition dragon = new RoleDefinition(ERole.DRAGON);
dragon.addAttribute(Attribute.MAX_HEALTH, 10, RoleAttribute.Operation.ADD, EWorld.ALL);
dragon.addAttribute(Attribute.ATTACK_DAMAGE, 0.5f, RoleAttribute.Operation.MULTIPLY, EWorld.NETHER);
```

**Changement de rôle :**
1. `clearAllRoleAttributes()` - Retire les anciens attributs
2. Changer le rôle
3. `addRoleAttribute()` - Applique les nouveaux
4. Event `AlphaPlayerRoleChangeEvent` est déclenché

### 3. Villageois (AVillager)

Les villageois ont des niveaux de progression avec tributs et récompenses.

**Structure d'un villageois :**
- Hérite de `MLVillager` (de MiubyLib)
- Config dans `/resources/villagers/*.yml`
- Système de niveaux avec tributs (items requis)
- Bénédictions (effets) appliquées aux joueurs

**Config villageois (YAML) :**
```yaml
name: "Nom du villageois"
type: PLAINS  # Type de villageois (biome)
profession: FARMER

levels:
  - name: "Niveau I"
    message: "Message au joueur"
    recap: "Résumé des récompenses"
    tribute:
      - material: DIRT
        amount: 64
    blessing:
      - type: unlock_tool
        value: STONE
      - type: max_health
        value: 2.0
```

**Types de blessings disponibles :**
- `unlock_tool` / `unlock_armor` : Débloque outils/armures
- `max_health` : Ajoute de la vie max
- `damage` : Modifie les dégâts
- `resistance` : Modifie la résistance
- `regen` : Régénération constante
- `item` : Donne un item
- `random_item` : Donne un item aléatoire
- `message` : Affiche un message
- `lock_world` / `limit_world` : Gère l'accès aux mondes
- `fly` : Active le vol
- `gamemode` : Change le gamemode
- `dispel` : Retire des morts

**VillagerLevel :**
Chaque niveau stocke :
- Tributs requis
- Blessings à appliquer
- Messages
- Cooldown (optionnel, en heures)

**Interaction :**
Click droit sur villageois → ouvre inventaire de tribut → donne items → level up

### 4. Items verrouillés (LockedItem)

Les outils et armures sont verrouillés par défaut et débloqués via villageois.

**Types d'items verrouillés :**

**Outils (LockedToolType) :**
- WOOD, STONE, IRON, GOLD, DIAMOND, NETHERITE

**Armures (LockedArmorType) :**
- LEATHER, CHAINMAIL, IRON, GOLD, DIAMOND, NETHERITE

**Fonctionnement :**
- Les items sont détectés via `ICustomItemMeta`
- Si non débloqué : item supprimé du craft/inventaire
- Déblocage via blessing `unlock_tool` ou `unlock_armor`
- État stocké en NBT sur l'item

**Listeners :**
- `ItemListener` : Vérifie crafts, drops, pickups
- Supprime automatiquement les items non autorisés

### 5. Growth Items

Items qui évoluent en fonction de leur utilisation.

**Concept :**
Un item commence faible et gagne des effets au fil du temps/utilisation.

**Structure :**
- `GrowthTier` : Palier d'évolution avec effets
- `ItemEffect` : Effet appliqué (enchant, nom, hâte, message)
- Config dans le code (voir `GrowthItems.java`)

**Types d'effets :**
- `AddEnchantmentItemEffect` : Ajoute un enchantement
- `NameItemEffect` : Change le nom
- `HasteItemEffect` : Donne hâte au mineur
- `MessageItemEffect` : Affiche un message

**Registry :**
```java
GrowthItemRegistry.register(Material.IRON_PICKAXE, List.of(
    new GrowthTier(0, List.of(...)),    // Tier 0
    new GrowthTier(100, List.of(...)),  // Tier 1 à 100 uses
    new GrowthTier(500, List.of(...))   // Tier 2 à 500 uses
));
```

**Listener :**
- `GrowthItemListener` : Track l'utilisation, upgrade automatique

### 6. Crops plantées (PlantedCrop)

Système pour tracker et gérer les crops plantées par les joueurs.

**PlantedCrop :**
```java
record PlantedCrop(UUID owner, Location location, Material crop)
```

**PlantedCropsManager :**
- Stocke en DB
- Vérifie la propriété lors de la récolte
- Empêche les autres joueurs de casser

**Listener :**
- `CropGrowthListener` : Track plantation et récolte

### 7. Mondes (MLWorld)

Gestion des différents mondes avec limites et restrictions.

**Mondes (EWorld) :**
- SPAWN : Monde de spawn sécurisé
- WILDERNESS : Monde principal de survie
- NETHER : Nether (débloqué progressivement)
- END : End (débloqué après nether)
- ALL : Wildcard pour attributs universels

**MLWorld (de MiubyLib) :**
- `spawnPoint` : Point de spawn
- `limit` : Rectangle 3D de limite (Rect)
- `isLocked` : Monde verrouillé ou non

**WorldListener :**
- Empêche entrée dans mondes locked
- Téléporte si hors limites
- Gère les morts dans différents mondes

### 8. Custom Recipes

Recipes Minecraft custom définies en YAML.

**Config (`recipes.yml`) :**
```yaml
recipes:
  - result:
      material: DIAMOND
      amount: 1
    ingredients:
      - material: COAL_BLOCK
        amount: 9
    type: SHAPED  # ou SHAPELESS
    shape:  # Si SHAPED
      - "CCC"
      - "CCC"
      - "CCC"
```

**CustomRecipeFactory :**
- Parse le YAML
- Crée les ShapedRecipe/ShapelessRecipe
- Enregistre dans Bukkit

### 9. Database (SQLite)

**Tables :**
- `alpha_players` : Données des joueurs (uuid, role, subroles, mort, success, etc.)

**PlayerColumn (enum) :**
Colonnes de la table joueurs

**Méthodes importantes :**
- `createAlphaPlayers()` : Charge tous les joueurs depuis DB
- `updatePlayer(uuid, column, value)` : Update une colonne
- `insertPlayer(uuid, name)` : Crée un nouveau joueur

**Pattern :**
Les données sont chargées au startup et updatées en temps réel.

### 10. Display (Tab et Scoreboard)

**TabDisplayManager :**
Gère l'affichage dans la tablist (header/footer).

**AlphaScoreboard :**
Scoreboard personnalisé par joueur avec teams colorées.

**AlphaTeam :**
Team pour colorer les noms des joueurs.

## Commandes

### /role <role> [player]
Change le rôle d'un joueur.

### /subrole <add|remove> <role> [player]
Ajoute/retire un sous-rôle.

### /sql <query>
Exécute une requête SQL (debug).

### /villager <create|teleport|reload> [name]
Gère les villageois.

### /customitem <item> [amount] [player]
Donne un custom item.

## Listeners

**ServerListener :**
- Init après chargement des mondes

**PlayerListener :**
- Join/quit
- Respawn
- Death (ajoute mort)
- Chat (format custom)

**AlphaPlayerListener :**
- Gère les events AlphaPlayer

**DamageListener :**
- Modifie les dégâts selon rôle
- Applique résistance/dégâts modifiers

**WorldListener :**
- Empêche accès mondes locked
- Téléporte si hors limites
- Change attributs selon monde

**ItemListener :**
- Vérifie items verrouillés
- Empêche craft/drop non autorisés

**GrowthItemListener :**
- Track utilisation
- Upgrade items

**CropGrowthListener :**
- Track plantation crops
- Vérifie propriété récolte

**VillagerListener :**
- Click sur villageois
- Ouvre GUI tribut

## Timer

Classe qui s'exécute périodiquement (toutes les X ticks).

**Fonctions :**
- Update le temps du monde
- Vérifie si c'est la nuit (`isNight`)
- Peut déclencher events périodiques

## Notes importantes

### Ordre d'initialisation critique

1. Database load
2. MiubyLib init
3. WorldFactory.initializeIfNeeded()
4. **ATTENDRE que les mondes soient chargés**
5. initAfterWorldsLoad()

### Gestion des attributs

Les attributs sont appliqués avec des `NamespacedKey` uniques :
```
Format: "world_roleId_attributeType"
Exemple: "wilderness_dragon_max_health"
```

Cela évite les doublons et permet de retirer spécifiquement un attribut.

### World-specific attributes

Les attributs de rôle peuvent être world-specific. Quand un joueur change de monde, il faut :
1. `clearAllRoleAttributes()` - Retire tous
2. `addRoleAttribute()` - Ré-applique pour le nouveau monde

Cela est géré automatiquement dans `WorldListener`.

### Villageois et persistence

Les villageois utilisent MiubyLib (MLVillager) qui gère :
- Sauvegarde de l'UUID et position
- Recherche automatique au chargement
- Retry si chunk pas chargé
- Recréation si introuvable après 10 tentatives

Les données sont dans `AlphaVillagerData` (location + UUID).

### Growth Items

Les growth items trackent leur utilisation via NBT :
```java
PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
int uses = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
```

À chaque utilisation, le plugin vérifie si un nouveau tier est atteint et upgrade l'item.

### Locked Items

Les items verrouillés ont une clé NBT `custom_item_id` :
```java
String itemId = pdc.get(keyId, PersistentDataType.STRING);
```

Le système vérifie cette clé pour identifier l'item et son unlock status.

## Dépendances

- **MiubyLib** : Lib custom pour villageois et mondes
- Bukkit/Paper API
- Lombok
- Kyori Adventure (text components)
- SQLite JDBC

## Configuration

**config.yml :**
Config générale du plugin (vide dans le code actuel, utilisé pour defaults)

**recipes.yml :**
Toutes les custom recipes

**villagers/*.yml :**
Un fichier par villageois avec leurs niveaux, tributs, blessings

## Structure des fichiers

```
Survi/
├── Commands.java              # Dispatcher de commandes
├── GameManager.java           # Singleton - gère tout
├── Survi.java                # Classe principale
├── Timer.java                # Timer périodique
├── crops/                    # Système de crops
├── database/                 # SQLite + queries
├── display/                  # Tab display
├── item/
│   ├── CustomRecipe*.java   # Recipes custom
│   ├── ECustomItem.java     # Enum items custom
│   ├── growth_item/         # Items évolutifs
│   └── locked_item/         # Items verrouillés
├── listener/                 # Tous les event listeners
├── player/
│   ├── AlphaPlayer.java     # Wrapper joueur
│   ├── AlphaLife.java       # Gestion vie
│   ├── AlphaScoreboard.java # Scoreboard
│   └── event/               # Events custom
├── role/
│   ├── ERole.java           # Enum des rôles
│   ├── Role.java            # Classe rôle
│   ├── RoleAttribute.java   # Attribut de rôle
│   └── RoleFactory.java     # Création rôles
├── villager/
│   ├── AVillager.java       # Classe villageois
│   ├── blessing/            # Tous les types de blessings
│   ├── VillagerLevel.java   # Niveau de villageois
│   └── VillagerFactory.java # Gère tous les villageois
└── world/
    ├── EWorld.java          # Enum des mondes
    └── WorldFactory.java    # Init mondes
```

## Tips pour revenir dans le code

### Pour ajouter un nouveau rôle :
1. Ajoute dans `ERole`
2. Crée `RoleDefinition` dans `RoleFactory`
3. Définis les attributs avec `RoleAttribute`

### Pour ajouter un villageois :
1. Crée `xxx.yml` dans `/resources/villagers/`
2. Définis levels avec tributs et blessings
3. Le villageois sera auto-loadé au démarrage

### Pour ajouter un blessing :
1. Crée classe extends `BlessingEffect`
2. Implémente `apply(AlphaPlayer)`
3. Ajoute le type dans le parser YAML

### Pour débugger un joueur :
```java
AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
// Voir role, attributs, mort, success, etc.
```

### Pour voir l'état d'un villageois :
```java
AVillager villager = VillagerRegistry.get("nom_id");
// Voir niveau actuel, tributs, etc.
```

### SQLite direct :
```sql
SELECT * FROM alpha_players;
```
Ou utilise `/sql <query>` in-game
