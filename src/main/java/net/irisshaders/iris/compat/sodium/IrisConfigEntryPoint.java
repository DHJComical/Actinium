package net.irisshaders.iris.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ExternalPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.client.gui.text.ClientTranslatedText;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.option.IrisVideoSettings;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Exposes the supported Iris settings through Sodium's explicit Config API.
 * The entrypoint deliberately binds only the legacy Iris shadow distance value.
 */
public final class IrisConfigEntryPoint implements ConfigEntryPoint {
    private static final Logger LOGGER = LogManager.getLogger("Actinium-Iris-Config");
    private static final ResourceLocation ICON = new ResourceLocation("iris", "textures/gui/config-icon.png");
    private static final ResourceLocation SHADOW_DISTANCE = new ResourceLocation("iris", "shadow_distance");
    private final Consumer<Integer> shadowSaver;
    private final Supplier<Integer> shadowLoader;
    private final Consumer<GuiScreen> shaderPackOpener;
    private final BiFunction<String, Object[], String> translator;

    /** Creates the production entrypoint backed by Iris' legacy settings and screen. */
    public IrisConfigEntryPoint() {
        this(value -> {
            IrisVideoSettings.shadowDistance = value;
            saveIrisConfig();
        }, () -> IrisVideoSettings.shadowDistance,
                parent -> Minecraft.getMinecraft().displayGuiScreen(new ShaderPackScreen(parent)),
                I18n::format);
    }

    /** Allows direct logic tests to supply persistence and screen boundaries without client bootstrapping. */
    public IrisConfigEntryPoint(Consumer<Integer> shadowSaver, Supplier<Integer> shadowLoader,
                                Consumer<GuiScreen> shaderPackOpener) {
        this(shadowSaver, shadowLoader, shaderPackOpener, I18n::format);
    }

    /** Creates an integration boundary with an explicit client translator. */
    public IrisConfigEntryPoint(Consumer<Integer> shadowSaver, Supplier<Integer> shadowLoader,
                                Consumer<GuiScreen> shaderPackOpener,
                                BiFunction<String, Object[], String> translator) {
        this.shadowSaver = Objects.requireNonNull(shadowSaver, "Iris shadow saver must not be null");
        this.shadowLoader = Objects.requireNonNull(shadowLoader, "Iris shadow loader must not be null");
        this.shaderPackOpener = Objects.requireNonNull(shaderPackOpener, "Iris shader pack opener must not be null");
        this.translator = Objects.requireNonNull(translator, "Iris client translator must not be null");
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ModOptionsBuilder owner = builder.registerModOptions("iris", Iris.MODNAME, "1.12")
                .setNonTintedIcon(ICON)
                .setColorTheme(builder.createColorTheme().setFullThemeRGB(0x6A5ACD, 0x8A79E8, 0x40358A));

        IntegerOptionBuilder shadow = builder.createIntegerOption(SHADOW_DISTANCE)
                .setName(this.text("options.iris.shadowDistance"))
                .setTooltip(value -> this.text(
                        IrisVideoSettings.isShadowDistanceSliderEnabled()
                                ? "options.iris.shadowDistance.enabled"
                                : "options.iris.shadowDistance.disabled"))
                .setRange(new Range(0, 256, 1))
                .setDefaultValue(32)
                .setStorageHandler(() -> { })
                .setValueFormatter(value -> new TextComponentString(Integer.toString(value)))
                .setEnabledProvider(this::isIrisEnabled)
                .setBinding(this.shadowSaver, this.shadowLoader);
        owner.addPage(builder.createOptionPage()
                .setName(this.text("options.iris.title"))
                .addOption(shadow));

        ExternalPageBuilder shaderPacks = builder.createExternalPage()
                .setName(this.text("options.iris.shaderPackSelection"))
                .setScreenConsumer(this::openShaderPackScreen);
        owner.addPage(shaderPacks);
    }

    private Boolean isIrisEnabled(ConfigState ignored) {
        return Iris.enabled;
    }

    private ClientTranslatedText text(String key) {
        return new ClientTranslatedText(key, this.translator);
    }

    private void openShaderPackScreen(GuiScreen parent) {
        this.shaderPackOpener.accept(parent);
    }

    private static void saveIrisConfig() {
        try {
            Iris.getIrisConfig().save();
        } catch (IOException exception) {
            LOGGER.error("Failed to save Iris configuration after applying Sodium options", exception);
        }
    }
}
