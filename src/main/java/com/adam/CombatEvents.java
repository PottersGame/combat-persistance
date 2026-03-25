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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatEvents {
    
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
                // Collect full inventory for persistence
                List<ItemStack> fullInv = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    fullInv.add(player.getInventory().getItem(i).copy());
                }

                CombatNPC npc = CombatNPC.spawn(player);
                // Save to disk via tracker
                tracker.addNPC(player.getUUID(), npc.getUUID(), fullInv, (ServerLevel) player.level());
                
                Combatpersistence.LOGGER.info("Player {} logged out in combat! Spawned NPC.", player.getName().getString());
            }
        });

        // The death of NPC is now handled by tracker.handleEntityDeath via Mixin
        // because standard events might miss it after a server restart.

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            if (tracker.checkAndClearOfflineDeath(player.getUUID())) {
                player.getInventory().clearContent();
                DamageSource generic = player.level().damageSources().generic();
                player.die(generic);
                Combatpersistence.LOGGER.info("Player {} logged in dead due to combat logging.", player.getName().getString());
            } else {
                UUID npcUuid = tracker.getNPCForPlayer(player.getUUID());
                if (npcUuid != null) {
                    // Remove NPC if it exists in the world
                    for (ServerLevel level : server.getAllLevels()) {
                        Entity npcEntity = level.getEntity(npcUuid);
                        if (npcEntity != null) {
                            npcEntity.discard();
                            break;
                        }
                    }
                    tracker.removeNPC(player.getUUID());
                    Combatpersistence.LOGGER.info("Player {} rejoined safely. Removed NPC.", player.getName().getString());
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