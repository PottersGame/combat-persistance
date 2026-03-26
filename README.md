# Combat Persistence 🛡️

A professional-grade, security-hardened **Fabric** mod for Minecraft that prevents combat logging by spawning a persistent NPC (Mannequin) when a player disconnects while in combat. This mod is designed for production servers where competitive integrity and security are paramount.

## 🚀 Features

*   **Combat Tagging**: Automatically tags players who hit or are hit by other players.
*   **Secure Persistent NPCs**: When a tagged player disconnects, a `Mannequin` NPC spawns at their exact location.
    *   **Dupe Protection**: Automatically clears both the player's inventory and the **cursor stack** (held items) on logout to prevent duplication glitches.
    *   **Visual Mirroring**: NPCs copy the player's profile (Skin), armor, and held items.
    *   **NBT-Backed Persistence**: NPCs store the player's UUID in their NBT, allowing the mod to identify them even if the chunk unloads or the server restarts.
*   **Offline Death Handling**: If the NPC is killed while the player is offline, the items drop on the ground and the player will die immediately upon rejoining the server.
*   **Integrated Secure Auth System**:
    *   **Session-Based Autologin**: Remembers your IP and UUID for 24 hours (cracked) or 30 days (premium), eliminating repetitive password entry.
    *   **Premium Verification**: Use `/premium` to verify your account with Mojang's API and get extended autologin benefits.
    *   **Thread-Safe Persistence**: Uses `ReentrantLock` to prevent data corruption during asynchronous saving.
    *   **Lobby Support**: Optionally teleports unauthenticated players to a safe lobby dimension.
*   **Command Blocking**: Configurable list of commands (like `/spawn`, `/tp`, `/home`) that are blocked while in combat.
*   **Server-Side Only**: Entirely server-side. No client-side installation required.

---

## 🛠️ Configuration

The configuration file is located at `config/combatpersistence.json`.

| Setting | Default | Description |
| :--- | :--- | :--- |
| `combatTagDurationSeconds` | 15 | How long the combat tag lasts. |
| `npcNamePrefix` | `§7[OFFLINE] §f` | Prefix for the NPC's name tag. |
| `playSpawnSound` | `true` | Whether to play a thunder sound when an NPC spawns. |
| `combatMessage` | `§c§lIN COMBAT: §f%s s remaining` | Action bar message for tagged players. |
| `blockedCommands` | `[...]` | Commands disabled during combat. |
| `enableAuth` | `true` | Toggle the built-in authentication system. |
| `sessionDurationHours` | 24 | Autologin window for standard (cracked) players. |
| `hideCoordinatesBeforeAuth`| `true` | Hides location and coordinates from unauthenticated players. |

---

## 💬 Commands

*   **/register <password> <confirmPassword>**: Register your account.
*   **/login <password>**: Login to your account.
*   **/premium**: Verify your premium status via Mojang API (grants 30-day autologin).
*   **/skin <name>**: Change your skin using a Mojang account name.

---

## 🔒 Security & Exploits Fixed

This mod has been rigorously audited to fix common vulnerabilities found in other combat/auth mods:
*   **Cursor Stack Dupe**: Players can no longer dupe items by holding them on their cursor during logout.
*   **Ghost Inventory**: Players can no longer keep their items if the server crashes before their inventory is cleared.
*   **Spoofing Protection**: Removed insecure IP-only autologin; implemented session secrets.
*   **Race Conditions**: Fixed async authentication race conditions that could lead to unauthorized access.
*   **Thread Corruption**: All persistent data saving is protected by locks to prevent file corruption during high server load.

---

## 📦 Installation

1.  Download the latest `.jar` file from the [Releases](https://github.com/PottersGame/combat-persistance/releases) page.
2.  Place it in your server's `mods` folder.
3.  Ensure you have the **Fabric API** installed.
4.  Restart your server.

## 🔨 Development

To build the mod from source:
1.  Clone the repository.
2.  Run `./gradlew build`.
3.  The jar will be located in `build/libs/`.

## 📜 License

This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.
