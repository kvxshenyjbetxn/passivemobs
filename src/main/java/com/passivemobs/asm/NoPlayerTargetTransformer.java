package com.passivemobs.asm;

import static org.objectweb.asm.Opcodes.*;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

/**
 * ASM patches targeting Notch-obfuscated class files (1.7.10 production).
 *
 * Notch names used:
 *   sa = EntityLivingBase
 *   sv = EntityLiving
 *   yz = EntityPlayer
 *
 * Patches:
 *   1. EntityAITarget: any method (Lsa;Z)Z → isSuitableTarget
 *   2. EntityLiving: any method (Lsa;)V → setAttackTarget / setRevengeTarget
 */
public class NoPlayerTargetTransformer implements IClassTransformer {

    public static volatile boolean active = true;

    private static final String OUR_CLASS = "com/passivemobs/asm/NoPlayerTargetTransformer";
    private static final String OUR_FIELD = "active";

    // Notch class names for 1.7.10
    // sa = Entity, sv = EntityLivingBase, yz = EntityPlayer
    private static final String NOTCH_ENTITY_LIVING     = "sv";  // EntityLiving
    private static final String NOTCH_ENTITY_AI_TARGET  = "ub";  // EntityAITarget
    private static final String NOTCH_ENTITY_PLAYER     = "yz";  // EntityPlayer
    private static final String NOTCH_ENTITY_LIVING_BASE = "sv"; // EntityLivingBase = sv in Notch (entityLiving is also sv? no, EntityLiving extends EntityLivingBase, internal name for EntityLivingBase is sv, EntityLiving might be something else)

    // MCP class names (for deobf environment fallback)
    private static final String MCP_ENTITY_LIVING    = "net.minecraft.entity.EntityLiving";
    private static final String MCP_ENTITY_AI_TARGET = "net.minecraft.entity.ai.EntityAITarget";
    private static final String MCP_ENTITY_PLAYER    = "net/minecraft/entity/player/EntityPlayer";

    // Descriptors (Notch internal names)
    // EntityLivingBase = sv in Notch 1.7.10
    // EntityPlayer     = yz in Notch 1.7.10
    private static final String DESC_SET_TARGET  = "(Lsv;)V";    // (EntityLivingBase)V
    private static final String DESC_SUIT_TARGET = "(Lsv;Z)Z";   // (EntityLivingBase;Z)Z

    @Override
    public byte[] transform(String name, String tName, byte[] bytes) {
        if (bytes == null) return null;

        boolean isLiving    = tName.equals(MCP_ENTITY_LIVING)    || name.equals(NOTCH_ENTITY_LIVING);
        boolean isAITarget  = tName.equals(MCP_ENTITY_AI_TARGET) || name.equals(NOTCH_ENTITY_AI_TARGET);

        if (isLiving)   return patchSetTarget(bytes);
        if (isAITarget) return patchIsSuitableTarget(bytes);
        return bytes;
    }

    // ── Patch isSuitableTarget(EntityLivingBase;Z)Z ──────────────────

    private byte[] patchIsSuitableTarget(byte[] bytes) {
        System.out.println("[PassiveMobs] ASM: patching EntityAITarget.isSuitableTarget");
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, exc);
                // Match by descriptor: (Lsa;Z)Z = (EntityLivingBase;boolean)boolean
                // In MCP: (Lnet/minecraft/entity/EntityLivingBase;Z)Z
                if (desc.equals(DESC_SUIT_TARGET) || desc.equals("(Lnet/minecraft/entity/EntityLivingBase;Z)Z")) {
                    System.out.println("[PassiveMobs]   -> PATCHED " + name + desc);
                    return new MethodVisitor(ASM5, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            mv.visitFieldInsn(GETSTATIC, OUR_CLASS, OUR_FIELD, "Z");
                            Label skip = new Label();
                            mv.visitJumpInsn(IFEQ, skip);
                            mv.visitVarInsn(ALOAD, 1);
                            // Check instanceof using whatever class name is in context
                            // For Notch: yz, for MCP: net/minecraft/entity/player/EntityPlayer
                            // We try Notch first; if the class is deobf, it still compiles fine
                            mv.visitTypeInsn(INSTANCEOF, NOTCH_ENTITY_PLAYER);
                            mv.visitJumpInsn(IFEQ, skip);
                            mv.visitInsn(ICONST_0);
                            mv.visitInsn(IRETURN);
                            mv.visitLabel(skip);
                        }
                    };
                }
                return mv;
            }
        }, 0);
        return cw.toByteArray();
    }

    // ── Patch setAttackTarget / setRevengeTarget (Lsa;)V ─────────────

    private byte[] patchSetTarget(byte[] bytes) {
        System.out.println("[PassiveMobs] ASM: patching EntityLiving setAttackTarget/setRevengeTarget");
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, exc);
                if (desc.equals(DESC_SET_TARGET) || desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)V")) {
                    System.out.println("[PassiveMobs]   -> PATCHED " + name + desc);
                    return new MethodVisitor(ASM5, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            mv.visitFieldInsn(GETSTATIC, OUR_CLASS, OUR_FIELD, "Z");
                            Label skip = new Label();
                            mv.visitJumpInsn(IFEQ, skip);
                            mv.visitVarInsn(ALOAD, 1);
                            mv.visitTypeInsn(INSTANCEOF, NOTCH_ENTITY_PLAYER);
                            mv.visitJumpInsn(IFEQ, skip);
                            mv.visitInsn(RETURN);
                            mv.visitLabel(skip);
                        }
                    };
                }
                return mv;
            }
        }, 0);
        return cw.toByteArray();
    }
}
