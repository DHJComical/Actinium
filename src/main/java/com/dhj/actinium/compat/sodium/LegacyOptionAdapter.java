package com.dhj.actinium.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder;
import net.caffeinemc.mods.sodium.client.gui.text.ClientTranslatedText;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Converts the retained Embeddium option contract into the modern Sodium Config API without reflection.
 */
public final class LegacyOptionAdapter {
    /** Custom flag retained for legacy shader extensions until Iris installs its explicit hook. */
    public static final ResourceLocation SHADER_PIPELINE_RELOAD =
            new ResourceLocation("actinium", "legacy_option_flag.requires_shader_pipeline_reload");

    private final ConfigBuilder builder;
    private final boolean writable;
    private final Map<OptionStorage<?>, LegacyStorageHandler> storages = new IdentityHashMap<>();
    private final Set<ResourceLocation> registeredOptionIds;
    private final Predicate<String> translationKeyChecker;
    private final BiFunction<String, Object[], String> translator;

    /**
     * Creates a conversion session sharing option IDs across all pages in one Config registry.
     */
    public LegacyOptionAdapter(ConfigBuilder builder, boolean writable, Set<ResourceLocation> registeredOptionIds) {
        this(builder, writable, registeredOptionIds, I18n::hasKey, I18n::format);
    }

    /** Creates an adapter with an explicit client translation boundary for integration tests. */
    public LegacyOptionAdapter(ConfigBuilder builder, boolean writable, Set<ResourceLocation> registeredOptionIds,
                               Predicate<String> translationKeyChecker,
                               BiFunction<String, Object[], String> translator) {
        if (builder == null) {
            throw new IllegalArgumentException("Config builder must not be null");
        }
        if (registeredOptionIds == null) {
            throw new IllegalArgumentException("Registered option ID set must not be null");
        }
        this.builder = builder;
        this.writable = writable;
        this.registeredOptionIds = registeredOptionIds;
        this.translationKeyChecker = Objects.requireNonNull(translationKeyChecker,
                "Translation key checker must not be null");
        this.translator = Objects.requireNonNull(translator, "Client translator must not be null");
    }

    /** Adds every unique option from the supplied legacy pages in traversal order. */
    public void addPages(ModOptionsBuilder owner, List<OptionPage> pages) {
        for (OptionPage page : pages) {
            OptionPageBuilder targetPage = this.builder.createOptionPage().setName(this.convertText(page.getName()));
            boolean pageHasOptions = false;
            for (OptionGroup group : page.getGroups()) {
                OptionGroupBuilder targetGroup = this.builder.createOptionGroup();
                boolean groupHasOptions = false;
                for (Option<?> option : group.getOptions()) {
                    ResourceLocation id = requireId(option);
                    if (this.registeredOptionIds.add(id)) {
                        targetGroup.addOption(this.convertOption(option, id));
                        groupHasOptions = true;
                    }
                }
                if (groupHasOptions) {
                    targetPage.addOptionGroup(targetGroup);
                    pageHasOptions = true;
                }
            }
            if (!pageHasOptions) {
                throw new IllegalArgumentException("Legacy page '" + page.getId() + "' has no unique options");
            }
            owner.addPage(targetPage);
        }
    }

    private OptionBuilder convertOption(Option<?> option, ResourceLocation id) {
        if (option.getControl() instanceof TickBoxControl) {
            return this.convertBoolean(option, id);
        }
        if (option.getControl() instanceof SliderControl slider) {
            return this.convertSlider(option, id, slider);
        }
        if (option.getControl() instanceof CyclingControl<?> cycling) {
            return this.convertCycling(option, id, cycling);
        }
        throw new IllegalArgumentException("Unsupported legacy control '" + option.getControl().getClass().getName()
                + "' for option '" + id + "'");
    }

    private BooleanOptionBuilder convertBoolean(Option<?> source, ResourceLocation id) {
        Option<Boolean> option = castOption(source, Boolean.class, id);
        return this.applyCommon(this.builder.createBooleanOption(id), option)
                .setDefaultValue(option.getValue())
                .setBinding(value -> this.saveLegacy(option, value), option::getValue);
    }

    private IntegerOptionBuilder convertSlider(Option<?> source, ResourceLocation id, SliderControl slider) {
        Option<Integer> option = castOption(source, Integer.class, id);
        return this.applyCommon(this.builder.createIntegerOption(id), option)
                .setDefaultValue(option.getValue())
                .setRange(slider.getMin(), slider.getMax(), slider.getInterval())
                .setValueFormatter(value -> this.convertText(slider.getFormatter().format(value)))
                .setBinding(value -> this.saveLegacy(option, value), option::getValue);
    }

    private OptionBuilder convertCycling(Option<?> source, ResourceLocation id, CyclingControl<?> cycling) {
        Object[] values = cycling.getAllowedValues();
        if (values.length == 0) {
            throw new IllegalArgumentException("Legacy cycling option '" + id + "' has no allowed values");
        }
        if (values[0] instanceof Integer) {
            return this.convertIntegerCycling(source, id, cycling, values);
        }
        if (values[0] instanceof Enum<?> first) {
            return this.convertEnumCycling(source, id, cycling, first.getDeclaringClass());
        }
        throw new IllegalArgumentException("Unsupported legacy cycling value type '"
                + values[0].getClass().getName() + "' for option '" + id + "'");
    }

    private IntegerOptionBuilder convertIntegerCycling(Option<?> source, ResourceLocation id,
                                                        CyclingControl<?> cycling, Object[] values) {
        Option<Integer> option = castOption(source, Integer.class, id);
        int[] allowed = Arrays.stream(values).mapToInt(value -> (Integer) value).toArray();
        SteppedValidator validator = ArithmeticValues.of(id, allowed);
        TextComponent[] names = cycling.getNames();
        return this.applyCommon(this.builder.createIntegerOption(id), option)
                .setDefaultValue(option.getValue())
                .setValidator(validator)
                .setValueFormatter(value -> this.convertText(nameForInteger(id, value, allowed, names)))
                .setBinding(value -> this.saveLegacy(option, value), option::getValue);
    }

    private <E extends Enum<E>> EnumOptionBuilder<E> convertEnumCycling(Option<?> source, ResourceLocation id,
                                                                         CyclingControl<?> cycling,
                                                                         Class<E> enumClass) {
        Option<E> option = castEnumOption(source, enumClass, id);
        List<E> values = castEnumValues(cycling.getAllowedValues(), enumClass);
        TextComponent[] names = cycling.getNames();
        Map<E, ITextComponent> namesByValue = new IdentityHashMap<>();
        for (int index = 0; index < values.size(); index++) {
            namesByValue.put(values.get(index), this.convertText(names[index]));
        }
        return this.applyCommon(this.builder.createEnumOption(id, enumClass), option)
                .setDefaultValue(option.getValue())
                .setAllowedValues(new LinkedHashSet<>(values))
                .setElementNameProvider(value -> {
                    ITextComponent name = namesByValue.get(value);
                    if (name == null) {
                        throw new IllegalArgumentException("Missing legacy enum label for option '" + id
                                + "' and value '" + value + "'");
                    }
                    return name;
                })
                .setBinding(value -> this.saveLegacy(option, value), option::getValue);
    }

    private <B extends StatefulOptionBuilder<?>> B applyCommon(
            B target, Option<?> source) {
        target.setName(this.convertText(source.getName()))
                .setTooltip(this.convertText(source.getTooltip()))
                .setEnabledProvider(state -> this.writable && source.isAvailable())
                .setStorageHandler(this.storageFor(source.getStorage()))
                .setControlHiddenWhenDisabled(false)
                .setFlags(source.getFlags().stream().map(LegacyOptionAdapter::convertFlag)
                        .toArray(ResourceLocation[]::new));
        if (source.getImpact() != null) {
            target.setImpact(OptionImpact.valueOf(source.getImpact().name()));
        }
        return target;
    }

    private LegacyStorageHandler storageFor(OptionStorage<?> storage) {
        return this.storages.computeIfAbsent(storage, LegacyStorageHandler::new);
    }

    private static ResourceLocation requireId(Option<?> option) {
        if (option.getId() == null || !OptionIdentifier.isPresent(option.getId())) {
            throw new IllegalArgumentException("Legacy option '" + option.getName() + "' has no stable ID");
        }
        return new ResourceLocation(option.getId().getModId(), option.getId().getPath());
    }

    private static ResourceLocation convertFlag(OptionFlag flag) {
        if (flag == OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD) {
            return SHADER_PIPELINE_RELOAD;
        }
        return new ResourceLocation("sodium", "builtin_option_flag." + flag.name().toLowerCase(Locale.ROOT));
    }

    private ITextComponent convertText(TextComponent component) {
        if (component instanceof TextComponent.Literal literal) {
            return new TextComponentString(literal.text());
        }
        if (component instanceof TextComponent.Translatable translatable) {
            String key = translatable.keys().stream().filter(this.translationKeyChecker).findFirst()
                    .orElse(translatable.keys().get(0));
            Object[] args = translatable.args().stream()
                    .map(argument -> argument instanceof TextComponent nested ? this.convertText(nested) : argument)
                    .toArray();
            return new ClientTranslatedText(key, this.translator, args);
        }
        if (component instanceof TextComponent.Styled styled) {
            return this.convertText(styled.inner());
        }
        throw new IllegalArgumentException("Unsupported legacy text component: " + component.getClass().getName());
    }

    private static TextComponent nameForInteger(ResourceLocation id, int value, int[] allowed,
                                                TextComponent[] names) {
        for (int index = 0; index < allowed.length; index++) {
            if (allowed[index] == value) {
                return names[index];
            }
        }
        throw new IllegalArgumentException("Value '" + value + "' is not allowed for option '" + id + "'");
    }

    private <V> void saveLegacy(Option<V> option, V value) {
        this.storageFor(option.getStorage()).record(option.getFlags());
        option.setValue(value);
        option.applyChanges();
    }

    private static <V> Option<V> castOption(Option<?> option, Class<V> valueClass, ResourceLocation id) {
        Object value = option.getValue();
        if (!valueClass.isInstance(value)) {
            throw new IllegalArgumentException("Legacy option '" + id + "' returned "
                    + value.getClass().getName() + " instead of " + valueClass.getName());
        }
        @SuppressWarnings("unchecked")
        Option<V> typed = (Option<V>) option;
        return typed;
    }

    private static <E extends Enum<E>> Option<E> castEnumOption(Option<?> option, Class<E> enumClass,
                                                                ResourceLocation id) {
        return castOption(option, enumClass, id);
    }

    private static <E extends Enum<E>> List<E> castEnumValues(Object[] values, Class<E> enumClass) {
        List<E> typed = new ArrayList<>(values.length);
        for (Object value : values) {
            typed.add(enumClass.cast(value));
        }
        return typed;
    }

    private record ArithmeticValues(int min, int max, int step) implements SteppedValidator {
        private static ArithmeticValues of(ResourceLocation id, int[] values) {
            if (values.length == 1) {
                return new ArithmeticValues(values[0], values[0], 1);
            }
            int step = values[1] - values[0];
            if (step <= 0) {
                throw new IllegalArgumentException("Legacy integer cycle must be strictly increasing for option '"
                        + id + "'");
            }
            for (int index = 2; index < values.length; index++) {
                if (values[index] - values[index - 1] != step) {
                    throw new IllegalArgumentException("Legacy integer cycle is not evenly stepped for option '"
                            + id + "'");
                }
            }
            return new ArithmeticValues(values[0], values[values.length - 1], step);
        }
    }

    private static final class LegacyStorageHandler implements StorageEventHandler {
        private final OptionStorage<?> storage;
        private final Set<OptionFlag> pendingFlags = new LinkedHashSet<>();

        private LegacyStorageHandler(OptionStorage<?> storage) {
            this.storage = storage;
        }

        private void record(Collection<OptionFlag> flags) {
            this.pendingFlags.addAll(flags);
        }

        @Override
        public void afterSave() {
            Set<OptionFlag> flags = Set.copyOf(this.pendingFlags);
            this.pendingFlags.clear();
            this.storage.save(flags);
        }
    }
}
