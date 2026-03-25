package com.adam;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Combatpersistence implements ModInitializer {
    public static final String MOD_ID = "combatpersistence";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static CombatConfig config;
    public static final CombatTracker tracker = new CombatTracker();
    public static final AuthManager authManager = new AuthManager();
    
    // Globally accessible set of players waiting to be killed on rejoin
    public static final Set<UUID> pendingJoinDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Combat Persistence Mod...");

        config = CombatConfig.load();

        CombatEvents.register(tracker, config);
        AuthCommands.register();
        
        LOGGER.info("Combat tag duration is set to {} seconds.", config.combatTagDurationSeconds);
    }
}