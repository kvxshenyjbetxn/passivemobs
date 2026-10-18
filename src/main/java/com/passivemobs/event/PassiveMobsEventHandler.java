package com.passivemobs.event;

import com.passivemobs.PassiveMobsMod;
import com.passivemobs.TargetingRules;
import com.passivemobs.asm.NoPlayerTargetTransformer;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.*;

public class PassiveMobsEventHandler {
    @SubscribeEvent
    public void onTickStart(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            NoPlayerTargetTransformer.active = PassiveMobsMod.enabled;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(LivingAttackEvent event) {
        // Players (including PvP) and excluded bosses must not be affected.
        if (event.entity instanceof EntityPlayer && TargetingRules.isPassive(event.source.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSetTarget(LivingSetAttackTargetEvent event) {
        if (!event.entity.worldObj.isRemote && event.target instanceof EntityPlayer
            && TargetingRules.isPassive(event.entityLiving)) {
            clearTargets((EntityLiving) event.entityLiving);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entity.worldObj.isRemote || !TargetingRules.isPassive(event.entityLiving)) return;
        clearTargets((EntityLiving) event.entityLiving);
    }

    private void clearTargets(EntityLiving mob) {
        boolean cleared = false;
        if (mob.getAttackTarget() instanceof EntityPlayer) {
            mob.setAttackTarget(null);
            cleared = true;
        }
        if (mob.getAITarget() instanceof EntityPlayer) mob.setRevengeTarget(null);
        if (mob instanceof EntityCreature && ((EntityCreature) mob).getEntityToAttack() instanceof EntityPlayer) {
            ((EntityCreature) mob).setTarget(null);
            cleared = true;
        }
        if (cleared) {
            mob.getNavigator().clearPathEntity();
            if (mob instanceof EntityCreeper) ((EntityCreeper) mob).setCreeperState(-1);
        }
    }
}
