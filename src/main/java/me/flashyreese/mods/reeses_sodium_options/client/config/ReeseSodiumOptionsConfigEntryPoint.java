package me.flashyreese.mods.reeses_sodium_options.client.config;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Actinium's own RSO config page builder: registers Reese's Sodium Options'
 * own configuration (enabled, appearance, behavior) directly as an embeddium
 * {@link OptionPage}, without the Sodium config model. The support group
 * (donation buttons) is removed per the user's requirement.
 */
public final class ReeseSodiumOptionsConfigEntryPoint {
    private static final String MOD_ID = "reeses-sodium-options";

    private ReeseSodiumOptionsConfigEntryPoint() {
    }

    /** Builds the RSO config page (embeddium model). */
    public static OptionPage createOptionsPage() {
        List<OptionGroup> groups = List.of(
                createGeneralOptions(),
                createAppearanceOptions(),
                createBehaviorOptions());
        return new OptionPage(
                OptionIdentifier.create(MOD_ID, "rso_options"),
                TextComponent.translatable("rso.options.page"),
                groups);
    }

    private static OptionGroup createGeneralOptions() {
        return OptionGroup.createBuilder()
                .setId(OptionIdentifier.create(MOD_ID, "general"))
                .add(booleanOption("enabled",
                        () -> ReeseSodiumOptionsConfig.config().isEnabled(),
                        value -> ReeseSodiumOptionsConfig.config().setEnabled(value)))
                .build();
    }

    private static OptionGroup createAppearanceOptions() {
        return OptionGroup.createBuilder()
                .setId(OptionIdentifier.create(MOD_ID, "appearance"))
                .add(booleanOption("tab_header_icons",
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaderIcons(),
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaderIcons(value)))
                .add(booleanOption("tab_header_version_labels",
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels(),
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaderVersionLabels(value)))
                .add(enumOption("tab_header_collapse_mode", ReeseSodiumOptionsConfig.TabHeaderCollapseMode.class,
                        () -> ReeseSodiumOptionsConfig.config().getTabHeaderCollapseMode(),
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaderCollapseMode(value)))
                .add(booleanOption("tab_headers",
                        () -> ReeseSodiumOptionsConfig.config().isTabHeaders(),
                        value -> ReeseSodiumOptionsConfig.config().setTabHeaders(value)))
                .add(booleanOption("collapse_single_page_groups",
                        () -> ReeseSodiumOptionsConfig.config().isCollapseSinglePageGroups(),
                        value -> ReeseSodiumOptionsConfig.config().setCollapseSinglePageGroups(value)))
                .add(booleanOption("collapsible_groups",
                        () -> ReeseSodiumOptionsConfig.config().isCollapsibleGroups(),
                        value -> ReeseSodiumOptionsConfig.config().setCollapsibleGroups(value)))
                .add(intOption("tooltip_delay",
                        () -> ReeseSodiumOptionsConfig.config().getTooltipDelayMs(),
                        value -> ReeseSodiumOptionsConfig.config().setTooltipDelayMs(value),
                        ReeseSodiumOptionsConfig.MIN_TOOLTIP_DELAY_MS,
                        ReeseSodiumOptionsConfig.MAX_TOOLTIP_DELAY_MS,
                        100,
                        value -> TextComponent.translatable("rso.options.value.milliseconds", value)))
                .add(booleanOption("tooltip_option_ids",
                        () -> ReeseSodiumOptionsConfig.config().isTooltipOptionIds(),
                        value -> ReeseSodiumOptionsConfig.config().setTooltipOptionIds(value)))
                .add(booleanOption("color_themes",
                        () -> ReeseSodiumOptionsConfig.config().isColorThemes(),
                        value -> ReeseSodiumOptionsConfig.config().setColorThemes(value)))
                .add(booleanOption("themed_headers_and_labels",
                        () -> ReeseSodiumOptionsConfig.config().isThemedHeadersAndLabels(),
                        value -> ReeseSodiumOptionsConfig.config().setThemedHeadersAndLabels(value)))
                .add(booleanOption("themed_tooltip_borders",
                        () -> ReeseSodiumOptionsConfig.config().isThemedTooltipBorders(),
                        value -> ReeseSodiumOptionsConfig.config().setThemedTooltipBorders(value)))
                .add(booleanOption("reduced_motion",
                        () -> ReeseSodiumOptionsConfig.config().isReducedMotion(),
                        value -> ReeseSodiumOptionsConfig.config().setReducedMotion(value)))
                .build();
    }

    private static OptionGroup createBehaviorOptions() {
        return OptionGroup.createBuilder()
                .setId(OptionIdentifier.create(MOD_ID, "behavior"))
                .add(booleanOption("reverse_cycling_controls",
                        () -> ReeseSodiumOptionsConfig.config().isReverseCyclingControls(),
                        value -> ReeseSodiumOptionsConfig.config().setReverseCyclingControls(value)))
                .add(booleanOption("shift_scroll_slider_adjustments",
                        () -> ReeseSodiumOptionsConfig.config().isShiftScrollSliderAdjustments(),
                        value -> ReeseSodiumOptionsConfig.config().setShiftScrollSliderAdjustments(value)))
                .add(intOption("search_result_limit",
                        () -> ReeseSodiumOptionsConfig.config().getSearchResultLimit(),
                        value -> ReeseSodiumOptionsConfig.config().setSearchResultLimit(value),
                        ReeseSodiumOptionsConfig.MIN_SEARCH_RESULT_LIMIT,
                        ReeseSodiumOptionsConfig.MAX_SEARCH_RESULT_LIMIT,
                        1,
                        value -> TextComponent.translatable("rso.options.value.results", value)))
                .add(booleanOption("hide_non_matching_options",
                        () -> ReeseSodiumOptionsConfig.config().isHideNonMatchingOptions(),
                        value -> ReeseSodiumOptionsConfig.config().setHideNonMatchingOptions(value)))
                .add(booleanOption("hide_non_matching_tabs",
                        () -> ReeseSodiumOptionsConfig.config().isHideNonMatchingTabs(),
                        value -> ReeseSodiumOptionsConfig.config().setHideNonMatchingTabs(value)))
                .add(enumOption("disabled_option_visibility", ReeseSodiumOptionsConfig.DisabledOptionVisibility.class,
                        () -> ReeseSodiumOptionsConfig.config().getDisabledOptionVisibility(),
                        value -> ReeseSodiumOptionsConfig.config().setDisabledOptionVisibility(value)))
                .add(enumOption("focus_border_mode", ReeseSodiumOptionsConfig.FocusBorderMode.class,
                        () -> ReeseSodiumOptionsConfig.config().getFocusBorderMode(),
                        value -> ReeseSodiumOptionsConfig.config().setFocusBorderMode(value)))
                .add(booleanOption("controller_guides",
                        () -> ReeseSodiumOptionsConfig.config().isControllerGuides(),
                        value -> ReeseSodiumOptionsConfig.config().setControllerGuides(value)))
                .add(booleanOption("reset_button_overlay",
                        () -> ReeseSodiumOptionsConfig.config().isResetButtonOverlay(),
                        value -> ReeseSodiumOptionsConfig.config().setResetButtonOverlay(value)))
                .add(booleanOption("undo_button_overlay",
                        () -> ReeseSodiumOptionsConfig.config().isUndoButtonOverlay(),
                        value -> ReeseSodiumOptionsConfig.config().setUndoButtonOverlay(value)))
                .add(booleanOption("always_show_action_buttons",
                        () -> ReeseSodiumOptionsConfig.config().isAlwaysShowActionButtons(),
                        value -> ReeseSodiumOptionsConfig.config().setAlwaysShowActionButtons(value)))
                .build();
    }

    private static OptionImpl<Object, Boolean> booleanOption(String name,
                                                             Supplier<Boolean> getter,
                                                             Consumer<Boolean> setter) {
        RsoStorage storage = new RsoStorage();
        return OptionImpl.createBuilder(boolean.class, storage)
                .setId(OptionIdentifier.create(MOD_ID, name, boolean.class))
                .setName(TextComponent.translatable("rso.options." + name + ".name"))
                .setTooltip(TextComponent.translatable("rso.options." + name + ".tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding((ignored, value) -> setter.accept(value), ignored -> getter.get())
                .setDefaultValue(getter.get())
                .build();
    }

    private static OptionImpl<Object, Integer> intOption(String name,
                                                         Supplier<Integer> getter,
                                                         Consumer<Integer> setter,
                                                         int min, int max, int step,
                                                         ControlValueFormatter formatter) {
        RsoStorage storage = new RsoStorage();
        return OptionImpl.createBuilder(int.class, storage)
                .setId(OptionIdentifier.create(MOD_ID, name, int.class))
                .setName(TextComponent.translatable("rso.options." + name + ".name"))
                .setTooltip(TextComponent.translatable("rso.options." + name + ".tooltip"))
                .setControl(option -> new SliderControl(option, min, max, step, formatter))
                .setBinding((ignored, value) -> setter.accept(value), ignored -> getter.get())
                .setDefaultValue(getter.get())
                .build();
    }

    private static <E extends Enum<E>> OptionImpl<Object, E> enumOption(String name,
                                                                         Class<E> enumClass,
                                                                         Supplier<E> getter,
                                                                         Consumer<E> setter) {
        RsoStorage storage = new RsoStorage();
        E[] constants = enumClass.getEnumConstants();
        TextComponent[] names = new TextComponent[constants.length];
        for (int i = 0; i < constants.length; i++) {
            names[i] = TextComponent.translatable("rso.options." + name + ".value." + enumId(constants[i]));
        }
        return OptionImpl.createBuilder(enumClass, storage)
                .setId(OptionIdentifier.create(MOD_ID, name, enumClass))
                .setName(TextComponent.translatable("rso.options." + name + ".name"))
                .setTooltip(TextComponent.translatable("rso.options." + name + ".tooltip"))
                .setControl(option -> new CyclingControl<>(option, enumClass, names))
                .setBinding((ignored, value) -> setter.accept(value), ignored -> getter.get())
                .setDefaultValue(getter.get())
                .build();
    }

    private static String enumId(Object value) {
        if (value instanceof ReeseSodiumOptionsConfig.TabHeaderCollapseMode mode) {
            return mode.id();
        }
        if (value instanceof ReeseSodiumOptionsConfig.DisabledOptionVisibility visibility) {
            return visibility.id();
        }
        if (value instanceof ReeseSodiumOptionsConfig.FocusBorderMode mode) {
            return mode.id();
        }
        return value.toString().toLowerCase(java.util.Locale.ROOT);
    }

    /** Placeholder storage: RSO config reads/writes the static ConfigData; the storage only persists on save. */
    private static final class RsoStorage implements OptionStorage<Object> {
        @Override
        public Object getData() {
            return this;
        }

        @Override
        public void save() {
            ReeseSodiumOptionsConfig.writeConfig();
        }

        @Override
        public void save(Set<org.embeddedt.embeddium.api.options.structure.OptionFlag> flags) {
            this.save();
        }
    }
}
