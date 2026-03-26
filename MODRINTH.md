# Combat Persistence

Combat Persistence is a server-side Fabric mod designed for competitive Minecraft servers. It prevents combat logging by spawning a vulnerable NPC whenever a player disconnects while in combat, ensuring that players cannot evade death by closing the game.

### Features

*   **Smart Combat Tagging**: Monitors PvP interactions and tags players for a configurable duration.
*   **Persistent NPCs**: When a tagged player disconnects, a Mannequin NPC spawns at their location.
    *   **Visual Mirroring**: NPCs copy the player's profile (Skin, Armor, Held Items).
    *   **Persistence**: NPCs are backed by NBT data, surviving chunk unloads and server restarts.
*   **Offline Death**: If an NPC is killed, the player's items drop and they will find themselves at the death screen upon rejoining.
*   **Advanced Anti-Dupe**: Prevents common duplication glitches by clearing both the inventory and the **cursor stack** immediately on disconnect.
*   **Secure Authentication**:
    *   **Session Management**: Remembers login state for up to 30 days for premium users.
    *   **Premium Verification**: Use `/premium` to link your account to Mojang's servers.
    *   **Safe Limbo**: Teleports unauthenticated players to a lobby to prevent world-leaking.
*   **Command Blocking**: Prevents players from using commands like `/tp` or `/home` while tagged.
*   **Server-Side Only**: No client installation required.

### Support

If you find this mod useful, you can support development here:
[ko-fi.com/pottersgame](https://ko-fi.com/pottersgame)

### Configuration

All settings can be customized in `config/combatpersistence.json`:
- `combatTagDurationSeconds`: The length of the combat tag.
- `blockedCommands`: List of blacklisted commands during combat.
- `enableAuth`: Toggle the built-in authentication system.
- `npcNamePrefix`: Custom label for offline players.
- `lobbyDimension` and `lobbyX, lobbyY, lobbyZ`: Set the location for unauthenticated players.

### Requirements

- **Fabric Loader**
- **Fabric API**
- **Minecraft 26.1+**
- **Java 25+**

---

[Source Code](https://github.com/PottersGame/combat-persistance) | [Issue Tracker](https://github.com/PottersGame/combat-persistance/issues) | [Support (Ko-fi)](https://ko-fi.com/pottersgame)
