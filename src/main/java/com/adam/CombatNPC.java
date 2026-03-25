package com.adam;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CombatNPC extends Mannequin {

    public final UUID originalPlayerUuid;
    private final String originalPlayerName;
    private final List<ItemStack> extraInventory = new ArrayList<>();
    private boolean hasDropped = false;

    public CombatNPC(ServerLevel world, UUID originalPlayerUuid, String originalPlayerName) {
        super(EntityType.MANNEQUIN, world);
        this.originalPlayerUuid = originalPlayerUuid;
        this.originalPlayerName = originalPlayerName;
    }

    public static CombatNPC spawn(ServerPlayer original) {
        ServerLevel world = (ServerLevel) original.level();
        MinecraftServer server = world.getServer();
        CombatConfig config = Combatpersistence.config;

        String playerName = original.getName().getString();
        CombatNPC npc = new CombatNPC(world, original.getUUID(), playerName);

        // Visual setup
        GameProfile currentProfile = original.getGameProfile();
        npc.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(currentProfile));

        // Async skin resolution for offline mode
        if (currentProfile.properties().isEmpty()) {
            String skinName = Combatpersistence.authManager.getCustomSkin(original.getUUID());
            final String finalSkinName = skinName != null ? skinName : playerName;
            CompletableFuture.runAsync(() -> {
                try {
                    var profileOpt = server.services().profileResolver().fetchByName(finalSkinName);
                    if (profileOpt.isPresent()) {
                        GameProfile mojangProfile = profileOpt.get();
                        ProfileResult result = server.services().sessionService().fetchProfile(mojangProfile.id(), true);
                        if (result != null && result.profile() != null) {
                            final GameProfile finalProfile = result.profile();
                            server.execute(() -> {
                                if (!npc.isRemoved()) {
                                    npc.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(finalProfile));
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            });
        }

        npc.setCustomName(Component.literal(config.npcNamePrefix + playerName));
        npc.setCustomNameVisible(true);

        // Position and Attributes
        npc.setPos(original.getX(), original.getY(), original.getZ());
        npc.setYRot(original.getYRot());
        npc.setXRot(original.getXRot());
        npc.setYHeadRot(original.getYHeadRot());
        npc.yBodyRot = original.yBodyRot;
        npc.getAttributes().assignAllValues(original.getAttributes());
        npc.setHealth(original.getHealth());
        npc.setAbsorptionAmount(original.getAbsorptionAmount());
        
        // Copy Equipment (Visual only)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            npc.setItemSlot(slot, original.getItemBySlot(slot).copy());
        }

        // Store Full Inventory for drop-on-death
        for (int i = 0; i < original.getInventory().getContainerSize(); i++) {
            ItemStack stack = original.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                npc.extraInventory.add(stack.copy());
            }
        }

        world.addFreshEntity(npc);

        if (config.playSpawnSound) {
            world.playSound(null, npc.getX(), npc.getY(), npc.getZ(), 
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, 
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return npc;
    }

    @Override
    public boolean canUsePortal(boolean allowVehicles) {
        return false; // Prevent pushing NPCs into portals to evade trackers
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!hasDropped) {
            dropExtraInventory();
            Combatpersistence.tracker.markOfflineDeath(this.originalPlayerUuid);
            Combatpersistence.tracker.removeNPC(this.originalPlayerUuid);
            
            // Fix A: Clear equipment so vanilla logic doesn't drop it (preventing armor dupe)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
            
            // Fix B: Broadcast the kill
            if (this.level() instanceof ServerLevel world) {
                Component deathMsg = Component.literal("§c[PvP] §7" + this.originalPlayerName + " combat logged and was slain!");
                world.getServer().getPlayerList().broadcastSystemMessage(deathMsg, false);
            }
            hasDropped = true;
        }
        super.die(damageSource);
    }

    public void dropExtraInventory() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (ItemStack stack : extraInventory) {
                this.spawnAtLocation(serverLevel, stack);
            }
            extraInventory.clear();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(new Vec3(0, this.getDeltaMovement().y, 0)); 
    }
}