# Combat Persistence

A professional, production-ready **Fabric** mod for Minecraft 26.1 (and above) that prevents combat logging by spawning a persistent NPC (Mannequin) when a player disconnects while in combat.

This repository is also configured as an **AI-First Minecraft Modding Template**. It includes a suite of custom Python tools to accelerate AI-driven and human-driven mod development by completely eliminating JSON boilerplate and Mixin mapping lookups.

---

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

---

## 🤖 AI-Driven Development Tools (`.gemini/`)

This repository ships with a custom set of CLI tools designed specifically for Minecraft/Fabric modding. They are meant to be used by AI assistants (like Gemini CLI or GitHub Copilot) or by human developers to bypass the most tedious parts of modding.

### Setup
If you want to use the placeholder texture generator, install the Python requirements:
```bash
pip install -r .gemini/requirements.txt
```

To initialize or update the mapping database, run the custom gradle task:
```bash
.\gradlew.bat geminiClassDb
```

### Available Tools

1. **Class & Method Lookup (`mc-lookup.py`)**
   Never guess a method descriptor again. This tool reads directly from the Fabric `compileClasspath`.
   * `python .gemini/mc-lookup.py search "Player"` -> Finds class paths.
   * `python .gemini/mc-lookup.py inspect "net.minecraft...Player"` -> Dumps bytecode/signatures.
   * `python .gemini/mc-lookup.py mixin "Player" "tick"` -> Auto-generates a ready-to-paste `@Mixin` class with perfect descriptors!

2. **JSON Scaffolding (`mc-scaffold.py`)**
   Eliminate JSON boilerplate.
   * `python .gemini/mc-scaffold.py create-item "ruby_sword"` -> Generates item models, translates names, registers to `en_us.json`.
   * `python .gemini/mc-scaffold.py create-block "ruby_ore"` -> Generates blockstates, models, loot tables, and translations.

3. **Recipe Generation (`mc-recipe.py`)**
   * `python .gemini/mc-recipe.py shaped ...` -> Auto-builds a crafting table JSON.
   * *Supports shaped, shapeless, and smelting/blasting.*

4. **Tag Generation (`mc-tag.py`)**
   * `python .gemini/mc-tag.py blocks "mineable/pickaxe" "mymod:ruby_ore"` -> Safely appends to existing Minecraft/Fabric tags without wiping them out.

5. **Placeholder Textures (`mc-texture.py`)**
   * `python .gemini/mc-texture.py item "ruby_sword" --color "#FF0000"` -> Procedurally generates 16x16 pixel-art placeholders instantly directly into your `assets/` folder.

6. **Crash Log Translator (`mc-translate.py`)**
   * `python .gemini/mc-translate.py crash-log.txt` -> Converts obfuscated `class_1234` and `method_5678` names back into readable Yarn mappings by hooking into your local Gradle cache.

**See `GEMINI.md` for AI-specific instructions.**

---

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

1.  Download the latest release.
2.  Place the `.jar` file in your server's `mods` folder.
3.  Ensure you have the **Fabric API** installed.
4.  Restart your server.

## 🔨 Development

To build the mod from source:

1.  Clone the repository.
2.  Run `./gradlew build` (or `.\gradlew.bat build` on Windows).
3.  The compiled jar will be in `build/libs/`.

## 📜 License

This project is licensed under the GPL3 License - see the [LICENSE](LICENSE) file for details.
