# AI Minecraft Modding Instructions

This repository is configured for rapid AI-driven development of a Minecraft Fabric Mod. As an AI agent working in this repository, you **MUST** follow the instructions and workflows outlined in this document.

## Mod Details
- **Mod Loader:** Fabric
- **Minecraft Version:** 26.1 (Latest)
- **Mappings:** Yarn / Intermediary

## AI Toolchain & Workflows
To avoid hallucinating JVM bytecode, guessing JSON structures, or writing tedious boilerplate, you must utilize the custom `.gemini/` Python scripts built into this repository.

### 1. Class Lookup & Mixin Generation (`mc-lookup.py`)
**Never guess method descriptors or class paths when writing mixins.** Use the lookup tool.

*   **Search for a class:**
    ```bash
    python .gemini/mc-lookup.py search "Player"
    ```
*   **Inspect a class (View bytecode/methods):**
    ```bash
    python .gemini/mc-lookup.py inspect "net.minecraft.world.entity.player.Player"
    ```
*   **Generate a Mixin Stub:**
    ```bash
    python .gemini/mc-lookup.py mixin "Player" "tick"
    ```
    *Use this generated stub directly in your Java code to ensure `@Inject` method descriptors are perfectly accurate.*

*   **Update Database:** If dependencies or the Minecraft version change in `build.gradle`, run `python .gemini/mc-lookup.py update` to refresh the local class database.

### 2. JSON Data Scaffolding (`mc-scaffold.py`)
Do not manually write standard Block or Item JSON files. 

*   **Create an Item:**
    ```bash
    python .gemini/mc-scaffold.py create-item "ruby_sword"
    ```
    *This generates the item model and updates `lang/en_us.json`.*

*   **Create a Block:**
    ```bash
    python .gemini/mc-scaffold.py create-block "ruby_ore"
    ```
    *This generates the blockstate, block model, item block model, a basic loot table, and updates `lang/en_us.json`.*

### 3. Recipe Generation (`mc-recipe.py`)
Do not manually write recipe JSONs.

*   **Shaped Recipe:**
    ```bash
    python .gemini/mc-recipe.py shaped "ruby_sword" "mymod:ruby_sword" --pattern " # " " # " " S " --keys "#=mymod:ruby" "S=minecraft:stick"
    ```
*   **Shapeless Recipe:**
    ```bash
    python .gemini/mc-recipe.py shapeless "ruby_block" "mymod:ruby_block" --ingredients "mymod:ruby" "mymod:ruby"
    ```
*   **Smelting Recipe:**
    ```bash
    python .gemini/mc-recipe.py smelt "ruby_from_ore" "mymod:ruby" "mymod:ruby_ore" --method smelting --exp 1.5 --time 200
    ```

### 4. Tag Generation (`mc-tag.py`)
Use this to append items/blocks to tags (like `mineable/pickaxe` or custom tags). It safely appends to existing files instead of overwriting them.

*   **Add block to pickaxe mineable:**
    ```bash
    python .gemini/mc-tag.py blocks "mineable/pickaxe" "mymod:ruby_ore" "mymod:ruby_block"
    ```
*   **Add item to common swords tag:**
    ```bash
    python .gemini/mc-tag.py items "swords" "mymod:ruby_sword"
    ```

### 5. Placeholder Textures (`mc-texture.py`)
When scaffolding new items, immediately generate a placeholder texture so the game doesn't show the missing texture block.

*   **Generate an Item Texture:**
    ```bash
    python .gemini/mc-texture.py item "ruby_sword" --color "#FF0000"
    ```
*   **Generate a Block Texture:**
    ```bash
    python .gemini/mc-texture.py block "ruby_ore" --color "#FF0000"
    ```

### 6. Crash Log Translation (`mc-translate.py`)
If the user provides a crash log with obfuscated names (like `class_123` or `method_456`), save the log to a text file and translate it.

*   **Translate a log:**
    ```bash
    python .gemini/mc-translate.py crash-log.txt
    ```

## Core Directives for the AI
1.  **Always Verify Mappings:** If you are unsure of a class name or method signature, search it using `mc-lookup.py` rather than guessing.
2.  **Scaffold First:** Use the scaffolding Python scripts to lay down the boilerplate, then manually modify the generated JSON files if custom logic is required.
3.  **Compile & Test:** Whenever you change Java code, always run `.\gradlew.bat build` to ensure the mod compiles correctly.
4.  **Stay Context-Aware:** Before starting a complex task, map out the workspace to understand existing classes in `src/main/java`.