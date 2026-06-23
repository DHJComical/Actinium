package com.dhj.actinium.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public class ActiniumLWJGLRelocationTransformer implements IClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumLWJGLRelocation");
    private static final Remapper LWJGL_REMAPPER = new LwjglRemapper();

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass != null && (
            transformedName.startsWith("org.embeddedt.embeddium")
                || transformedName.startsWith("org.taumc.celeritas")
                || transformedName.startsWith("com.dhj.actinium"))) {
            try {
                var reader = new ClassReader(basicClass);
                var writer = new ClassWriter(0);
                var remapper = new ClassRemapper(writer, LWJGL_REMAPPER);
                reader.accept(remapper, 0);
                return writer.toByteArray();
            } catch (Exception e) {
                LOGGER.error("Exception remapping class", e);
                return basicClass;
            }
        }

        return basicClass;
    }

    private static class LwjglRemapper extends Remapper {
        @Override
        public String map(String internalName) {
            return internalName;
        }
    }
}
