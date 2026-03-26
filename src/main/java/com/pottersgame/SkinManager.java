package com.pottersgame;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {

    private static final Map<String, Property> skinCache = new ConcurrentHashMap<>();

    /**
     * Fetches skin properties asynchronously from the Mojang API.
     * This bypasses the need for the server to be in online mode.
     */
    public static CompletableFuture<Property> fetchSkin(String identifier) {
        if (skinCache.containsKey(identifier)) {
            return CompletableFuture.completedFuture(skinCache.get(identifier));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuid = identifier;
                
                // 1. If identifier is a name, we must get the UUID first
                if (identifier.length() <= 16) {
                    URL nameUrl = URI.create("https://api.mojang.com/users/profiles/minecraft/" + identifier).toURL();
                    HttpURLConnection nameConn = (HttpURLConnection) nameUrl.openConnection();
                    if (nameConn.getResponseCode() == 200) {
                        JsonObject nameResp = JsonParser.parseReader(new InputStreamReader(nameConn.getInputStream())).getAsJsonObject();
                        uuid = nameResp.get("id").getAsString();
                    } else {
                        return null; // Player not found or rate limited
                    }
                }

                // 2. Fetch the signed profile from Mojang sessionserver
                URL url = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    JsonObject response = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    if (response.has("properties")) {
                        JsonArray props = response.getAsJsonArray("properties");
                        for (int i = 0; i < props.size(); i++) {
                            JsonObject prop = props.get(i).getAsJsonObject();
                            if (prop.get("name").getAsString().equals("textures")) {
                                String value = prop.get("value").getAsString();
                                String signature = prop.has("signature") ? prop.get("signature").getAsString() : "";
                                Property property = new Property("textures", value, signature);
                                skinCache.put(identifier, property);
                                return property;
                            }
                        }
                    }
                } else if (conn.getResponseCode() == 429) {
                    Combatpersistence.LOGGER.warn("Mojang API Rate Limit hit while fetching skin for {}", identifier);
                }
            } catch (Exception e) {
                Combatpersistence.LOGGER.error("Failed to fetch skin {} from Mojang API", identifier, e);
            }
            return null;
        });
    }

    private static MinecraftServer getServer(ServerPlayer player) {
        try {
            Field serverField = ServerPlayer.class.getDeclaredField("server");
            serverField.setAccessible(true);
            return (MinecraftServer) serverField.get(player);
        } catch (Exception e) {
            if (player.level() instanceof ServerLevel sl) {
                return sl.getServer();
            }
        }
        return null;
    }

    public static void applySkin(ServerPlayer player, String skinName) {
        MinecraftServer server = getServer(player);
        if (server == null) return;
        
        fetchSkin(skinName).thenAccept(skinProperty -> {
            if (skinProperty != null) {
                server.execute(() -> {
                    // Get the player's current GameProfile
                    GameProfile oldProfile = player.getGameProfile();
                    
                    // Create a new mutable Multimap
                    com.google.common.collect.Multimap<String, Property> properties = HashMultimap.create();
                    
                    // Copy existing properties from the old GameProfile
                    for (Map.Entry<String, Property> entry : oldProfile.properties().entries()) {
                        properties.put(entry.getKey(), entry.getValue());
                    }
                    
                    // Remove existing "textures" properties and add the new one
                    properties.removeAll("textures");
                    properties.put("textures", skinProperty);
                    
                    // Wrap the final multimap in an immutable PropertyMap
                    PropertyMap updatedProperties = new PropertyMap(properties);
                    
                    // Create a new GameProfile record
                    GameProfile newProfile = new GameProfile(oldProfile.id(), oldProfile.name(), updatedProperties);
                    
                    // Replace the GameProfile in the Player object using reflection
                    try {
                        Field profileField = Player.class.getDeclaredField("gameProfile");
                        profileField.setAccessible(true);
                        profileField.set(player, newProfile);
                    } catch (Exception e) {
                        Combatpersistence.LOGGER.error("Failed to swap GameProfile via reflection", e);
                    }
                    
                    // Refresh player for everyone
                    refreshPlayer(player);
                    Combatpersistence.LOGGER.info("Applied skin {} to player {}", skinName, player.getName().getString());
                });
            }
        });
    }

    private static void refreshPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return;
        MinecraftServer server = world.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        
        ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        ClientboundPlayerInfoUpdatePacket addPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));

        for (ServerPlayer viewer : players) {
            viewer.connection.send(removePacket);
            viewer.connection.send(addPacket);

            // If the viewer is the player themselves, we need a more forceful update
            if (viewer == player) {
                // Send a respawn packet with KEEP_ALL_DATA to force entity recreation without losing state
                player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(world), (byte) 3));
                
                // Teleport them to their current position to trigger the camera/position sync
                TeleportTransition transition = new TeleportTransition(
                    world,
                    player.position(),
                    player.getDeltaMovement(),
                    player.getYRot(),
                    player.getXRot(),
                    TeleportTransition.DO_NOTHING
                );
                player.teleport(transition);
                
                // Refresh abilities and inventory sync
                player.onUpdateAbilities();
                server.getPlayerList().sendPlayerPermissionLevel(player);
                server.getPlayerList().sendLevelInfo(player, world);
                server.getPlayerList().sendAllPlayerInfo(player);
                player.containerMenu.sendAllDataToRemote();
            } else if (viewer.level() == player.level()) {
                // Force recreation for other viewers to refresh the skin
                viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(player.getId()));
                viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(player, 0, player.blockPosition()));
                viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(player.getId(), player.getEntityData().getNonDefaultValues()));
            }
        }
    }
}
