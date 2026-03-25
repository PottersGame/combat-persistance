package com.adam.mixin;

import com.adam.Combatpersistence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class CombatCommandMixin {

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void onHandleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
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
}