package com.gtnewhorizons.angelica.loading.fml.transformers;

import com.gtnewhorizons.angelica.glsm.redirect.RedirectorDebugOptions;
import com.gtnewhorizons.angelica.loading.shared.AngelicaClassDump;
import com.gtnewhorizons.angelica.loading.shared.transformers.AngelicaRedirector;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class AngelicaRedirectorTransformer implements IClassTransformer {

    private static final boolean DEBUG = RedirectorDebugOptions.enableDebug();
    private final AngelicaRedirector inner;
    private final String[] exclusions;

    public AngelicaRedirectorTransformer() {
        inner = new AngelicaRedirector();
        exclusions = inner.getTransformerExclusions();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public byte[] transform(String className, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        for (String exclusion : exclusions) {
            if (transformedName.startsWith(exclusion)) {
                return basicClass;
            }
        }

        if (!inner.shouldTransform(basicClass)) {
            return basicClass;
        }

        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        boolean changed = inner.transformClassNode(transformedName, cn);
        if (!changed) {
            return basicClass;
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        byte[] bytes = cw.toByteArray();
        if (DEBUG && (transformedName.startsWith("net.minecraft.client.renderer.")
            || transformedName.startsWith("net.minecraft.client.shader.")
            || transformedName.startsWith("net.minecraft.client.gui.FontRenderer")
            || transformedName.startsWith("net.minecraftforge.client.")
            || transformedName.startsWith("org.taumc.celeritas.")
            || transformedName.startsWith("com.dhj.actinium."))) {
            System.out.println("[ActiniumRedirector] late transformed " + transformedName);
        }
        AngelicaClassDump.dumpClass(transformedName, basicClass, bytes, this);
        return bytes;
    }
}
