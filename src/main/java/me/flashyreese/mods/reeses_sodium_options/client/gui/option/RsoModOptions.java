package me.flashyreese.mods.reeses_sodium_options.client.gui.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.ActiniumTheme;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;

/**
 * Actinium 自研的 mod 选项聚合模型：把 embeddium 的 {@link OptionPage} 列表
 * 按 modId 分组，为 RSO 的 tab 体系提供 configId/name/version/icon/theme/pages
 * 视图。不依赖任何 Sodium 配置模型。
 */
public final class RsoModOptions {
    private final String configId;
    private final String name;
    private final String version;
    private final net.minecraft.util.ResourceLocation icon;
    private final boolean iconMonochrome;
    private final ActiniumTheme theme;
    private final List<RsoPage> pages;

    public RsoModOptions(String configId, String name, String version,
                         net.minecraft.util.ResourceLocation icon, boolean iconMonochrome,
                         ActiniumTheme theme, List<RsoPage> pages) {
        this.configId = configId;
        this.name = name;
        this.version = version;
        this.icon = icon;
        this.iconMonochrome = iconMonochrome;
        this.theme = theme;
        this.pages = List.copyOf(pages);
    }

    /** 从 embeddium 页面构建（名称/版本取 mod 元数据或回退到 configId）。 */
    public static RsoModOptions fromPage(OptionPage page) {
        String modId = page.getId().getModId();
        String displayName = extractText(page.getName());
        return new RsoModOptions(
                modId,
                displayName,
                "1.12",
                null,
                false,
                ActiniumTheme.defaultFor(modId),
                List.of(new RsoPage(page))
        );
    }

    /** 把同一 modId 下的多页聚合为一个 RSO 视图。 */
    public static RsoModOptions aggregate(String configId, List<OptionPage> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate empty page list for '" + configId + "'");
        }
        String displayName = extractText(pages.get(0).getName());
        List<RsoPage> rsoPages = pages.stream().map(RsoPage::new).toList();
        return new RsoModOptions(configId, displayName, "1.12", null, false,
                ActiniumTheme.defaultFor(configId), rsoPages);
    }

    /** 展开回底层 embeddium 页面（供 apply/undo 遍历）。 */
    public void unwrapPages(List<OptionPage> target) {
        for (RsoPage page : this.pages) {
            target.add(page.delegate);
        }
    }

    private static String extractText(TextComponent component) {
        if (component instanceof TextComponent.Translatable translatable) {
            for (String key : translatable.keys()) {
                if (I18n.hasKey(key)) {
                    return I18n.format(key, translatable.args().toArray());
                }
            }
            return translatable.keys().get(0);
        }
        return component.toString();
    }

    public String configId() {
        return this.configId;
    }

    public String name() {
        return this.name;
    }

    public String version() {
        return this.version;
    }

    public net.minecraft.util.ResourceLocation icon() {
        return this.icon;
    }

    public boolean iconMonochrome() {
        return this.iconMonochrome;
    }

    public ActiniumTheme theme() {
        return this.theme;
    }

    public List<RsoPage> pages() {
        return this.pages;
    }

    /** RSO 视图的页面：包装 embeddium OptionPage，把文本转成 1.12.2 ITextComponent。 */
    public static final class RsoPage {
        private final OptionPage delegate;

        RsoPage(OptionPage delegate) {
            this.delegate = delegate;
        }

        public ITextComponent name() {
            return convertText(this.delegate.getName());
        }

        public List<RsoOptionGroup> groups() {
            return this.delegate.getGroups().stream()
                    .map(RsoOptionGroup::new)
                    .toList();
        }

        /** 返回底层 embeddium 页面（供 tab 构建与 apply/undo 遍历）。 */
        public OptionPage unwrap() {
            return this.delegate;
        }
    }

    /** RSO 视图的选项组：包装 embeddium OptionGroup。 */
    public static final class RsoOptionGroup {
        private final org.embeddedt.embeddium.api.options.structure.OptionGroup delegate;

        RsoOptionGroup(org.embeddedt.embeddium.api.options.structure.OptionGroup delegate) {
            this.delegate = delegate;
        }

        public ITextComponent name() {
            return new TextComponentString("");
        }

        public List<org.embeddedt.embeddium.api.options.structure.Option<?>> options() {
            return this.delegate.getOptions();
        }
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
