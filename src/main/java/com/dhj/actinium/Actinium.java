package com.dhj.actinium;

import com.dhj.actinium.compat.dh.ActiniumDHIrisCompat;
import com.dhj.actinium.debug.ActiniumDiagnostics;
import com.gtnewhorizons.angelica.iris.IrisGLSMBridge;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.coderbot.iris.Iris;
import net.coderbot.iris.compat.dh.DHCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import com.dhj.actinium.command.TogglePassCommand;
import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import com.dhj.actinium.runtime.ActiniumRuntime;

import java.lang.management.ManagementFactory;

@Mod(modid = Actinium.MODID, useMetadata = true, clientSideOnly = true, acceptableRemoteVersions = "*")
public class Actinium {
    public static final String MODID = ActiniumRuntime.MODID;

    @EventHandler
    public void onConstruct(FMLConstructionEvent event) {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);

        var container = Loader.instance().getIndexedModList().get(MODID);
        String version = container != null ? container.getVersion() : "unknown";
        ActiniumRuntime.setVersion(version);

        ActiniumDiagnostics.logConstruction();
        initializeDistantHorizonsIrisCompat();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        initializeDistantHorizonsIrisCompat();
    }

    @EventHandler
    public void onInit(FMLInitializationEvent event) {
        initializeDistantHorizonsIrisCompat();

        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            ClientCommandHandler.instance.registerCommand(new TogglePassCommand());
        }

        if (Iris.enabled) {
            IrisGLSMBridge.register();
            Iris.INSTANCE.fmlInitEvent();
            MinecraftForge.EVENT_BUS.register(Iris.INSTANCE);
        }

        ActiniumDiagnostics.logInitialization(ActiniumRuntime.version());
    }

    private static void initializeDistantHorizonsIrisCompat() {
        if (Iris.enabled && DHCompat.isDistantHorizonsLoaded()) {
            ActiniumDHIrisCompat.registerAccessor();
            DHCompat.run();
        }
    }

    @SubscribeEvent
    public void onF3Text(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        var strings = event.getRight();
        strings.add("");
        strings.add(String.format("%s%s Renderer (%s)", ChatFormatting.AQUA, "Actinium", ActiniumRuntime.version()));

        if (Minecraft.getMinecraft().isReducedDebug()) {
            return;
        }

        var renderer = ActiniumWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(renderer.getDebugStrings());
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());
                break;
            }
        }
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }

    public static SodiumGameOptions options() {
        return ActiniumRuntime.options();
    }
}

