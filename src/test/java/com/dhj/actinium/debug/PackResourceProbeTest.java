package com.dhj.actinium.debug;

import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the production translation failure.
 *
 * Root cause: the mod jar declared pack_format 2, so FML wrapped its resource pack in
 * LegacyV2Adapter, which uppercases lang paths (lang/zh_cn.lang -> lang/zh_CN.lang).
 * Zip entry lookup is case-sensitive, so every lang file in the jar became unreachable
 * in production (development survived only because NTFS is case-insensitive for folder packs).
 *
 * This test replays FML's addModAsResource wrapping decision against the built jar and
 * requires every shipped lang file to be reachable through the effective pack.
 */
class PackResourceProbeTest {

    private static final String[] DOMAINS = {"actinium", "celeritas", "iris", "sodium"};
    private static final Pattern PACK_FORMAT = Pattern.compile("\"pack_format\"\\s*:\\s*(\\d+)");

    @Test
    void langFilesSurviveFmlResourcePackWrapping() throws Exception {
        File sourceMcmeta = projectFile("src/main/resources/pack.mcmeta");
        assertTrue(sourceMcmeta.isFile(), "source pack.mcmeta missing: " + sourceMcmeta);
        Matcher sourceMatcher = PACK_FORMAT.matcher(java.nio.file.Files.readString(sourceMcmeta.toPath()));
        assertTrue(sourceMatcher.find(), "pack_format not declared in " + sourceMcmeta);
        assertEquals(3, Integer.parseInt(sourceMatcher.group(1)),
                "pack_format must be 3; format 2 makes FML wrap the pack in LegacyV2Adapter, "
                        + "which uppercases lang paths and hides every lang file inside the jar");

        File jar = projectFile("build/libs/Actinium-2.4.0-dev.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(jar.isFile(), "production jar not built, skipping jar-level replay");

        int packFormat = readPackFormat(jar);
        assertEquals(3, packFormat, "built jar must carry pack_format 3: " + jar);

        IResourcePack pack = new FileResourcePack(jar);
        if (packFormat == 2) {
            pack = new LegacyV2Adapter(pack);
        }

        for (String domain : DOMAINS) {
            for (String path : new String[]{"lang/zh_cn.lang", "lang/en_us.lang"}) {
                ResourceLocation location = new ResourceLocation(domain, path);
                assertTrue(pack.resourceExists(location),
                        () -> location + " unreachable through the effective FML resource pack of " + jar);
                try (InputStream in = pack.getInputStream(location)) {
                    assertTrue(in.read() >= 0, () -> location + " is empty in " + jar);
                }
            }
        }
    }

    private static File projectFile(String relative) {
        return java.nio.file.Paths.get("").toAbsolutePath().resolve("../..").normalize().resolve(relative).toFile();
    }

    private static int readPackFormat(File jar) throws Exception {
        try (ZipFile zip = new ZipFile(jar)) {
            java.util.zip.ZipEntry entry = zip.getEntry("pack.mcmeta");
            assertTrue(entry != null, "pack.mcmeta missing in " + jar);
            String mcmeta;
            try (InputStream in = zip.getInputStream(entry)) {
                mcmeta = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher matcher = PACK_FORMAT.matcher(mcmeta);
            assertTrue(matcher.find(), "pack_format not declared in pack.mcmeta of " + jar);
            return Integer.parseInt(matcher.group(1));
        }
    }
}
