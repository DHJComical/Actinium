package net.coderbot.iris.rendertarget;

import com.gtnewhorizons.angelica.compat.mojang.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeImageBackedSingleColorTextureTest {
    @Test
    void preservesRgbaChannelsInBufferedImage() {
        NativeImage image = NativeImageBackedSingleColorTexture.create(0x12, 0x34, 0x56, 0x78);

        assertEquals(0x78123456, image.getPixelRGBA(0, 0));
    }
}
