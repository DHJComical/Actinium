package net.coderbot.iris.client;

import net.coderbot.iris.debug.IrisDebugOptions;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class IrisDebugScreenHandler {
    public static final IrisDebugScreenHandler INSTANCE = new IrisDebugScreenHandler();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGameOverlayTextEvent(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo && !IrisDebugOptions.disableF3Additions()) {
            event.getRight().add(Math.min(event.getRight().size(), 2), "[" + Iris.MODNAME + "] Version: " + Iris.getFormattedVersion());

            if (Iris.getIrisConfig().areShadersEnabled()) {
                event.getRight().add("[" + Iris.MODNAME + "] Shaderpack: " + Iris.getCurrentPackName() + (Iris.isFallback() ? " (fallback)" : ""));
                Iris.getCurrentPack().ifPresent(pack -> event.getRight().add("[" + Iris.MODNAME + "] " + pack.getProfileInfo()));
                if (mc.world != null) {
                    event.getRight().add("[" + Iris.MODNAME + "] Shadows: " + ActiniumWorldRenderer.instance().getChunksDebugString());
                }
            } else {
                event.getRight().add("[" + Iris.MODNAME + "] Shaders are disabled");
            }

            Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(event.getLeft()));
        }
    }
}
