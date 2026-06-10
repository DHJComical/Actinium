package com.gtnewhorizons.angelica.loading.shared;

import com.dhj.actinium.debug.ActiniumStartupDebugConfig;
import net.minecraft.launchwrapper.Launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AngelicaClassDump {

    private static final boolean DUMP_CLASS = ActiniumStartupDebugConfig.enableClassDump();

    public static void dumpClass(String className, byte[] originalBytes, byte[] transformedBytes, Object transformer) {
        if (!DUMP_CLASS) {
            return;
        }
        dumpBytes(originalBytes, className + "_PRE", transformer);
        dumpBytes(transformedBytes, className + "_POST", transformer);
    }

    private static void dumpBytes(byte[] bytes, String className, Object transformer) {
        String transformerName = transformer.getClass().getSimpleName().toUpperCase();
        String relativeName = className.replace('.', '/').replace('$', '.');
        Path output = Launch.minecraftHome.toPath()
            .resolve("ASM_GTNH")
            .resolve(transformerName)
            .resolve(relativeName + ".class");
        try {
            Files.createDirectories(output.getParent());
            Files.write(output, bytes);
        } catch (IOException ignored) {
        }
    }

    private AngelicaClassDump() {}
}
