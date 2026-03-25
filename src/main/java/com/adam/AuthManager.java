package com.adam;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File AUTH_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "auth_data.json");
    private static final File LOC_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pending_locations.json");

    private Map<UUID, UserData> users = new ConcurrentHashMap<>();
    private final Set<UUID> authenticatedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Map<UUID, OriginalLocation> preAuthLocations = new ConcurrentHashMap<>();

    public AuthManager() {
        loadUsers();
        loadLocations();
    }

    public static class UserData {
        public String passwordHash;
        public String lastIp;
        public long lastLoginTime;
        public String customSkin;

        public UserData(String hash, String ip) {
            this.passwordHash = hash;
            this.lastIp = ip;
            this.lastLoginTime = System.currentTimeMillis();
        }
    }

    public static class OriginalLocation {
        public double x, y, z;
        public float yaw, pitch;
        public String dimension;

        public OriginalLocation(ServerPlayer player) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
            this.dimension = player.level().dimension().identifier().toString();
        }
    }

    public boolean isRegistered(UUID uuid) {
        return users.containsKey(uuid);
    }

    public boolean isAuthenticated(ServerPlayer player) {
        return authenticatedPlayers.contains(player.getUUID());
    }

    public void register(ServerPlayer player, String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        UserData data = new UserData(hash, getIp(player));
        users.put(player.getUUID(), data);
        authenticatedPlayers.add(player.getUUID());
        saveUsers();
    }

    public boolean login(ServerPlayer player, String password) {
        UserData data = users.get(player.getUUID());
        if (data != null && BCrypt.checkpw(password, data.passwordHash)) {
            data.lastIp = getIp(player);
            data.lastLoginTime = System.currentTimeMillis();
            authenticatedPlayers.add(player.getUUID());
            saveUsers();
            return true;
        }
        return false;
    }

    public boolean checkAutoLogin(ServerPlayer player) {
        UserData data = users.get(player.getUUID());
        if (data == null) return false;

        String currentIp = getIp(player);
        long hoursSinceLastLogin = (System.currentTimeMillis() - data.lastLoginTime) / (1000 * 60 * 60);
        
        if (currentIp.equals(data.lastIp) && hoursSinceLastLogin < Combatpersistence.config.sessionDurationHours) {
            authenticatedPlayers.add(player.getUUID());
            return true;
        }
        return false;
    }

    // This MUST be called on the Server Main Thread
    public void onAuthenticated(ServerPlayer player) {
        player.setNoGravity(false);
        player.setInvulnerable(false);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        
        // This MUST run before restoring location, in case they died in a different dimension.
        CombatEvents.handlePendingDeath(player);
        
        restoreLocation(player);
    }

    public void logout(ServerPlayer player) {
        authenticatedPlayers.remove(player.getUUID());
    }

    public void saveLocation(ServerPlayer player) {
        if (!preAuthLocations.containsKey(player.getUUID())) {
            preAuthLocations.put(player.getUUID(), new OriginalLocation(player));
            saveLocations();
        }
    }

    private void restoreLocation(ServerPlayer player) {
        OriginalLocation loc = preAuthLocations.remove(player.getUUID());
        if (loc != null) {
            saveLocations();
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(loc.dimension));
            ServerLevel currentLevel = (ServerLevel) player.level();
            ServerLevel targetDim = currentLevel.getServer().getLevel(dimKey);
            if (targetDim != null) {
                TeleportTransition transition = new TeleportTransition(
                    targetDim, 
                    new Vec3(loc.x, loc.y, loc.z), 
                    Vec3.ZERO, 
                    loc.yaw, 
                    loc.pitch, 
                    TeleportTransition.DO_NOTHING
                );
                player.teleport(transition);
            }
        }
    }

    public void setCustomSkin(UUID uuid, String skinName) {
        UserData data = users.get(uuid);
        if (data != null) {
            data.customSkin = skinName;
            saveUsers();
        }
    }

    public String getCustomSkin(UUID uuid) {
        UserData data = users.get(uuid);
        return (data != null) ? data.customSkin : null;
    }

    private String getIp(ServerPlayer player) {
        String addr = player.connection.getRemoteAddress().toString();
        if (addr.startsWith("/")) addr = addr.substring(1);
        return addr.split(":")[0];
    }

    private void loadUsers() {
        if (AUTH_FILE.exists()) {
            try (FileReader reader = new FileReader(AUTH_FILE)) {
                Type type = new TypeToken<ConcurrentHashMap<UUID, UserData>>(){}.getType();
                Map<UUID, UserData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) users = loaded;
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to load auth data", e);
            }
        }
    }

    private void saveUsers() {
        Map<UUID, UserData> copy = new HashMap<>(users);
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(AUTH_FILE)) {
                GSON.toJson(copy, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save auth data in background", e);
            }
        });
    }

    private void loadLocations() {
        if (LOC_FILE.exists()) {
            try (FileReader reader = new FileReader(LOC_FILE)) {
                Type type = new TypeToken<ConcurrentHashMap<UUID, OriginalLocation>>(){}.getType();
                Map<UUID, OriginalLocation> loaded = GSON.fromJson(reader, type);
                if (loaded != null) preAuthLocations = loaded;
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to load pending locations", e);
            }
        }
    }

    private void saveLocations() {
        Map<UUID, OriginalLocation> copy = new HashMap<>(preAuthLocations);
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(LOC_FILE)) {
                GSON.toJson(copy, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save pending locations in background", e);
            }
        });
    }
}