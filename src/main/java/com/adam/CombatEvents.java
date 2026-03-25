package com.adam;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatEvents {
    
    // Players who joined and need to be killed on the next tick to avoid "ghost state" bugs
    private static final Set<UUID> pendingJoinDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(CombatTracker tracker, CombatConfig config) {
        
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer attacker) {
                if (victim != attacker && !victim.getAbilities().instabuild && !attacker.getAbilities().instabuild) {
                    tracker.tag(victim, config.combatTagDurationSeconds);
                    tracker.tag(attacker, config.combatTagDurationSeconds);
                }
            }
            if (entity instanceof CombatNPC victimNpc && source.getEntity() instanceof ServerPlayer attacker) {
                if (!attacker.getAbilities().instabuild) {
                    tracker.tag(attacker, config.combatTagDurationSeconds);
                }
            }
            return true; 
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            if (tracker.isInCombat(player)) {
                List<ItemStack> fullInv = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    fullInv.add(player.getInventory().getItem(i).copy());
                }

                CombatNPC npc = CombatNPC.spawn(player);
                tracker.addNPC(player.getUUID(), npc.getUUID(), fullInv, (ServerLevel) player.level());
                
                Combatpersistence.LOGGER.info("Player {} logged out in combat! Spawned NPC.", player.getName().getString());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID uuid = player.getUUID();
            
            if (tracker.checkAndClearOfflineDeath(uuid)) {
                player.getInventory().clearContent();
                pendingJoinDeaths.add(uuid);
                Combatpersistence.LOGGER.info("Player {} marked for delayed death due to combat logging.", player.getName().getString());
            } else {
                UUID npcUuid = tracker.getNPCForPlayer(uuid);
                if (npcUuid != null) {
                    for (ServerLevel level : server.getAllLevels()) {
                        Entity npcEntity = level.getEntity(npcUuid);
                        if (npcEntity != null) {
                            npcEntity.discard();
                            break;
                        }
                    }
                    tracker.removeNPC(uuid);
                    Combatpersistence.LOGGER.info("Player {} rejoined safely. Removed NPC.", player.getName().getString());
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!pendingJoinDeaths.isEmpty()) {
                Iterator<UUID> iterator = pendingJoinDeaths.iterator();
                while (iterator.hasNext()) {
                    UUID uuid = iterator.next();
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    // Check if player exists and is connected
                    if (player != null) {
                        player.getInventory().clearContent();
                        DamageSource generic = player.level().damageSources().generic();
                        player.setHealth(0.0f);
                        player.die(generic);
                        iterator.remove();
                        Combatpersistence.LOGGER.info("Executed delayed death for player {}.", player.getName().getString());
                    }
                }
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            tracker.tick(world);
            
            Map<UUID, Long> tagged = tracker.getTaggedPlayers();
            if (!tagged.isEmpty()) {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : tagged.entrySet()) {
                    ServerPlayer player = (ServerPlayer) world.getPlayerInAnyDimension(entry.getKey());
                    if (player != null && player.level() == world) {
                        long remainingMillis = Math.max(0, entry.getValue() - now);
                        if (remainingMillis > 0) {
                            int seconds = (int) Math.ceil(remainingMillis / 1000.0);
                            String msg = String.format(config.combatMessage, seconds);
                            player.sendSystemMessage(Component.literal(msg), true);
                        }
                    }
                }
            }
        });
    }
}