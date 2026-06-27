package com.pottersgame.mixin;

import com.pottersgame.Combatpersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (Combatpersistence.config != null && Combatpersistence.config.disableEnderChests) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(Combatpersistence.config.enderChestDisabledMessage), true);
            }
            // Stop the menu from ever opening, on both client prediction and server.
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
