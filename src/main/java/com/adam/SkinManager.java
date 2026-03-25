package com.adam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkinManager {

    /**
     * Fetches skin properties asynchronously from the Ashcon API.
     * This bypasses the need for the server to be in online mode.
     */
    public static CompletableFuture<Property> fetchSkin(String skinName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + skinName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    JsonObject response = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    if (response.has("textures") && response.getAsJsonObject("textures").has("raw")) {
                        JsonObject raw = response.getAsJsonObject("textures").getAsJsonObject("raw");
                        String value = raw.get("value").getAsString();
                        String signature = raw.get("signature").getAsString();
                        return new Property("textures", value, signature);
                    }
                } else {
                    Combatpersistence.LOGGER.warn("Failed to fetch skin for {} (HTTP {})", skinName, conn.getResponseCode());
                }
            } catch (Exception e) {
                Combatpersistence.LOGGER.error("Failed to fetch skin {} from Ashcon API", skinName, e);
            }
            return null;
        });
    }

    public static void applySkin(ServerPlayer player, String skinName) {
        MinecraftServer server = ((ServerLevel)player.level()).getServer();
        
        fetchSkin(skinName).thenAccept(skinProperty -> {
            if (skinProperty != null) {
                server.execute(() -> {
                    // Update player's local properties
                    GameProfile profile = player.getGameProfile();
                    profile.properties().removeAll("textures");
                    profile.properties().put("textures", skinProperty);
                    
                    // Refresh player for everyone
                    refreshPlayer(player);
                    Combatpersistence.LOGGER.info("Applied skin {} to player {}", skinName, player.getName().getString());
                });
            }
        });
    }

    private static void refreshPlayer(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel)player.level()).getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        
        ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        ClientboundPlayerInfoUpdatePacket addPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));

        for (ServerPlayer viewer : players) {
            viewer.connection.send(removePacket);
            viewer.connection.send(addPacket);
        }
    }
}