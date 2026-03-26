# Combat Persistence 🛡️

**Combat Persistence** is a professional-grade, server-side Fabric mod designed for competitive Minecraft servers. It prevents "combat logging" by spawning a vulnerable NPC whenever a player disconnects while in combat, ensuring that players cannot evade death by simply closing the game.

### 🌟 Key Features

*   **Smart Combat Tagging**: Monitors player interactions and tags those in combat. Configurable duration and action bar feedback.
*   **Persistent NPCs**: disconnects while tagged spawn a `Mannequin` NPC at the player's exact spot.
    *   NPCs mirror the player's visual appearance (Armor, Skins, Held Items).
    *   NPCs are backed by **NBT data**, meaning they survive chunk unloads and server restarts.
*   **Reliable Offline Deaths**: If your NPC is killed while you are offline, you will find yourself at the death screen upon rejoining. No "ghost items" or survival glitches.
*   **Advanced Dupe Protection**: Rigorously tested logic that clears both the inventory and the **cursor stack** (carried items) immediately on disconnect to eliminate duplication exploits.
*   **Built-in Secure Authentication**:
    *   **Session Management**: Remembers your login state (24-hour session for cracked, **30 days for premium**).
    *   **Premium Verification**: Use `/premium` to link your account to Mojang's official servers for enhanced security and convenience.
    *   **Safe Limbo**: Teleports unauthenticated players to a lobby and hides their coordinates to prevent world-leaking.
*   **Command Blocking**: Prevent players from running `/tp`, `/home`, or `/spawn` to escape a fight.
*   **No Client Needed**: This is a 100% server-side mod. Your players don't need to install anything!

### 🔧 Configuration

All settings can be customized in `config/combatpersistence.json`:
- `combatTagDurationSeconds`: Customize the length of the tag.
- `blockedCommands`: Add your own custom commands to the blacklist.
- `enableAuth`: Toggle the entire authentication system on or off.
- `npcNamePrefix`: Change how offline players are labeled in-game.

### 📦 Requirements

- **Fabric Loader**
- **Fabric API**
- **Minecraft 26.1+**
- **Java 25+**

### 💎 Why Combat Persistence?

Unlike many other combat-log solutions, Combat Persistence is built with an **exploit-first** mindset. We have manually patched duplication glitches involving cursor stacks, inventory ghosting, and authentication bypasses that plague other similar mods.

---

[Source Code](https://github.com/PottersGame/combat-persistance) | [Issue Tracker](https://github.com/PottersGame/combat-persistance/issues) | [GPL-3.0 License](https://github.com/PottersGame/combat-persistance/blob/main/LICENSE)
