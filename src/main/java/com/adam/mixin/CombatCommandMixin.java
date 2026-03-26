package com.adam.mixin;

import com.adam.Combatpersistence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

@Mixin(ServerGamePacketListenerImpl.class)
public class CombatCommandMixin {

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onHandleContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        if (Combatpersistence.config.enableAuth && !Combatpersistence.authManager.isAuthenticated(handler.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onHandlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        if (Combatpersistence.config.enableAuth && !Combatpersistence.authManager.isAuthenticated(handler.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void onHandleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        boolean authenticated = Combatpersistence.authManager.isAuthenticated(handler.player);
        
        // 1. Auth Blocking
        if (!authenticated) {
            String command = packet.command().toLowerCase();
            if (!command.startsWith("login") && !command.startsWith("register")) {
                handler.player.sendSystemMessage(Component.literal("§cYou must log in to use commands!"));
                ci.cancel();
                return;
            }
            return; // Allow login/register
        }

        // 2. Combat Blocking (Existing feature)
        if (Combatpersistence.tracker.isInCombat(handler.player)) {
            String command = "/" + packet.command();
            for (String blocked : Combatpersistence.config.blockedCommands) {
                if (command.startsWith(blocked + " ") || command.equalsIgnoreCase(blocked)) {
                    handler.player.sendSystemMessage(Component.literal("§cYou cannot use this command while in combat!"));
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onHandleChat(ServerboundChatPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        if (!Combatpersistence.authManager.isAuthenticated(handler.player)) {
            handler.player.sendSystemMessage(Component.literal("§cYou must log in to chat!"));
            ci.cancel();
        }
    }
}