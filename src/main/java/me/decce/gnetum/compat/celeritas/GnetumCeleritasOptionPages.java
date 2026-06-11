package me.decce.gnetum.compat.celeritas;

import me.decce.gnetum.Gnetum;
import me.decce.gnetum.GnetumConfig;
import me.decce.gnetum.PerformanceAnalyzer;
import me.decce.gnetum.Tags;
import me.decce.gnetum.gui.ConfigScreen;
import me.decce.gnetum.util.AnyBooleanValue;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.Control;
import org.taumc.celeritas.api.options.control.ControlElement;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.OptionStorage;

import java.util.List;

public final class GnetumCeleritasOptionPages {
    private static final OptionStorage<GnetumConfig> STORAGE = new GnetumOptionStorage();
    private static final OptionIdentifier<Void> PAGE_ID = OptionIdentifier.create(Tags.MOD_ID, "options");
    private static final OptionIdentifier<Void> GENERAL_GROUP_ID = OptionIdentifier.create(Tags.MOD_ID, "general");
    private static final OptionIdentifier<Void> DETAIL_GROUP_ID = OptionIdentifier.create(Tags.MOD_ID, "detail");

    private GnetumCeleritasOptionPages() {
    }

    public static OptionPage page() {
        return new OptionPage(PAGE_ID, TextComponent.translatable("gnetum.celeritas.page"), List.of(
                OptionGroup.createBuilder()
                        .setId(GENERAL_GROUP_ID)
                        .add(OptionImpl.createBuilder(boolean.class, STORAGE)
                                .setId(OptionIdentifier.create(Tags.MOD_ID, "enabled", boolean.class))
                                .setName(TextComponent.translatable("gnetum.config.enabled"))
                                .setTooltip(TextComponent.translatable("gnetum.celeritas.enabled.tooltip"))
                                .setControl(TickBoxControl::new)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding((cfg, value) -> cfg.enabled.value = value ? AnyBooleanValue.ON : AnyBooleanValue.OFF, cfg -> cfg.enabled.get())
                                .build())
                        .add(OptionImpl.createBuilder(boolean.class, STORAGE)
                                .setId(OptionIdentifier.create(Tags.MOD_ID, "show_hud_fps", boolean.class))
                                .setName(TextComponent.translatable("gnetum.config.showFps"))
                                .setTooltip(TextComponent.translatable("gnetum.celeritas.show_fps.tooltip"))
                                .setControl(TickBoxControl::new)
                                .setImpact(OptionImpact.LOW)
                                .setBinding((cfg, value) -> cfg.showHudFps.value = value ? AnyBooleanValue.ON : AnyBooleanValue.OFF, cfg -> cfg.showHudFps.get())
                                .build())
                        .add(OptionImpl.createBuilder(int.class, STORAGE)
                                .setId(OptionIdentifier.create(Tags.MOD_ID, "number_of_passes", int.class))
                                .setName(TextComponent.translatable("gnetum.config.numberOfPasses"))
                                .setTooltip(TextComponent.translatable("gnetum.config.numberOfPasses.tooltip"))
                                .setControl(option -> new SliderControl(option, 2, 10, 1, ControlValueFormatter.number()))
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding((cfg, value) -> cfg.numberOfPasses = value, cfg -> cfg.numberOfPasses)
                                .build())
                        .add(OptionImpl.createBuilder(int.class, STORAGE)
                                .setId(OptionIdentifier.create(Tags.MOD_ID, "max_fps", int.class))
                                .setName(TextComponent.translatable("gnetum.config.maxFps"))
                                .setTooltip(TextComponent.translatable("gnetum.config.maxFps.tooltip"))
                                .setControl(option -> new SliderControl(option, 1, GnetumConfig.UNLIMITED_FPS, 1, value -> value >= GnetumConfig.UNLIMITED_FPS
                                        ? TextComponent.translatable("gnetum.celeritas.max_fps.unlimited")
                                        : TextComponent.literal(value + " fps")))
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding((cfg, value) -> cfg.maxFps = value, cfg -> cfg.maxFps)
                                .build())
                        .build(),
                OptionGroup.createBuilder()
                        .setId(DETAIL_GROUP_ID)
                        .add(OptionImpl.createBuilder(boolean.class, STORAGE)
                                .setId(OptionIdentifier.create(Tags.MOD_ID, "open_config", boolean.class))
                                .setName(TextComponent.translatable("gnetum.celeritas.open_config.name"))
                                .setTooltip(TextComponent.translatable("gnetum.celeritas.open_config.tooltip"))
                                .setControl(option -> new OpenScreenControl(option,
                                        TextComponent.translatable("gnetum.celeritas.open_config.button"),
                                        () -> {
                                            var mc = Minecraft.getMinecraft();
                                            mc.displayGuiScreen(new ConfigScreen(mc.currentScreen, PerformanceAnalyzer.analyze()));
                                        }))
                                .setBinding((cfg, value) -> { }, cfg -> false)
                                .build())
                        .build()
        ));
    }

    private static final class GnetumOptionStorage implements OptionStorage<GnetumConfig> {
        @Override
        public GnetumConfig getData() {
            return Gnetum.config;
        }

        @Override
        public void save() {
            Gnetum.config.save();
        }
    }

    private static final class OpenScreenControl implements Control<Boolean> {
        private final Option<Boolean> option;
        private final TextComponent buttonLabel;
        private final Runnable action;

        private OpenScreenControl(Option<Boolean> option, TextComponent buttonLabel, Runnable action) {
            this.option = option;
            this.buttonLabel = buttonLabel;
            this.action = action;
        }

        @Override
        public Option<Boolean> getOption() {
            return option;
        }

        @Override
        public ControlElement<Boolean> createElement(Dim2i dim) {
            return new OpenScreenControlElement(option, dim, buttonLabel, action);
        }

        @Override
        public int getMaxWidth() {
            return 74;
        }
    }

    private static final class OpenScreenControlElement extends ControlElement<Boolean> {
        private final TextComponent buttonLabel;
        private final Runnable action;
        private final Dim2i buttonDim;

        private OpenScreenControlElement(Option<Boolean> option, Dim2i dim, TextComponent buttonLabel, Runnable action) {
            super(option, dim);
            this.buttonLabel = buttonLabel;
            this.action = action;
            this.buttonDim = new Dim2i(dim.getLimitX() - 68, dim.y() + 2, 62, dim.height() - 4);
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);

            boolean hovered = buttonDim.containsCursor(mouseX, mouseY);
            int background = hovered ? 0xE0202020 : 0x90000000;

            drawContext.fill(buttonDim.x(), buttonDim.y(), buttonDim.getLimitX(), buttonDim.getLimitY(), background);
            drawContext.drawBorder(buttonDim.x(), buttonDim.y(), buttonDim.getLimitX(), buttonDim.getLimitY(), hovered ? DefaultColors.ELEMENT_ACTIVATED : 0x30FFFFFF);

            int textX = buttonDim.getCenterX() - (drawContext.getStringWidth(buttonLabel) / 2);
            int textY = buttonDim.getCenterY() - 4;
            drawContext.drawString(buttonLabel, textX, textY, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            if (option.isAvailable() && button == 0 && dim.containsCursor(mouseX, mouseY)) {
                action.run();
                context.playClickSound();
                return true;
            }

            return false;
        }
    }
}
