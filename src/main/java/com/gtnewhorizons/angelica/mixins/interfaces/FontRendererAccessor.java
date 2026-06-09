package com.gtnewhorizons.angelica.mixins.interfaces;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;

public interface FontRendererAccessor {
    int angelica$drawStringBatched(String text, int x, int y, int argb, boolean dropShadow);

    void angelica$bindTexture(net.minecraft.util.ResourceLocation location);

    BatchingFontRenderer angelica$getBatcher();
}
