package com.adam;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class CombatTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "combat_persistence_data.json");

    private final Map<UUID, Long> combatEndTimes = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveNPCData> activeNPCs = new ConcurrentHashMap<>();
    private Set<UUID> offlineDeaths = new HashSet<>();
    private final Set<UUID> pendingJoinDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private final ReentrantLock saveLock = new ReentrantLock();

    public CombatTracker() {
        loadData();
    }

    public static class ActiveNPCData {
        public UUID playerUuid;
        public UUID npcUuid;
        public List<String> inventoryNbt; 

        public ActiveNPCData(UUID playerUuid, UUID npcUuid, List<ItemStack> inventory, ServerLevel world) {
            this.playerUuid = playerUuid;
            this.npcUuid = npcUuid;
            this.inventoryNbt = new ArrayList<>();
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) {
                    ItemStack.CODEC.encodeStart(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack)
                        .resultOrPartial(e -> Combatpersistence.LOGGER.error("Failed to encode item: {}", e))
                        .ifPresent(tag -> this.inventoryNbt.add(tag.toString()));
                }
            }
        }
    }

    public void tag(ServerPlayer player, int durationSeconds) {
        combatEndTimes.put(player.getUUID(), System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public boolean isInCombat(ServerPlayer player) {
        Long endTime = combatEndTimes.get(player.getUUID());
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public Map<UUID, Long> getTaggedPlayers() {
        return combatEndTimes;
    }

    public void addNPC(UUID playerUuid, UUID npcUuid, List<ItemStack> inventory, ServerLevel world) {
        activeNPCs.put(playerUuid, new ActiveNPCData(playerUuid, npcUuid, inventory, world));
        saveData();
    }

    public UUID getNPCForPlayer(UUID playerUuid) {
        ActiveNPCData data = activeNPCs.get(playerUuid);
        return data != null ? data.npcUuid : null;
    }

    public void removeNPC(UUID playerUuid) {
        activeNPCs.remove(playerUuid);
        saveData();
    }

    public ActiveNPCData getAndRemoveNPCData(UUID playerUuid) {
        ActiveNPCData data = activeNPCs.remove(playerUuid);
        if (data != null) {
            saveData();
        }
        return data;
    }

    public void markOfflineDeath(UUID playerUuid) {
        offlineDeaths.add(playerUuid);
        pendingJoinDeaths.add(playerUuid);
        activeNPCs.remove(playerUuid);
        saveData();
    }

    public boolean checkAndClearOfflineDeath(UUID playerUuid) {
        if (offlineDeaths.remove(playerUuid)) {
            saveData();
            return true;
        }
        return false;
    }

    public boolean isPendingDeath(UUID uuid) {
        return pendingJoinDeaths.contains(uuid);
    }

    public void removePendingDeath(UUID uuid) {
        if (pendingJoinDeaths.remove(uuid)) {
            saveData();
        }
    }

    public void handleEntityDeath(Entity entity) {
        UUID entityUuid = entity.getUUID();
        
        // 1. Check if it's a CombatNPC with its own stored inventory (BEST persistence)
        if (entity instanceof CombatNPC npc) {
            List<ItemStack> inventory = npc.getStoredInventory();
            if (!inventory.isEmpty()) {
                for (ItemStack stack : inventory) {
                    entity.spawnAtLocation((ServerLevel) entity.level(), stack);
                }
            } else {
                // Fallback to memory cache only if NPC's NBT was empty for some reason
                for (ActiveNPCData data : activeNPCs.values()) {
                    if (data.npcUuid.equals(entityUuid)) {
                        dropNpcInventory(entity, data);
                        break;
                    }
                }
            }
            
            if (npc.originalPlayerUuid != null) {
                markOfflineDeath(npc.originalPlayerUuid);
            }
            return;
        }

        // 2. Generic fallback for non-CombatNPC entities (shouldn't happen for our NPCs anymore)
        for (ActiveNPCData data : activeNPCs.values()) {
            if (data.npcUuid.equals(entityUuid)) {
                dropNpcInventory(entity, data);
                markOfflineDeath(data.playerUuid);
                return;
            }
        }
    }

    private void dropNpcInventory(Entity entity, ActiveNPCData data) {
        if (entity.level() instanceof ServerLevel world) {
            for (String nbtStr : data.inventoryNbt) {
                try {
                    CompoundTag tag = net.minecraft.nbt.TagParser.parseCompoundFully(nbtStr);
                    ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
                        .resultOrPartial(e -> Combatpersistence.LOGGER.error("Failed to decode item: {}", e))
                        .ifPresent(stack -> {
                             // Use spawnAtLocation for safety
                             entity.spawnAtLocation(world, stack);
                        });
                } catch (Exception e) {
                    Combatpersistence.LOGGER.error("Failed to parse NPC inventory item string", e);
                }
            }
        }
    }

    private void saveData() {
        saveData(true);
    }

    public void saveData(boolean async) {
        // Prepare data for saving
        Set<UUID> deathsCopy = new HashSet<>(offlineDeaths);
        Set<UUID> pendingCopy = new HashSet<>(pendingJoinDeaths);
        Map<UUID, ActiveNPCData> npcsCopy = new HashMap<>(activeNPCs);
        
        Runnable saveAction = () -> {
            saveLock.lock();
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                JsonObject root = new JsonObject();
                root.add("offlineDeaths", GSON.toJsonTree(deathsCopy));
                root.add("pendingJoinDeaths", GSON.toJsonTree(pendingCopy));
                root.add("activeNPCs", GSON.toJsonTree(npcsCopy));
                GSON.toJson(root, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save combat data", e);
            } finally {
                saveLock.unlock();
            }
        };

        if (async) {
            CompletableFuture.runAsync(saveAction);
        } else {
            saveAction.run();
        }
    }

    public void stop() {
        saveData(false);
    }

    private void loadData() {
        if (DATA_FILE.exists()) {
            try (FileReader reader = new FileReader(DATA_FILE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("offlineDeaths")) {
                    Type setType = new TypeToken<HashSet<UUID>>(){}.getType();
                    offlineDeaths = GSON.fromJson(root.get("offlineDeaths"), setType);
                }
                if (root.has("pendingJoinDeaths")) {
                    Type setType = new TypeToken<HashSet<UUID>>(){}.getType();
                    Set<UUID> loaded = GSON.fromJson(root.get("pendingJoinDeaths"), setType);
                    if (loaded != null) pendingJoinDeaths.addAll(loaded);
                }
                if (root.has("activeNPCs")) {
                    Type mapType = new TypeToken<ConcurrentHashMap<UUID, ActiveNPCData>>(){}.getType();
                    Map<UUID, ActiveNPCData> loaded = GSON.fromJson(root.get("activeNPCs"), mapType);
                    if (loaded != null) activeNPCs.putAll(loaded);
                }
            } catch (Exception e) {
                Combatpersistence.LOGGER.error("Failed to load combat data", e);
            }
        }
    }

    public void tick(ServerLevel world) {
        long now = System.currentTimeMillis();

        // We iterate over combatEndTimes to see what has expired
        for (Iterator<Map.Entry<UUID, Long>> it = combatEndTimes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now >= entry.getValue()) {
                UUID playerUuid = entry.getKey();
                ActiveNPCData npcData = activeNPCs.get(playerUuid);
                if (npcData != null) {
                    // Discard the NPC entity if it exists
                    for (ServerLevel level : world.getServer().getAllLevels()) {
                        Entity npc = level.getEntity(npcData.npcUuid);
                        if (npc != null) {
                            npc.discard();
                            break;
                        }
                    }
                    // Note: We DO NOT remove from activeNPCs here! 
                    // We want the inventory to persist until the player rejoins.
                }
                it.remove(); // Remove from combatEndTimes
            }
        }
    }

    public void restoreInventory(ServerPlayer player) {
        ActiveNPCData data = activeNPCs.remove(player.getUUID());
        if (data == null) return;

        saveData();

        if (player.level() instanceof ServerLevel world) {
            for (String nbtStr : data.inventoryNbt) {
                try {
                    CompoundTag tag = net.minecraft.nbt.TagParser.parseCompoundFully(nbtStr);
                    ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
                        .resultOrPartial(e -> Combatpersistence.LOGGER.error("Failed to decode restored item: {}", e))
                        .ifPresent(stack -> {
                             if (!player.getInventory().add(stack)) {
                                 player.drop(stack, false);
                             }
                        });
                } catch (Exception e) {
                    Combatpersistence.LOGGER.error("Failed to parse restored inventory item string", e);
                }
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aYour inventory has been restored."));
        }
    }
    }