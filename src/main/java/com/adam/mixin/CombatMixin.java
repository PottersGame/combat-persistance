package com.adam.mixin;

import com.adam.Combatpersistence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ServerGamePacketListenerImpl.class, LivingEntity.class})
public class CombatMixin {

    // Mixin for Command Blocking (only applies to ServerGamePacketListenerImpl)
    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void onHandleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if ((Object)this instanceof ServerGamePacketListenerImpl handler) {
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

    // Mixin for Entity Death (only applies to LivingEntity)
    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity entity) {
            // Check if this entity is one of our NPCs
            Combatpersistence.tracker.handleEntityDeath(entity);
        }
    }
}