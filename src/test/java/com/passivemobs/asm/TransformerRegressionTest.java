package com.passivemobs.asm;

import com.passivemobs.TypeHierarchy;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.io.*;
import java.util.jar.*;
import static org.objectweb.asm.Opcodes.*;

/** Dependency-free regression runner: verifies actual Minecraft bytecode without booting the game. */
public final class TransformerRegressionTest {
    private static final String[] MCP = {
        "net.minecraft.entity.EntityLiving", "net.minecraft.entity.EntityCreature",
        "net.minecraft.entity.monster.EntityGhast", "net.minecraft.entity.ai.EntityAITarget"
    };
    private static final String[] NOTCH = {"sw", "td", "yd", "vu"};
    private static final String[] FIELDS = {"attackTarget", "entityToAttack", "targetedEntity"};
    private static final String[] SRG_FIELDS = {"field_70696_bz", "field_70789_a", "field_70792_g"};
    private static final String[] NOTCH_FIELDS = {"bp", "bm", "bq"};
    private static int checked;

    private interface ParentBoss {}
    private interface ChildBoss extends ParentBoss {}
    private static class BossBase implements ChildBoss {}
    private static class BossSubclass extends BossBase {}

    public static void main(String[] args) throws Exception {
        if (!TypeHierarchy.hasType(BossSubclass.class, ParentBoss.class.getName())
            || !TypeHierarchy.hasType(BossSubclass.class, BossBase.class.getName())
            || TypeHierarchy.hasType(Object.class, ParentBoss.class.getName())
            || TypeHierarchy.hasType(BossSubclass.class, "missing.optional.mod.IBoss")) {
            throw new AssertionError("Optional boss interface / inherited exclusion matching failed");
        }
        NoPlayerTargetTransformer transformer = new NoPlayerTargetTransformer();
        if (transformer.transform(null, null, null) != null) throw new AssertionError("null input");
        for (int namespace = 0; namespace < 3; namespace++) {
            for (int kind = 0; kind < 4; kind++) {
                String owner = namespace == 2 ? NOTCH[kind] : MCP[kind].replace('.', '/');
                byte[] fixture = fixture(owner, kind, namespace);
                byte[] patched = transformer.transform(namespace == 2 ? NOTCH[kind] : MCP[kind], MCP[kind], fixture);
                verify(patched);
                ClassNode node = new ClassNode();
                new ClassReader(patched).accept(node, 0);
                for (Object obj : node.methods) {
                    MethodNode method = (MethodNode) obj;
                    int hooks = hooks(method);
                    if (method.name.equals("unrelated") ? hooks != 0 : hooks == 0) {
                        throw new AssertionError("Incorrect hook scope: " + owner + "." + method.name);
                    }
                }
            }
        }
        java.net.URL resource = TransformerRegressionTest.class.getClassLoader()
            .getResource("net/minecraft/entity/EntityLiving.class");
        if (resource == null || !resource.getProtocol().equals("jar")) {
            throw new AssertionError("Minecraft classes must be available from a jar");
        }
        String[] jars = new String[args.length + 1];
        jars[0] = new File(((java.net.JarURLConnection) resource.openConnection())
            .getJarFileURL().toURI()).getPath();
        System.arraycopy(args, 0, jars, 1, args.length);
        for (int jarIndex = 0; jarIndex < jars.length; jarIndex++) {
            try (JarFile jar = new JarFile(jars[jarIndex])) {
                for (int kind = 0; kind < 4; kind++) {
                    String name = jarIndex == 0 ? MCP[kind].replace('.', '/') : NOTCH[kind];
                    JarEntry entry = jar.getJarEntry(name + ".class");
                    if (entry == null) throw new AssertionError("Missing " + name);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (InputStream in = jar.getInputStream(entry)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    }
                    byte[] patched = transformer.transform(name.replace('/', '.'), MCP[kind], out.toByteArray());
                    verify(patched);
                    ClassNode node = new ClassNode();
                    new ClassReader(patched).accept(node, 0);
                    int count = 0;
                    for (Object method : node.methods) count += hooks((MethodNode) method);
                    if (count == 0) throw new AssertionError("No hooks in real class " + name);
                }
            }
        }
        System.out.println("Verified " + checked + " transformed classes (MCP/SRG/Notch and real game jars).");
    }

    private static int hooks(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("com/passivemobs/TargetingRules")) count++;
        }
        return count;
    }

    private static void verify(byte[] bytes) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (Object obj : node.methods) {
            MethodNode method = (MethodNode) obj;
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0) {
                new Analyzer(new BasicVerifier()).analyze(node.name, method);
            }
        }
        checked++;
    }

    private static byte[] fixture(String owner, int kind, int namespace) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_6, ACC_PUBLIC, owner, null, "java/lang/Object", null);
        String base = namespace == 2 ? "sv" : "net/minecraft/entity/EntityLivingBase";
        if (kind < 3) {
            String field = namespace == 0 ? FIELDS[kind] : namespace == 1 ? SRG_FIELDS[kind] : NOTCH_FIELDS[kind];
            writer.visitField(ACC_PUBLIC, field, "L" + base + ";", null, null).visitEnd();
            MethodVisitor method = writer.visitMethod(ACC_PUBLIC, "set", "(L" + base + ";)V", null, null);
            method.visitCode();
            method.visitVarInsn(ALOAD, 0);
            method.visitVarInsn(ALOAD, 1);
            method.visitFieldInsn(PUTFIELD, owner, field, "L" + base + ";");
            if (kind == 2) {
                method.visitVarInsn(ALOAD, 0);
                method.visitFieldInsn(GETFIELD, owner, field, "L" + base + ";");
                method.visitInsn(POP);
            }
            method.visitInsn(RETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        } else {
            String methodName = namespace == 0 ? "isSuitableTarget" : namespace == 1 ? "func_75296_a" : "a";
            MethodVisitor method = writer.visitMethod(ACC_PUBLIC, methodName, "(L" + base + ";Z)Z", null, null);
            method.visitCode();
            method.visitInsn(ICONST_1);
            method.visitInsn(IRETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        // The old transformer mistakenly modified every setter with this signature.
        MethodVisitor unrelated = writer.visitMethod(ACC_PUBLIC, "unrelated", "(L" + base + ";)V", null, null);
        unrelated.visitCode();
        unrelated.visitInsn(RETURN);
        unrelated.visitMaxs(0, 0);
        unrelated.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
