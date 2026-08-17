package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.ActiniumTheme;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.Objects;

/**
 * RSO 行类视图：包装 embeddium {@link Option}，暴露与上游一致的
 * pending/applied 语义与类型分派辅助。行类只依赖本接口与
 * {@link RsoModOptions}，不接触任何 Sodium 配置模型。
 */
public final class RsoOption {
    private final Option<?> delegate;

    public RsoOption(Option<?> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Option must not be null");
    }

    /** 返回底层 embeddium 选项（供控件类型检查使用）。 */
    public Option<?> unwrap() {
        return this.delegate;
    }

    /** 返回稳定的选项 ID（来自 embeddium OptionIdentifier）。 */
    public String rso$getId() {
        org.embeddedt.embeddium.api.options.OptionIdentifier<?> id = this.delegate.getId();
        return id == null ? "" : id.toString();
    }

    public ITextComponent getName() {
        return convertText(this.delegate.getName());
    }

    public ITextComponent getTooltip() {
        return convertText(this.delegate.getTooltip());
    }

    public boolean isEnabled() {
        return this.delegate.isAvailable();
    }

    public boolean hasChanged() {
        return this.delegate.hasChanged();
    }

    /** 返回用户尚未应用的 pending 值。 */
    public Object getPendingValue() {
        return this.delegate.getValue();
    }

    /** 返回最近一次应用后的基线值。 */
    public Object getAppliedValue() {
        return this.delegate.getAppliedValue();
    }

    /** 用新的 pending 值替换当前编辑值。 */
    public void modifyValue(Object value) {
        this.setValue(value);
    }

    @SuppressWarnings("unchecked")
    private void setValue(Object value) {
        ((Option<Object>) this.delegate).setValue(value);
    }

    /** 撤销 pending 改动，回到已应用基线。 */
    public void undo() {
        this.delegate.reset();
    }

    /** 恢复为声明默认值。 */
    public void resetToDefault() {
        this.delegate.resetToDefault();
    }

    /** 返回声明默认值。 */
    public Object getDefaultValue() {
        return this.delegate.getDefaultValue();
    }

    /** 返回性能影响标签（可能为 null）。 */
    public String getImpactName() {
        org.embeddedt.embeddium.api.options.structure.OptionImpact impact = this.delegate.getImpact();
        return impact == null ? null : impact.name();
    }

    /** 控件类型分派：是否为 tick-box（布尔）。 */
    public boolean isTickBox() {
        return this.delegate.getControl() instanceof org.embeddedt.embeddium.api.options.control.TickBoxControl;
    }

    /** 控件类型分派：是否为滑块（整数）。 */
    public boolean isSlider() {
        return this.delegate.getControl() instanceof org.embeddedt.embeddium.api.options.control.SliderControl;
    }

    /** 控件类型分派：是否为循环控件（枚举/离散值）。 */
    public boolean isCycling() {
        return this.delegate.getControl() instanceof org.embeddedt.embeddium.api.options.control.CyclingControl;
    }

    /** 控件类型分派：是否为外部按钮（打开独立屏幕）。 */
    public boolean isExternalButton() {
        return this.delegate.getControl() instanceof org.embeddedt.embeddium.api.options.control.ExternalButtonControl;
    }

    /** 返回滑块范围（isSlider 时为 true 才有意义）。 */
    public int sliderMin() {
        return ((org.embeddedt.embeddium.api.options.control.SliderControl) this.delegate.getControl()).getMin();
    }

    /** 返回滑块范围上限（isSlider 时为 true 才有意义）。 */
    public int sliderMax() {
        return ((org.embeddedt.embeddium.api.options.control.SliderControl) this.delegate.getControl()).getMax();
    }

    /** 返回滑块步长（isSlider 时为 true 才有意义）。 */
    public int sliderInterval() {
        return ((org.embeddedt.embeddium.api.options.control.SliderControl) this.delegate.getControl()).getInterval();
    }

    /** 返回滑块值的格式化文本（isSlider 时为 true 才有意义）。 */
    public ITextComponent formatSliderValue(Object value) {
        org.embeddedt.embeddium.api.options.control.SliderControl control =
                (org.embeddedt.embeddium.api.options.control.SliderControl) this.delegate.getControl();
        return convertText(control.getFormatter().format((Integer) value));
    }

    /** 返回循环控件的显示名（isCycling 时为 true 才有意义）。 */
    public ITextComponent getElementName(Object value) {
        org.embeddedt.embeddium.api.options.control.CyclingControl<Object> control =
                (org.embeddedt.embeddium.api.options.control.CyclingControl<Object>) this.delegate.getControl();
        Object[] allowed = control.getAllowedValues();
        TextComponent[] names = control.getNames();
        for (int i = 0; i < allowed.length; i++) {
            if (Objects.equals(allowed[i], value)) {
                return convertText(names[i]);
            }
        }
        return new TextComponentString(value.toString());
    }

    /** 返回循环控件是否允许该值（isCycling 时为 true 才有意义）。 */
    public boolean isValueAllowed(Object value) {
        org.embeddedt.embeddium.api.options.control.CyclingControl<Object> control =
                (org.embeddedt.embeddium.api.options.control.CyclingControl<Object>) this.delegate.getControl();
        for (Object allowed : control.getAllowedValues()) {
            if (Objects.equals(allowed, value)) {
                return true;
            }
        }
        return false;
    }

    /** 返回循环控件的全部值（isCycling 时为 true 才有意义）。 */
    public Object[] getAllowedValues() {
        org.embeddedt.embeddium.api.options.control.CyclingControl<Object> control =
                (org.embeddedt.embeddium.api.options.control.CyclingControl<Object>) this.delegate.getControl();
        return control.getAllowedValues();
    }

    /** 返回外部按钮的屏幕消费者（isExternalButton 时为 true 才有意义）。 */
    public java.util.function.Consumer<net.minecraft.client.gui.GuiScreen> getCurrentScreenConsumer() {
        return ((org.embeddedt.embeddium.api.options.control.ExternalButtonControl) this.delegate.getControl())
                .getScreenConsumer();
    }

    /** 是否应隐藏控件（embeddium 无此概念，恒为 false）。 */
    public boolean shouldHideControl() {
        return false;
    }

    private static ITextComponent convertText(TextComponent component) {
        if (component instanceof TextComponent.Translatable translatable) {
            return new TextComponentTranslation(translatable.keys().get(0),
                    translatable.args().stream()
                            .map(arg -> arg instanceof TextComponent nested ? convertText(nested) : arg)
                            .toArray());
        }
        if (component instanceof TextComponent.Literal literal) {
            return new TextComponentString(literal.text());
        }
        if (component instanceof TextComponent.Styled styled) {
            return convertText(styled.inner());
        }
        return new TextComponentString(component.toString());
    }
}
