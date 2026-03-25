package com.adam;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatEvents {
    
    private static final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public static void register(CombatTracker tracker, CombatConfig config) {
        
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer victim) {
                if (!Combatpersistence.authManager.isAuthenticated(victim)) return false;

                Entity attackerEntity = source.getEntity();
                
                if (attackerEntity instanceof ServerPlayer attacker && Combatpersistence.authManager.isAuthenticated(attacker)) {
                    if (victim != attacker && !victim.getAbilities().instabuild && !attacker.getAbilities().instabuild) {
                        tracker.tag(victim, config.combatTagDurationSeconds);
                        tracker.tag(attacker, config.combatTagDurationSeconds);
                    }
                }
            }
            if (entity instanceof CombatNPC victimNpc) {
                Entity attackerEntity = source.getEntity();
                if (attackerEntity instanceof ServerPlayer attacker && Combatpersistence.authManager.isAuthenticated(attacker)) {
                    if (!attacker.getAbilities().instabuild) {
                        tracker.tag(attacker, config.combatTagDurationSeconds);
                    }
                } else if (attackerEntity instanceof ServerPlayer) {
                    return false; 
                }
            }
            return true; 
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer sp) {
                if (!Combatpersistence.authManager.isAuthenticated(sp)) return InteractionResult.FAIL;
                if (entity instanceof CombatNPC) return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayer sp) {
                if (!Combatpersistence.authManager.isAuthenticated(sp)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer sp) {
                if (!Combatpersistence.authManager.isAuthenticated(sp)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player instanceof ServerPlayer sp) {
                if (!Combatpersistence.authManager.isAuthenticated(sp)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID uuid = player.getUUID();
            if (Combatpersistence.authManager.isAuthenticated(player) && tracker.isInCombat(player)) {
                List<ItemStack> fullInv = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    fullInv.add(player.getInventory().getItem(i).copy());
                }
                CombatNPC npc = CombatNPC.spawn(player);
                tracker.addNPC(uuid, npc.getUUID(), fullInv, (ServerLevel) player.level());
            }
            Combatpersistence.authManager.logout(player);
            joinTimes.remove(uuid);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID uuid = player.getUUID();
            joinTimes.put(uuid, System.currentTimeMillis());
            
            boolean wasOfflineKilled = tracker.checkAndClearOfflineDeath(uuid);
            if (wasOfflineKilled) {
                player.getInventory().clearContent();
                // tracker.markOfflineDeath already adds to tracker's pending deaths
            }

            if (config.enableAuth && (!server.usesAuthentication() || !config.forceAuthInOfflineMode)) {
                if (Combatpersistence.authManager.checkAutoLogin(player)) {
                    player.sendSystemMessage(Component.literal("§aWelcome back! Auto-logged in via IP."));
                    
                    // Successful Auto-login
                    Combatpersistence.authManager.onAuthenticated(player);
                    
                    String skin = Combatpersistence.authManager.getCustomSkin(uuid);
                    SkinManager.applySkin(player, skin != null ? skin : player.getName().getString());
                    
                    cleanUpNpc(player, tracker, server);
                } else {
                    player.setNoGravity(true);
                    player.setInvulnerable(true);

                    if (config.hideCoordinatesBeforeAuth) {
                        Combatpersistence.authManager.saveLocation(player);
                        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(config.lobbyDimension));
                        ServerLevel lobbyLevel = server.getLevel(dimKey);
                        if (lobbyLevel != null) {
                            TeleportTransition transition = new TeleportTransition(
                                lobbyLevel, 
                                new Vec3(config.lobbyX, config.lobbyY, config.lobbyZ), 
                                Vec3.ZERO, 0, 0, 
                                TeleportTransition.DO_NOTHING
                            );
                            player.teleport(transition);
                        }
                    }
                    
                    if (Combatpersistence.authManager.isRegistered(uuid)) {
                        player.sendSystemMessage(Component.literal("§6Please log in using /login <password>"));
                    } else {
                        player.sendSystemMessage(Component.literal("§6Please register using /register <password> <confirmPassword>"));
                    }
                }
            } else {
                cleanUpNpc(player, tracker, server);
                // If no auth, handle pending death immediately
                handlePendingDeath(player);

                // If this is an offline server, apply the skin (online servers do this automatically)
                if (!server.usesAuthentication()) {
                    String skin = Combatpersistence.authManager.getCustomSkin(uuid);
                    SkinManager.applySkin(player, skin != null ? skin : player.getName().getString());
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                boolean authenticated = Combatpersistence.authManager.isAuthenticated(player);

                if (config.enableAuth && !authenticated) {
                    Long joinTime = joinTimes.get(uuid);
                    if (joinTime != null && (now - joinTime) > (config.authTimeoutSeconds * 1000L)) {
                        player.connection.disconnect(Component.literal("Login timeout!"));
                        continue;
                    }

                    if (server.getTickCount() % 100 == 0) {
                        if (Combatpersistence.authManager.isRegistered(uuid)) {
                            player.sendSystemMessage(Component.literal("§c§lAUTH: §fUse /login <password>"), true);
                        } else {
                            player.sendSystemMessage(Component.literal("§c§lAUTH: §fUse /register <pwd> <pwd>"), true);
                        }
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

    public static void handlePendingDeath(ServerPlayer player) {
        if (Combatpersistence.tracker.isPendingDeath(player.getUUID())) {
            player.getInventory().clearContent();
            player.setHealth(0.0f);
            player.die(player.level().damageSources().generic());
            Combatpersistence.tracker.removePendingDeath(player.getUUID());
        }
    }

    private static void cleanUpNpc(ServerPlayer player, CombatTracker tracker, net.minecraft.server.MinecraftServer server) {
        UUID npcUuid = tracker.getNPCForPlayer(player.getUUID());
        if (npcUuid != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity npcEntity = level.getEntity(npcUuid);
                if (npcEntity != null) {
                    npcEntity.discard();
                    break;
                }
            }
            tracker.removeNPC(player.getUUID());
        }
    }
}