package com.dhj.actinium.compat.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.RsoModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自研选项宿主：收集全部 embeddium {@link OptionPage}（内置 Actinium 页面、
 * RSO 自身配置页、Iris 页与第三方事件贡献），按 modId 分组为
 * {@link RsoModOptions} 视图，并协调 apply/undo 与 flag 副作用。
 * 不依赖任何 Sodium 配置模型。
 */
public final class ActiniumOptionHost {
    private static final Logger LOGGER = LogManager.getLogger("Actinium-OptionHost");
    private static final class SharedHolder {
        private static final ActiniumOptionHost INSTANCE = new ActiniumOptionHost(new ActiniumApplyActionsImpl(Minecraft.getMinecraft()));
    }

    private final ActiniumApplyActions applyActions;
    private List<RsoModOptions> modOptions;

    private ActiniumOptionHost(ActiniumApplyActions applyActions) {
        this.applyActions = applyActions;
    }

    /** 返回进程级宿主（页面在首次访问时收集并冻结）。 */
    public static ActiniumOptionHost shared() {
        return SharedHolder.INSTANCE;
    }

    /** 供测试注入动作边界的构造入口。 */
    public static ActiniumOptionHost create(ActiniumApplyActions applyActions) {
        return new ActiniumOptionHost(applyActions);
    }

    /** 收集并分组全部选项页（惰性、一次性）。 */
    public synchronized List<RsoModOptions> modOptions() {
        if (this.modOptions == null) {
            this.modOptions = this.collect();
        }
        return this.modOptions;
    }

    private List<RsoModOptions> collect() {
        List<OptionPage> pages = new ArrayList<>();
        pages.addAll(ActiniumOptionPages.builtInPages());
        pages.add(me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfigEntryPoint.createOptionsPage());
        OptionGUIConstructionBridge.collectExtensions(pages);
        Map<String, List<OptionPage>> byMod = new LinkedHashMap<>();
        for (OptionPage page : pages) {
            String modId = page.getId().getModId();
            if (modId == null || modId.isBlank()) {
                throw new IllegalArgumentException("Option page has a blank namespace: " + page.getId());
            }
            byMod.computeIfAbsent(modId, ignored -> new ArrayList<>()).add(page);
        }
        List<RsoModOptions> result = new ArrayList<>();
        for (Map.Entry<String, List<OptionPage>> entry : byMod.entrySet()) {
            result.add(RsoModOptions.aggregate(entry.getKey(), entry.getValue()));
        }
        LOGGER.info("Collected {} option page groups: {}", result.size(),
                result.stream().map(RsoModOptions::configId).toList());
        return List.copyOf(result);
    }

    /** 应用所有挂起改动，并触发去重后的 flag 副作用。 */
    public void applyChanges() {
        Set<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
        Set<OptionStorage<?>> dirtyStorages = new java.util.HashSet<>();
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    option.applyChanges();
                    flags.addAll(option.getFlags());
                    dirtyStorages.add(option.getStorage());
                }
            }
        }
        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save(flags);
        }
        this.applyFlagSideEffects(flags);
    }

    /** 撤销所有挂起改动（回到已应用基线）。 */
    public void undoChanges() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    option.reset();
                }
            }
        }
    }

    /** 返回是否存在挂起改动。 */
    public boolean hasPendingChanges() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 全部恢复为声明默认值（挂起状态）。 */
    public void resetToDefaults() {
        for (OptionPage page : this.allPages()) {
            for (Option<?> option : page.getOptions()) {
                option.resetToDefault();
            }
        }
    }

    private List<OptionPage> allPages() {
        List<OptionPage> pages = new ArrayList<>();
        for (RsoModOptions options : this.modOptions()) {
            options.unwrapPages(pages);
        }
        return pages;
    }

    private void applyFlagSideEffects(Set<OptionFlag> flags) {
        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            this.applyActions.reloadRenderer();
        } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
            this.applyActions.updateRenderer();
        }
        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            this.applyActions.reloadAssets();
        }
        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
            this.applyActions.showRestartRequired();
        }
    }
}
