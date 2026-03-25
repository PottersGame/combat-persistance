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

public class CombatTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "combat_persistence_data.json");

    private final Map<UUID, Long> combatEndTimes = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveNPCData> activeNPCs = new ConcurrentHashMap<>();
    private Set<UUID> offlineDeaths = new HashSet<>();

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

    public void markOfflineDeath(UUID playerUuid) {
        offlineDeaths.add(playerUuid);
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

    public void handleEntityDeath(Entity entity) {
        UUID entityUuid = entity.getUUID();
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
                        .ifPresent(stack -> entity.spawnAtLocation(world, stack));
                } catch (Exception e) {
                    Combatpersistence.LOGGER.error("Failed to parse NPC inventory item string", e);
                }
            }
        }
    }

    private void saveData() {
        // Prepare data for saving
        Set<UUID> deathsCopy = new HashSet<>(offlineDeaths);
        Map<UUID, ActiveNPCData> npcsCopy = new HashMap<>(activeNPCs);
        
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                JsonObject root = new JsonObject();
                root.add("offlineDeaths", GSON.toJsonTree(deathsCopy));
                root.add("activeNPCs", GSON.toJsonTree(npcsCopy));
                GSON.toJson(root, writer);
            } catch (IOException e) {
                Combatpersistence.LOGGER.error("Failed to save combat data in background", e);
            }
        });
    }

    private void loadData() {
        if (DATA_FILE.exists()) {
            try (FileReader reader = new FileReader(DATA_FILE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("offlineDeaths")) {
                    Type setType = new TypeToken<HashSet<UUID>>(){}.getType();
                    offlineDeaths = GSON.fromJson(root.get("offlineDeaths"), setType);
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
        activeNPCs.entrySet().removeIf(entry -> {
            UUID playerUuid = entry.getKey();
            Long endTime = combatEndTimes.get(playerUuid);
            if (endTime != null && now >= endTime) {
                Entity npc = world.getEntity(entry.getValue().npcUuid);
                if (npc != null) npc.discard();
                combatEndTimes.remove(playerUuid);
                return true;
            }
            return false;
        });
        combatEndTimes.entrySet().removeIf(entry -> now >= entry.getValue() && !activeNPCs.containsKey(entry.getKey()));
    }
}