package com.adam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CombatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "combatpersistence.json");

    // Combat Settings
    public int combatTagDurationSeconds = 15;
    public String npcNamePrefix = "§7[OFFLINE] §f";
    public boolean playSpawnSound = true;
    public String combatMessage = "§c§lIN COMBAT: §f%s s remaining";
    public List<String> blockedCommands = new ArrayList<>();

    // Auth Settings
    public boolean enableAuth = true;
    public boolean forceAuthInOfflineMode = true;
    public int sessionDurationHours = 24; 
    public boolean hideCoordinatesBeforeAuth = true;
    public double lobbyX = 0, lobbyY = 1000, lobbyZ = 0; 
    public String lobbyDimension = "minecraft:overworld";
    public int authTimeoutSeconds = 60;

    public CombatConfig() {
        // Default blocked commands
        blockedCommands.add("/spawn");
        blockedCommands.add("/home");
        blockedCommands.add("/tp");
        blockedCommands.add("/tpa");
        blockedCommands.add("/warp");
        blockedCommands.add("/back");
    }

    public static CombatConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, CombatConfig.class);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to load combat config", e);
            }
        }
        
        CombatConfig config = new CombatConfig();
        save(config);
        return config;
    }

    public static void save(CombatConfig config) {
        // Wrap disk-writing in a background thread to prevent lag spikes
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save combat config in background", e);
            }
        });
    }
}