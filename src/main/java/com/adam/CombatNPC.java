package com.adam;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CombatNPC extends Mannequin {

    public final UUID originalPlayerUuid;
    private final String originalPlayerName;
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
            SkinManager.fetchSkin(finalSkinName).thenAccept(skinProperty -> {
                if (skinProperty != null) {
                    server.execute(() -> {
                        if (!npc.isRemoved()) {
                            // Get the existing GameProfile properties from the original player
                            PropertyMap existingProperties = original.getGameProfile().getProperties();
                            
                            // Create a new mutable PropertyMap
                            PropertyMap mutableProperties = new PropertyMap();
                            
                            // Copy existing properties to the new mutable map
                            for (Map.Entry<String, Property> entry : existingProperties.entries()) {
                                mutableProperties.put(entry.getKey(), entry.getValue());
                            }
                            
                            // Add the new skin property
                            mutableProperties.put("textures", skinProperty);
                            
                            // Create a new GameProfile with the updated properties
                            GameProfile updatedProfile = new GameProfile(original.getUUID(), playerName, mutableProperties);
                            
                            // Set the ResolvableProfile component for the NPC
                            npc.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(updatedProfile));
                        }
                    });
                }
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
            // Broadcast the kill
            if (this.level() instanceof ServerLevel world) {
                Component deathMsg = Component.literal("§c[PvP] §7" + this.originalPlayerName + " combat logged and was slain!");
                world.getServer().getPlayerList().broadcastSystemMessage(deathMsg, false);
            }

            // Fix A: Clear equipment so vanilla logic doesn't drop it (preventing armor dupe)
            // The items will be dropped by the tracker (CombatDeathMixin)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
            
            hasDropped = true;
        }
        super.die(damageSource);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(new Vec3(0, this.getDeltaMovement().y, 0)); 
    }
}