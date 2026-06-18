package com.passivemobs.event;

import com.passivemobs.PassiveMobsMod;
import com.passivemobs.asm.NoPlayerTargetTransformer;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.living.*;

public class PassiveMobsEventHandler {

    // ── Sync ASM flag with config ─────────────────────────────────────

    @SubscribeEvent
    public void onTickStart(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        NoPlayerTargetTransformer.active = PassiveMobsMod.enabled;
    }

    // ── Backup: clear all player targets at start of tick ─────────────

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTickStartClear(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (!PassiveMobsMod.enabled) return;
        clearAllTargets();
    }

    // ── Backup: clear all player targets at end of tick ───────────────

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTickEndClear(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!PassiveMobsMod.enabled) return;
        clearAllTargets();
    }

    // ── Safety net: cancel any mob-sourced damage ─────────────────────

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(LivingAttackEvent e) {
        if (!PassiveMobsMod.enabled) return;
        if (e.entity instanceof EntityPlayer
            && e.source.getEntity() instanceof EntityLivingBase) {
            e.setCanceled(true);
        }
    }

    // ── Per-entity: clear targets + creeper fuse + spider handling ──

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        if (!PassiveMobsMod.enabled) return;
        if (e.entity.worldObj.isRemote) return;

        // Creeper fuse reset
        if (e.entityLiving instanceof EntityCreeper) {
            EntityCreeper c = (EntityCreeper) e.entityLiving;
            if (c.getCreeperState() != -1) c.setCreeperState(-1);
        }

        // All mobs: clear target
        if (e.entityLiving instanceof EntityLiving) {
            EntityLiving mob = (EntityLiving) e.entityLiving;
            if (mob.getAttackTarget() instanceof EntityPlayer) {
                mob.setAttackTarget(null);
                mob.setRevengeTarget(null);
                mob.getNavigator().clearPathEntity();
                mob.motionX = 0;
                mob.motionZ = 0;
            }
        }

        // Spiders: freeze them in place (they bypass standard AI)
        if (e.entityLiving instanceof net.minecraft.entity.monster.EntitySpider) {
            net.minecraft.entity.monster.EntitySpider spider =
                (net.minecraft.entity.monster.EntitySpider) e.entityLiving;
            if (spider.getAttackTarget() instanceof EntityPlayer) {
                spider.setAttackTarget(null);
                spider.setRevengeTarget(null);
            }
            spider.motionX = 0;
            spider.motionY = 0;
            spider.motionZ = 0;
            spider.posX = spider.prevPosX;
            spider.posY = spider.prevPosY;
            spider.posZ = spider.prevPosZ;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────

    private void clearAllTargets() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;

        for (Object worldObj : server.worldServers) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) worldObj;
            for (Object entityObj : world.loadedEntityList) {
                if (entityObj instanceof EntityLiving) {
                    EntityLiving mob = (EntityLiving) entityObj;
                    if (mob.getAttackTarget() instanceof EntityPlayer) {
                        mob.setAttackTarget(null);
                        mob.setRevengeTarget(null);
                        mob.getNavigator().clearPathEntity();
                    }
                }
            }
        }
    }
}
