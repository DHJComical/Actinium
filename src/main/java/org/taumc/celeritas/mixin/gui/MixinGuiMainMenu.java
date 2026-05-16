package org.taumc.celeritas.mixin.gui;

import com.gtnewhorizons.angelica.render.PanoramaRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {
    @Shadow
    private float panoramaTimer;

    private static final ResourceLocation[] celeritas$titlePanoramaPaths = new ResourceLocation[] {
        new ResourceLocation("textures/gui/title/background/panorama_0.png"),
        new ResourceLocation("textures/gui/title/background/panorama_1.png"),
        new ResourceLocation("textures/gui/title/background/panorama_2.png"),
        new ResourceLocation("textures/gui/title/background/panorama_3.png"),
        new ResourceLocation("textures/gui/title/background/panorama_4.png"),
        new ResourceLocation("textures/gui/title/background/panorama_5.png")
    };

    /**
     * @author Actinium
     * @reason Replace the vanilla panorama path with the GTNH/Angelica implementation.
     */
    @Overwrite
    public void renderSkybox(int mouseX, int mouseY, float partialTicks) {
        PanoramaRenderer.getInstance()
            .renderSkybox(
                (int) this.panoramaTimer,
                partialTicks,
                celeritas$titlePanoramaPaths,
                this.mc,
                this.width,
                this.height,
                this.zLevel
            );
    }
}
