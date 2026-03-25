package com.adam.mixin;

import com.adam.Combatpersistence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class CombatDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Combatpersistence.tracker.handleEntityDeath(entity);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player) {
            if (Combatpersistence.config.enableAuth && !Combatpersistence.authManager.isAuthenticated(player)) {
                player.setDeltaMovement(0, 0, 0);
            }
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvulnerableTo(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayer player) {
            if (Combatpersistence.config.enableAuth && !Combatpersistence.authManager.isAuthenticated(player)) {
                cir.setReturnValue(true);
            }
        }
    }
}