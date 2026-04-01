package com.pottersgame;

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
import java.util.concurrent.locks.ReentrantLock;

public class CombatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "combatpersistence.json");

    // Combat Settings
    public int combatTagDurationSeconds = 15;
    public String npcNamePrefix = "§7[OFFLINE] §f";
    public boolean playSpawnSound = true;
    public String combatMessage = "§c§lIN COMBAT: §f%s s remaining";
    public String npcDeathBroadcast = "§c[PvP] §7%s combat logged and was slain!";
    public String inventoryRestoredMessage = "§aYour inventory has been restored.";
    public String loginTimeoutMessage = "§cLogin timeout!";
    public String registerPrompt = "§6Please register using /register <password> <confirmPassword>";
    public String loginPrompt = "§6Please log in using /login <password>";
    public String registerSuccess = "§aSuccessfully registered and logged in!";
    public String loginSuccess = "§aSuccessfully logged in!";
    public String alreadyLoggedIn = "§cYou are already logged in!";
    public String alreadyRegistered = "§cYou are already registered!";
    public String passwordMismatch = "§cPasswords do not match!";
    public String incorrectPassword = "§cIncorrect password!";
    public String authRequiredForCommand = "§cYou must log in to use commands!";
    public String authRequiredForChat = "§cYou must log in to chat!";
    public String authRequiredForSkin = "§cYou must log in to change your skin!";
    public String skinAppliedMessage = "§aApplied skin: %s";
    public List<String> blockedCommands = new ArrayList<>();

    // Auth Settings
    public boolean enableAuth = true;
    public boolean forceAuthInOfflineMode = true;
    public int sessionDurationHours = 24; 
    public boolean hideCoordinatesBeforeAuth = true;
    public double lobbyX = 0, lobbyY = 1000, lobbyZ = 0; 
    public String lobbyDimension = "minecraft:overworld";
    public int authTimeoutSeconds = 60;

    private static final ReentrantLock saveLock = new ReentrantLock();

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
            saveLock.lock();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save combat config in background", e);
            } finally {
                saveLock.unlock();
            }
        }, Combatpersistence.IO_EXECUTOR);
    }
}
