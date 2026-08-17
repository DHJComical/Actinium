package com.dhj.actinium.compat.sodium;

import com.dhj.actinium.gui.ActiniumGameOptionPages;
import com.dhj.actinium.runtime.ActiniumRuntime;
import net.irisshaders.iris.compat.sodium.IrisConfigEntryPoint;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import org.embeddedt.embeddium.api.options.structure.OptionPage;

import java.util.ArrayList;
import java.util.List;

/**
 * 自研内置选项页集合：直接返回 embeddium {@link OptionPage} 形式的
 * Actinium 视频选项页与 Iris 兼容页，不再经过 Sodium 配置模型转换。
 */
public final class ActiniumOptionPages {
    private ActiniumOptionPages() {
    }

    /** 构建 Actinium 当前暴露的全部内置页面（embeddium 模型）。 */
    public static List<OptionPage> builtInPages() {
        List<OptionPage> pages = new ArrayList<>(List.of(
                ActiniumGameOptionPages.general(),
                ActiniumGameOptionPages.quality(),
                CommonOptionPages.performance(ActiniumRuntime.options()),
                ActiniumGameOptionPages.advanced(),
                ActiniumGameOptionPages.debug()));
        pages.addAll(new IrisConfigEntryPoint().createPages());
        return List.copyOf(pages);
    }
}
