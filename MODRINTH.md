# Combat Persistence

Combat Persistence is a server-side Fabric mod designed to prevent "combat logging" by replacing disconnecting players with a vulnerable NPC. If the NPC is killed while the player is offline, the player's inventory is dropped, and they will die immediately upon rejoining the server.

### Features
- **Mannequin NPCs**: When a player in combat disconnects, a `Mannequin` entity is spawned at their location.
- **Visual Mirroring**: NPCs copy the player's Skin (via SkinManager/Mojang API), Armor, and Held Items.
- **Inventory Protection/Risk**: The NPC stores the player's full inventory. If the NPC survives, the player is safe; if it is killed, the items drop on the ground.
- **Offline Death Handling**: Players who lose their NPC while offline are marked in `combat_persistence_data.json` and killed instantly upon rejoining.
- **Command Blocking**: Configurable list of commands (e.g., `/spawn`, `/tp`, `/home`) that are blocked while in combat.
- **Integrated Auth System**: Includes an optional authentication system with features like lobby teleportation and coordinate hiding before login.
- **Server-Side Only**: No client-side installation is required.

### How It Works
1. **Combat Tagging**: Attacking or being attacked by a player tags you for a set duration (default: 15 seconds).
2. **Persistence**: If you disconnect while tagged, a Mannequin NPC spawns with your health and inventory.
3. **The Outcome**:
    - If the NPC survives the remaining tag time, it is discarded, and you login normally later.
    - If the NPC is killed, it drops your items, and you will see a death screen on your next login.

### Configuration (`config/combatpersistence.json`)
- `combatTagDurationSeconds`: How long the combat tag lasts (Default: 15).
- `npcNamePrefix`: Prefix for the NPC's name tag (Default: `§7[OFFLINE] §f`).
- `playSpawnSound`: Whether to play a thunder sound when an NPC spawns.
- `blockedCommands`: List of commands to disable during combat.
- `enableAuth`: Toggle the built-in authentication system.
- `authTimeoutSeconds`: Time allowed to login before being kicked.

### Data Storage
The mod saves active NPCs and pending offline deaths to `config/combat_persistence_data.json` to ensure combat states persist through server restarts.

### Requirements
- **Fabric Loader**
- **Fabric API**
- **Minecraft 26.1+** (Java 25)

### License
Licensed under GPL-3.0. Source code available on [GitHub](https://github.com/PottersGame/combat-persistance).
