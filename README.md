# Combat Persistence

A professional, production-ready **Fabric** mod for Minecraft 26.1 (and above) that prevents combat logging by spawning a persistent NPC (Mannequin) when a player disconnects while in combat.

## 🚀 Features

*   **Combat Tagging**: Automatically tags players who hit or are hit by other players.
*   **Persistent NPCs**: When a tagged player disconnects, a `Mannequin` NPC spawns at their exact location.
    *   NPCs mirror the player's skin, armor, and held items.
    *   NPCs carry the player's full inventory.
*   **Offline Death Handling**: If the NPC is killed while the player is offline:
    *   The player's entire inventory drops on the ground.
    *   The player will die immediately upon rejoining the server.
    *   **Ghost State Fix**: Uses a delayed death system to ensure rejoining players are properly synchronized before dying.
*   **Command Blocking**: Configurable list of commands (like `/spawn`, `/tp`, `/home`) that are blocked while in combat.
*   **Production Ready Persistence**: NPC data and offline death records are saved to disk (`config/combat_persistence_data.json`), surviving server restarts.
*   **Server-Side Only**: This mod is entirely server-side. Players do not need to install it to see the combat timers or experience the effects.
*   **Visual & Audio Feedback**:
    *   Action Bar countdown timer for tagged players.
    *   Thunder sound effect when an NPC spawns.
    *   Configurable NPC name prefixes.

## 🛠️ Configuration

The configuration file is located at `config/combatpersistence.json`.

```json
{
  "combatTagDurationSeconds": 15,
  "npcNamePrefix": "§7[OFFLINE] §f",
  "playSpawnSound": true,
  "combatMessage": "§c§lIN COMBAT: §f%s s remaining",
  "blockedCommands": [
    "/spawn",
    "/home",
    "/tp",
    "/tpa",
    "/warp",
    "/back"
  ]
}
```

## 📦 Installation

1.  Download the latest release from [GitHub/Modrinth].
2.  Place the `.jar` file in your server's `mods` folder.
3.  Ensure you have the **Fabric API** installed.
4.  Restart your server.

## 🔨 Development

To build the mod from source:

1.  Clone the repository.
2.  Run `./gradlew build`.
3.  The compiled jar will be in `build/libs/`.

## 📜 License

This project is licensed under the GPL License - see the [LICENSE](LICENSE) file for details.
