package dhj.embeddedt.embeddium.impl.gl.device;

import dhj.embeddedt.embeddium.impl.gl.functions.DeviceFunctions;

public interface RenderDevice {
    RenderDevice INSTANCE = new GLRenderDevice();

    CommandList createCommandList();

    static void enterManagedCode() {
        RenderDevice.INSTANCE.makeActive();
    }

    static void exitManagedCode() {
        RenderDevice.INSTANCE.makeInactive();
    }

    void makeActive();
    void makeInactive();

    DeviceFunctions getDeviceFunctions();
}
