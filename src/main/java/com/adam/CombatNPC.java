package com.adam;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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

public class CombatNPC extends Mannequin {

    public final UUID originalPlayerUuid;
    private final List<ItemStack> extraInventory = new ArrayList<>();
    private boolean hasDropped = false;

    public CombatNPC(ServerLevel world, UUID originalPlayerUuid) {
        super(EntityType.MANNEQUIN, world);
        this.originalPlayerUuid = originalPlayerUuid;
    }

    public static CombatNPC spawn(ServerPlayer original) {
        ServerLevel world = (ServerLevel) original.level();
        CombatConfig config = Combatpersistence.config;

        CombatNPC npc = new CombatNPC(world, original.getUUID());

        // Set Profile for Skin
        ResolvableProfile profile = ResolvableProfile.createResolved(original.getGameProfile());
        npc.setComponent(DataComponents.PROFILE, profile);

        // Visuals
        npc.setCustomName(Component.literal(config.npcNamePrefix + original.getName().getString()));
        npc.setCustomNameVisible(true);

        // Position
        npc.setPos(original.getX(), original.getY(), original.getZ());
        npc.setYRot(original.getYRot());
        npc.setXRot(original.getXRot());
        npc.setYHeadRot(original.getYHeadRot());
        npc.yBodyRot = original.yBodyRot;

        // Attributes & Health
        npc.getAttributes().assignAllValues(original.getAttributes());
        npc.setHealth(original.getHealth());
        npc.setAbsorptionAmount(original.getAbsorptionAmount());
        
        // Copy Equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            npc.setItemSlot(slot, original.getItemBySlot(slot).copy());
        }

        // Store Full Inventory
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
    public void die(DamageSource damageSource) {
        if (!hasDropped) {
            dropExtraInventory();
            Combatpersistence.tracker.markOfflineDeath(this.originalPlayerUuid);
            Combatpersistence.tracker.removeNPC(this.originalPlayerUuid);
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