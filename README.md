# Survi

A custom Paper survival plugin for the TeamAlpha private server.

Survi replaces vanilla survival progression with an interconnected set of systems: role-based character identities, six parallel job tracks, NPC-driven content unlocks, leveled-up mobs, and server-wide challenges — all designed for a small group playing together long-term.

---

## World Structure

The server runs four persistent worlds, each with a distinct purpose:

| World | Type | Notes |
|---|---|---|
| **Village** | Hub | Safe zone. Houses all NPCs. Never resets. |
| **Wilderness** | Survival | Main resource world. Resets periodically. |
| **Nether** | Nether | Resets with Wilderness. Explorer radius applies (÷8). |
| **End** | End | Resets with Wilderness. No radius limit. |

The Wilderness, Nether and End reset on a configurable frequency (day count), deleting the worlds and regenerating them fresh. The Village is permanent.

The playable area of the Village starts small and expands through **Village Zone stages** — rectangular zones configured in `zone.yml` that grow as players unlock Villager milestones.

---

## Roles

Every player is assigned a **role** by an admin. Roles define character identity and carry YAML-driven gameplay attributes (max health, damage modifiers, resistances, etc.) loaded from `roles.yml`.

Players can also hold **sub-roles** alongside their main role. Role changes fire a `AlphaPlayerRoleChangeEvent` for other systems to react.

---

## Jobs

Every player holds all six jobs simultaneously, each leveling independently from 0 to 10. Job level is determined by reputation accumulated with that job's Trader NPC.

| Job | Passive effect |
|---|---|
| **Miner** | Ore drop multiplier. Progressive night vision and darkness depth thresholds in caves. |
| **Lumberjack** | Log drop multiplier. |
| **Farmer** | Crop-related farming bonuses. |
| **Enchanter** | Enchantment table XP cap. Max enchantment level. |
| **Fisherman** | Fishing-specific potion effects, passive buffs applied periodically. |
| **Explorer** | Wilderness radius limit per level. Nether limit = Wilderness ÷ 8. The End has no limit. |

Each job has its own YAML config (`jobs/<job>.yml`) for fine-tuning all numerical values.

---

## Global Rank

Separate from jobs, a **global rank** reflects the player's total reputation across all Traders combined. Tiers configured in `config.yml`. The rank is displayed in the tab list.

---

## Villagers & Blessings

The Village hosts several custom Mannequin NPCs. Two types exist:

### VillagerLevel NPCs

Level-up NPCs each have multiple levels, defined in their YAML. To advance a villager to its next level, players must physically bring **tribute items** (a configurable list of materials and quantities) and deposit them.

On each level-up, the villager applies one or more **Blessings** — server-wide effects that permanently alter the game state for all players. Examples of available blessing effects:

- `DISPEL` — reduces the accumulated death-life penalty for all players
- `MAX_HEALTH` — raises all players' maximum health
- `RESISTANCE` / `DAMAGE` — modifies server-wide combat modifiers
- `UNLOCK_TOOL` / `UNLOCK_ARMOR` — gates higher gear tiers (Wood → Stone → Copper → Iron → Gold → Diamond → Netherite)
- `WORLD_LEVEL` — increments global world difficulty (see below)
- `WORLD_RESET` — triggers an immediate Wilderness reset
- `LOCK_WORLD` / `LIMIT_WORLD` — restricts access to a dimension
- `REPUTATION` — grants job reputation to all players
- `ITEM` / `RANDOM_ITEM` — distributes items
- `POTION` — applies server-wide long-duration potion effects

Blessings are also the reward type used by quests.

### Trader NPCs

One Trader per job (Miner, Lumberjack, Farmer, Enchanter, Fisherman, Explorer). Players interact with a Trader to receive daily quests and to submit their completion — earning reputation toward that job's level. Each Trader has a configurable shop defined in `traders/<job>.yml`.

---

## Quests

### Individual Quests

Players receive daily quests from Traders. Each quest targets a specific action type — mine a block type, kill a mob, fish, breed animals, smelt items, craft, enchant, harvest crops, and more. Quests are filtered by job and difficulty tier.

A player's active quest progress is shown in the **action bar**. Quest history (total claimed, completion dates) is persisted in the database. Each daily reset (at a configurable real-world hour) unlocks 2 additional quest slots. Admins can grant extra global slots to everyone with a command.

### Global Quests

Admin-launched server-wide challenges. All players contribute to a shared goal simultaneously. Progress is tracked on a **boss bar** visible to everyone. On completion, all participants receive the configured rewards. Global quests have a time limit after which they expire.

Quest definitions for both types live in YAML and are validated at startup against JSON schemas.

---

## Items

### Locked Items

Tool and armor tiers above a certain level are **locked** and unusable until a VillagerLevel NPC unlocks them server-wide via a `UNLOCK_TOOL` or `UNLOCK_ARMOR` blessing. Equipped locked gear imposes a health malus, creating a natural progression gate around gear acquisition.

### Growth Items

Special items that evolve as the player uses them. Each growth item is defined in `growth_items/<item>.yml` with a set of **tiers**, each requiring a number of accumulated uses to unlock. On reaching a new tier, the item gains effects: enchantments, stat attributes, new names, potion applications on hit, fire damage, or haste bursts. Items track custom stats in their PersistentDataContainer (visited biomes, killed mob types, etc.).

### Custom Items & Recipes

Named custom items registered in `recipes.yml`, craftable via custom shaped or shapeless recipes.

---

## Life System

Each player has a dynamic max health driven by three independent tracks:

- **Death-life** — accumulates with every death, reducing max health. Capped by the global Dispel counter.
- **Success-life** — accumulates with server-wide successes, increasing max health.
- **Blessing-life** — a flat bonus applied by `MAX_HEALTH` blessings.

The **Dispel** mechanic is a global integer managed by the `DISPEL` blessing. It offsets the death-life malus: with enough Dispel accumulated through François, even a heavily-death-penalized player recovers their max health.

---

## World Level & Mob Scaling

The **world level** is a single integer persisted in the database that rises each time a VillagerLevel NPC grants a `WORLD_LEVEL` blessing. It controls two things:

- **Mob rarity** — higher world levels increase the chance of elite mob variants spawning (configurable base + per-level, with a cap).
- **Mob difficulty** — mobs spawning at higher world levels receive scaled HP, damage, and potion effects. Each world level tier maps to a range of mob levels; within that range, a weighted random draw favors lower-level mobs to keep elites rare.

Special cases: Creepers get scaled explosion radius and fuse time; the Ender Dragon receives the maximum level for the current tier.

---

## Other Systems

**Graves** — On death, a chest is placed at the player's location containing their full inventory. Graves persist across restarts (database-backed). Access to another player's grave requires the `survi.grave.bypass` permission.

**Crops** — Crops placed by players are tracked per-block in the database.

**Rain** — Vanilla weather is overridden in configured worlds (Wilderness by default). Rain episodes trigger on a configurable random schedule (duration, min/max cooldown).

**Time** — The Minecraft day/night cycle mirrors real-world time: sunrise and sunset hours are configurable. A daily reset fires at a configurable real-world hour (default 6:00), resetting daily quest allocations.

**Offline notifications** — Events that occurred while a player was offline are queued and delivered on their next login.

**Tab list** — Custom tab display showing each player's role, global rank, max health, and active quest progress.

---

## Admin Commands

All commands use the Brigadier API with server-side tab-completion.

| Command | Purpose |
|---|---|
| `/survi` | System utilities — reload, SQL debug, container management |
| `/player` | View and modify player data |
| `/role` / `/subrole` | Assign or change player roles |
| `/villager` | Spawn, move, and manage NPCs |
| `/quest` | Force-assign quests, add quest slots, manage quest state |
| `/globalquest` | Start and stop global quests |
| `/job` | View and manually adjust job reputation |
| `/blessing` | Trigger a blessing directly |
| `/world` | Force-reset worlds, teleport players between worlds |
| `/mob` | Inspect mob levels and debug mob stats |
| `/item` / `/growthitem` | Give custom and growth items |

---

## Configuration

All tuning values are externalized in YAML and hot-reloadable. Files are deployed from the JAR on first start and updated automatically when the plugin version changes.

| File | Content |
|---|---|
| `config.yml` | Time, combat modifiers, reputation ranks, job level thresholds, world-level scaling, explorer radius limits, rain parameters |
| `zone.yml` | Village zone stages (bounds, spawn, portal position per stage) |
| `roles.yml` | Per-role gameplay attributes |
| `jobs/<job>.yml` | Per-job numerical config (drop multipliers, cave Y thresholds, etc.) |
| `quests/<quest>.yml` | Individual quest definitions |
| `global_quests.yml` | Global quest definitions |
| `growth_items/<item>.yml` | Growth item tiers and effects |
| `villagers/<npc>.yml` | Villager levels, tributes, and blessings |
| `traders/<job>.yml` | Trader shop contents |
| `recipes.yml` | Custom crafting recipes |
| `monsters.yml` | Mob scaling config per entity type |
| `lang/fr.yml` | All player-facing messages (MiniMessage format) |

All YAML files are validated against JSON schemas on startup.

---

## Data

All server state is persisted in a single SQLite database (schema v15). Tables include: players, villager positions, crops, job reputation, active quests and history, trade history, tribute history, graves, and server-wide key-value data (world level, daily reset timestamp, dispel counter, zone start time, etc.).

---

## Requirements

- **Paper** 26.2
- **Java** 25
- **MiubyLib** v1.16