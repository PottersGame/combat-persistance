package com.adam;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkinManager {

    public static void applySkin(ServerPlayer player, String skinName) {
        MinecraftServer server = ((ServerLevel)player.level()).getServer();
        
        CompletableFuture.runAsync(() -> {
            try {
                // Fetch Mojang Profile
                var profileOpt = server.services().profileResolver().fetchByName(skinName);
                if (profileOpt.isPresent()) {
                    GameProfile mojangProfile = profileOpt.get();
                    ProfileResult result = server.services().sessionService().fetchProfile(mojangProfile.id(), true);
                    
                    if (result != null && result.profile() != null) {
                        GameProfile finalProfile = result.profile();
                        
                        server.execute(() -> {
                            // Update player's local properties
                            player.getGameProfile().properties().clear();
                            player.getGameProfile().properties().putAll(finalProfile.properties());
                            
                            // Refresh player for everyone
                            refreshPlayer(player);
                            Combatpersistence.LOGGER.info("Applied skin {} to player {}", skinName, player.getName().getString());
                        });
                    }
                }
            } catch (Exception e) {
                Combatpersistence.LOGGER.error("Failed to fetch skin {} for {}", skinName, player.getName().getString(), e);
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