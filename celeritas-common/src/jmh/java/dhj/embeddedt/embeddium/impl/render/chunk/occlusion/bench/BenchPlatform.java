package dhj.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import dhj.embeddedt.embeddium.impl.gl.device.RenderDevice;
import dhj.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import dhj.embeddedt.embeddium.impl.render.chunk.bench.voxel.BenchRenderPasses;
import dhj.embeddedt.embeddium.impl.render.chunk.region.RenderRegionManager;

/**
 * Headless stand-in for the platform layer: a real {@link RenderDevice} on a real GL context
 * ({@link HeadlessGl}) and a real {@link RenderRegionManager} with real ids. No production class is stubbed.
 */
public final class BenchPlatform {
    private static boolean initialized;
    private static RenderRegionManager regionManager;

    private BenchPlatform() {
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        HeadlessGl.ensureContext();

        RenderDevice.enterManagedCode();

        regionManager = new RenderRegionManager(RenderDevice.INSTANCE.createCommandList(),
                BenchRenderPasses.configuration());

        initialized = true;
    }

    /** {@return the shared region manager} */
    public static synchronized RenderRegionManager regionManager() {
        ensureInitialized();

        return regionManager;
    }
}
