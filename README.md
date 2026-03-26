# Combat Persistence

Combat Persistence is a server-side Fabric mod for Minecraft that prevents combat logging by spawning a persistent NPC (Mannequin) when a player disconnects during a fight. It is built for production servers where competitive integrity and security are priority.

## Features

*   **Combat Tagging**: Automatically tags players when they engage in PvP.
*   **Persistent NPCs**: When a tagged player disconnects, a Mannequin NPC spawns at their location.
    *   **Anti-Dupe**: Clears the player's inventory and cursor stack on logout to prevent duplication exploits.
    *   **Visual Mirroring**: NPCs mirror the player's skin, armor, and held items.
    *   **Persistence**: NPCs store player UUIDs in NBT, surviving chunk unloads and server restarts.
*   **Offline Death**: If the NPC is killed, the player's items drop and they will die upon rejoining the server.
*   **Authentication System**:
    *   **Autologin**: Remembers IP and UUID for 24 hours (cracked) or 30 days (premium).
    *   **Premium Verification**: Use `/premium` to verify accounts via Mojang's API.
    *   **Lobby Support**: Teleports unauthenticated players to a safe lobby dimension.
*   **Command Blocking**: Configurable list of commands blocked while in combat.
*   **Server-Side**: No client-side installation required.

## Configuration

The configuration file is located at `config/combatpersistence.json`.

### Combat Settings
| Setting | Default | Description |
| :--- | :--- | :--- |
| `combatTagDurationSeconds` | 15 | Duration of the combat tag in seconds. |
| `npcNamePrefix` | `§7[OFFLINE] §f` | The prefix applied to the NPC's name tag. |
| `playSpawnSound` | `true` | Plays a thunder sound when an NPC spawns. |
| `combatMessage` | `...` | The action bar message shown to tagged players. Use `%s` for time remaining. |
| `blockedCommands` | `[...]` | List of commands disabled during combat (e.g., /spawn, /home). |

### Authentication Settings
| Setting | Default | Description |
| :--- | :--- | :--- |
| `enableAuth` | `true` | Enables the built-in authentication system. |
| `forceAuthInOfflineMode` | `true` | Requires authentication even if the server is in offline mode. |
| `sessionDurationHours` | 24 | How long an autologin session lasts for standard players. |
| `hideCoordinatesBeforeAuth`| `true` | Conceals the player's location from unauthenticated users. |
| `authTimeoutSeconds` | 60 | Time in seconds before an unauthenticated player is kicked. |
| `lobbyDimension` | `overworld` | The dimension where unauthenticated players are held. |
| `lobbyX, lobbyY, lobbyZ` | `0, 1000, 0` | The coordinates for the authentication lobby. |

## Commands

*   **/register <password> <confirmPassword>**: Register your account.
*   **/login <password>**: Login to your account.
*   **/premium**: Verify premium status via Mojang API.
*   **/skin <name>**: Change your skin using a Mojang account name.

## Security

This mod addresses several common vulnerabilities:
*   **Cursor Stack Dupe**: Prevents item duplication via cursor holding during logout.
*   **Ghost Inventory**: Ensures inventory state is cleared before the player fully disconnects.
*   **Spoofing Protection**: Uses session secrets rather than just IP-based autologin.
*   **Thread Safety**: Uses `ReentrantLock` and background saving to prevent data corruption.

## Support

If you find this mod useful, you can support development here:
[ko-fi.com/pottersgame](https://ko-fi.com/pottersgame)

## Installation

1.  Download the latest release from [GitHub](https://github.com/PottersGame/combat-persistance/releases).
2.  Place the `.jar` in your server's `mods` folder.
3.  Ensure **Fabric API** is installed.

## License

This project is licensed under the GPL-3.0 License.
