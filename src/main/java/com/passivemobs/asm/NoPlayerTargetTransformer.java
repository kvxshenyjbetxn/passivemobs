package com.passivemobs.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

/** Filters only combat target fields, never world player lookups or arbitrary setters. */
public class NoPlayerTargetTransformer implements IClassTransformer {
    public static volatile boolean active = true;
    private static final String RULES = "com/passivemobs/TargetingRules";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        boolean living = matches(name, transformedName, "sw", "net.minecraft.entity.EntityLiving");
        boolean creature = matches(name, transformedName, "td", "net.minecraft.entity.EntityCreature");
        boolean ghast = matches(name, transformedName, "yd", "net.minecraft.entity.monster.EntityGhast");
        boolean ai = matches(name, transformedName, "vu", "net.minecraft.entity.ai.EntityAITarget");
        if (!living && !creature && !ghast && !ai) return bytes;

        ClassReader reader = new ClassReader(bytes);
        // No branches or locals are added: preserve frames and avoid loading Minecraft classes here.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final String owner = reader.getClassName();
        final boolean notch = owner.indexOf('/') < 0;
        final boolean targetAI = ai;
        final boolean privateGhastTarget = ghast;
        final String targetField = living ? "attackTarget" : creature ? "entityToAttack" : "targetedEntity";
        final String srgField = living ? "field_70696_bz" : creature ? "field_70789_a" : "field_70792_g";
        final String notchField = living ? "bp" : creature ? "bm" : "bq";
        reader.accept(new ClassVisitor(ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, method, descriptor, signature, exceptions);
                final boolean suitable = targetAI
                    && (method.equals("isSuitableTarget") || method.equals("func_75296_a") || (notch && method.equals("a")))
                    && descriptor.equals(notch ? "(Lsv;Z)Z" : "(Lnet/minecraft/entity/EntityLivingBase;Z)Z");
                return new MethodVisitor(ASM5, visitor) {
                    @Override
                    public void visitFieldInsn(int opcode, String fieldOwner, String field, String desc) {
                        if (!targetAI && opcode == PUTFIELD && fieldOwner.equals(owner)
                            && (field.equals(targetField) || field.equals(srgField) || (notch && field.equals(notchField)))
                            && desc.startsWith("L")) {
                            // Stack: field receiver, target -> receiver, filtered target.
                            super.visitInsn(DUP2);
                            super.visitInsn(SWAP);
                            super.visitMethodInsn(INVOKESTATIC, RULES, "filterTarget",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                            super.visitTypeInsn(CHECKCAST, Type.getType(desc).getInternalName());
                            super.visitInsn(SWAP);
                            super.visitInsn(POP);
                        }
                        if (privateGhastTarget && opcode == GETFIELD && fieldOwner.equals(owner)
                            && (field.equals(targetField) || field.equals(srgField) || (notch && field.equals(notchField)))) {
                            // Also hide a target acquired before /passivemobs on.
                            super.visitInsn(DUP);
                            super.visitFieldInsn(opcode, fieldOwner, field, desc);
                            super.visitInsn(SWAP);
                            super.visitMethodInsn(INVOKESTATIC, RULES, "filterTarget",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                            super.visitTypeInsn(CHECKCAST, Type.getType(desc).getInternalName());
                            return;
                        }
                        super.visitFieldInsn(opcode, fieldOwner, field, desc);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (suitable && opcode == IRETURN) {
                            super.visitVarInsn(ALOAD, 0);
                            super.visitFieldInsn(GETFIELD, owner, notch ? "c" :
                                (method.equals("isSuitableTarget") ? "taskOwner" : "field_75299_d"),
                                notch ? "Ltd;" : "Lnet/minecraft/entity/EntityCreature;");
                            super.visitVarInsn(ALOAD, 1);
                            super.visitMethodInsn(INVOKESTATIC, RULES, "filterSuitability",
                                "(ZLjava/lang/Object;Ljava/lang/Object;)Z", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static boolean matches(String name, String transformed, String notch, String mcp) {
        return mcp.equals(transformed) || notch.equals(name);
    }
}
