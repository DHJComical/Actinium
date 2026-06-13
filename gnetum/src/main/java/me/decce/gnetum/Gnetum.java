package me.decce.gnetum;

import me.decce.gnetum.compat.celeritas.GnetumCeleritasOptionsListener;
import me.decce.gnetum.gui.BaseScreen;
import me.decce.gnetum.gui.ConfigScreen;
import me.decce.gnetum.hud.VanillaHuds;
import me.decce.gnetum.util.AnyBooleanValue;
import me.decce.gnetum.util.time.GlfwTimeSource;
import me.decce.gnetum.util.time.JavaTimeSource;
import me.decce.gnetum.util.time.TimeSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;

public class Gnetum {
    private static MutableScaledResolution scaledResolution;
    private static TimeSource timeSource;
    private static FpsCounter fpsCounter;
    public static GnetumConfig config;
    public static final String HAND_ELEMENT = "gnetum:minecraft_hand";
    public static final String OTHER_MODS = "gnetum_unknown";
    public static PassManager passManager;
    public static UncachedElements uncachedElements;
    public static String currentElement;
    public static ElementType currentElementType;
    public static boolean rendering;
    public static boolean renderingCanceled;
    public static boolean isRenderingHelmet;
    public static long lastSwapNanos;

    public static final KeyBinding KEYBIND = new KeyBinding("gnetum.config.keyMapping", KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_END, "key.categories.misc");

    public static Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new Gnetum());
    }

    public static MutableScaledResolution getScaledResolution() {
        if (scaledResolution == null) {
            scaledResolution = new MutableScaledResolution();
        }
        return scaledResolution;
    }

    public static TimeSource getTimeSource() {
        if (timeSource == null) {
            if (GlfwTimeSource.isAvailable()) {
                timeSource = new GlfwTimeSource();
            }
            else {
                timeSource = new JavaTimeSource();
            }
        }
        return timeSource;
    }

    public static FpsCounter getFpsCounter() {
        if (fpsCounter == null) {
            fpsCounter = new FpsCounter();
        }
        return fpsCounter;
    }

    public static CacheSetting getCacheSetting(String vanillaOverlay) {
        if (!config.mapVanillaElements.containsKey(vanillaOverlay)) {
            config.mapVanillaElements.put(vanillaOverlay, new CacheSetting(SuggestedPass.get(vanillaOverlay)));
        }
        return Gnetum.config.mapVanillaElements.get(vanillaOverlay);
    }

    public static CacheSetting getCacheSetting(String moddedOverlay, ElementType type) {
        if (type == ElementType.VANILLA) return getCacheSetting(moddedOverlay);
        var map = type == ElementType.PRE ? config.mapModdedElementsPre : config.mapModdedElementsPost;
        if (!map.containsKey(moddedOverlay)) {
            map.put(moddedOverlay, new CacheSetting(type == ElementType.PRE ? 1 : config.numberOfPasses));
            config.validate();
        }
        return map.get(moddedOverlay);
    }

    public static void disableCachingForCurrentElement(String reason) {
        if (currentElement == null || currentElementType == null) return;
        CacheSetting cacheSetting = getCacheSetting(currentElement, currentElementType);
        if (cacheSetting.enabled.get() && cacheSetting.enabled.value == AnyBooleanValue.AUTO) {
            LOGGER.info("Disabling caching for element {}. Reason: {}", currentElement, reason);
            cacheSetting.enabled.defaultValue = false;
            FramebufferManager.getInstance().dropCurrentFrame();
        }
    }

    public static void construct(FMLConstructionEvent event) {
        try {
            OptionGUIConstructionEvent.BUS.addListener(GnetumCeleritasOptionsListener::onCeleritasOptionsConstruct);
            LOGGER.info("Registered Gnetum options page with the Celeritas GUI");
        } catch (Throwable t) {
            if (t instanceof NoClassDefFoundError) {
                LOGGER.error("Celeritas is too old to host Gnetum options");
            } else {
                LOGGER.error("Unable to register Gnetum with the Celeritas GUI", t);
            }
        }
    }

    public static void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        uncachedElements = new UncachedElements();
        config = GnetumConfig.load(event.getModConfigurationDirectory().toPath());
        passManager = new PassManager();
        VanillaHuds.init();
    }

    public static void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(KEYBIND);
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) {
        if (KEYBIND.isPressed() && !(Minecraft.getMinecraft().currentScreen instanceof BaseScreen)) {
            Minecraft.getMinecraft().displayGuiScreen(new ConfigScreen(PerformanceAnalyzer.analyze()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // HIGH to run after VintageFix's listener
    public void onRenderF3(RenderGameOverlayEvent.Text event) {
        boolean shouldEnable = Gnetum.config.enabled.get();
        boolean enabled = Gnetum.config.isEnabled();
        if (!shouldEnable || !Gnetum.config.showHudFps.get()) {
            return;
        }
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            String str;
            if (enabled) {
                str = String.format("HUD: %d fps (%d passes, max %s)", Gnetum.getFpsCounter().getFps(), Gnetum.config.numberOfPasses, Gnetum.config.maxFps == GnetumConfig.UNLIMITED_FPS ? "unlimited" : Gnetum.config.maxFps);
            }
            else {
                str = "HUD: not cached because framebuffer is not enabled (try disabling Fast Render)";
            }
            if (event.getLeft().size() > 2) {
                event.getLeft().add(2, str);
            }
            else {
                event.getLeft().add(str);
            }
        }
    }
}
