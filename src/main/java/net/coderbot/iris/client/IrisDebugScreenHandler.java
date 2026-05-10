package net.coderbot.iris.client;

import com.dhj.actinium.config.ActiniumConfig;
import com.gtnewhorizons.angelica.proxy.ClientProxy;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Objects;

public class IrisDebugScreenHandler {
    public static final IrisDebugScreenHandler INSTANCE = new IrisDebugScreenHandler();
    private static final List<BufferPoolMXBean> iris$pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

    private static final BufferPoolMXBean iris$directPool;

    static {
        BufferPoolMXBean found = null;

        for (BufferPoolMXBean pool : iris$pools) {
            if (pool.getName().equals("direct")) {
                found = pool;
                break;
            }
        }

        iris$directPool = Objects.requireNonNull(found);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGameOverlayTextEvent(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo && !ActiniumConfig.disableF3Additions) {
            event.getRight().add(Math.min(event.getRight().size(), 2), "Direct Buffers: +" + iris$humanReadableByteCountBin(iris$directPool.getMemoryUsed()));

            if (Iris.getIrisConfig().areShadersEnabled()) {
                event.getRight().add("[" + Iris.MODNAME + "] Shaderpack: " + Iris.getCurrentPackName() + (Iris.isFallback() ? " (fallback)" : ""));
                Iris.getCurrentPack().ifPresent(pack -> event.getRight().add("[" + Iris.MODNAME + "] " + pack.getProfileInfo()));
            } else {
                event.getRight().add("[" + Iris.MODNAME + "] Shaders are disabled");
            }
            if (ActiniumConfig.speedupAnimations) {
                event.getRight().add(Math.min(event.getRight().size(), 9), "animationsMode: " + ClientProxy.animationsMode);
            }

            Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(event.getLeft()));

        }
    }

    private static String iris$humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.3f %ciB", value / 1024.0, ci.current());
    }

}
