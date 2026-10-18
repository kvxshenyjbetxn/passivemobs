package com.passivemobs;

import com.passivemobs.asm.NoPlayerTargetTransformer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;

/** Runtime hooks are separate from the transformer to avoid early Minecraft class loading. */
public final class TargetingRules {
    private TargetingRules() {}

    public static boolean isPassive(Object mob) {
        if (!NoPlayerTargetTransformer.active || !(mob instanceof EntityLiving)) return false;
        if (PassiveMobsMod.allowBosses && (mob instanceof IBossDisplayData
            || TypeHierarchy.hasType(mob.getClass(), "micdoodle8.mods.galacticraft.core.entities.IBoss"))) {
            return false;
        }
        for (String excluded : PassiveMobsMod.excludedEntities) {
            if (TypeHierarchy.hasType(mob.getClass(), excluded)) return false;
        }
        return true;
    }

    public static Object filterTarget(Object target, Object mob) {
        return target instanceof EntityPlayer && isPassive(mob) ? null : target;
    }

    public static boolean filterSuitability(boolean suitable, Object mob, Object target) {
        return suitable && !(target instanceof EntityPlayer && isPassive(mob));
    }
}
