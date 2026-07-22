package org.taumc.celeritas.compat;

import com.dhj.actinium.compat.sodium.LegacyOptionPageProvider;
import com.dhj.actinium.compat.sodium.OptionGUIConstructionBridge;
import org.embeddedt.embeddium.api.OptionGroupConstructionEvent;
import org.embeddedt.embeddium.api.OptionPageConstructionEvent;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.taumc.celeritas.CeleritasVintage;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates current option events with the isolated legacy event dispatcher.
 */
public final class CeleritasLegacyEventBridge implements LegacyOptionPageProvider {
    static final CeleritasLegacyEventBridge INSTANCE = new CeleritasLegacyEventBridge();
    private static final InstallOnce PROVIDER_INSTALLATION = new InstallOnce();
    private static final InstallOnce GROUP_EVENT_INSTALLATION = new InstallOnce();
    private static final InstallOnce PAGE_EVENT_INSTALLATION = new InstallOnce();

    private CeleritasLegacyEventBridge() {
    }

    public static void install() {
        PROVIDER_INSTALLATION.run(() -> OptionGUIConstructionBridge.registerLegacyProvider(INSTANCE));
        GROUP_EVENT_INSTALLATION.run(() -> OptionGroupConstructionEvent.BUS.addListener(INSTANCE::bridgeGroupEvent));
        PAGE_EVENT_INSTALLATION.run(() -> OptionPageConstructionEvent.BUS.addListener(INSTANCE::bridgePageEvent));
    }

    private void bridgeGroupEvent(OptionGroupConstructionEvent event) {
        if (BridgeDispatchGuard.INSTANCE.isSuppressed()) {
            return;
        }
        List<OptionModel<?>> models = new ArrayList<>(event.getOptions().size());
        Map<OptionModel<?>, Option<?>> originals = new IdentityHashMap<>();
        for (Option<?> option : event.getOptions()) {
            OptionModel<?> model = CurrentOptionMapper.describeOption(option);
            models.add(model);
            originals.put(model, option);
        }
        List<OptionModel<?>> dispatched = LegacyEventDispatcher.dispatchGroup(
                CurrentOptionMapper.describeIdentifier(event.getId()), models);
        List<Option<?>> replaced = new ArrayList<>(dispatched.size());
        for (OptionModel<?> model : dispatched) {
            Option<?> original = originals.get(model);
            replaced.add(original != null ? original : CurrentOptionMapper.createOption(model));
        }
        event.getOptions().clear();
        event.getOptions().addAll(replaced);
    }

    private void bridgePageEvent(OptionPageConstructionEvent event) {
        if (BridgeDispatchGuard.INSTANCE.isSuppressed()) {
            return;
        }
        List<OptionGroupModel> groups = LegacyEventDispatcher.dispatchPage(
                CurrentOptionMapper.describeIdentifier(event.getId()), event.getTranslationKey());
        for (OptionGroupModel group : groups) {
            event.addGroup(CurrentOptionMapper.createGroup(group));
        }
    }

    @Override
    public void appendPages(List<OptionPage> pages) {
        List<OptionPageModel> models = new ArrayList<>(pages.size());
        Map<OptionPageModel, OptionPage> originals = new IdentityHashMap<>();
        for (OptionPage page : pages) {
            OptionPageModel model = CurrentOptionMapper.describePage(page);
            models.add(model);
            originals.put(model, page);
        }
        List<OptionPageModel> dispatched = LegacyEventDispatcher.dispatchGui(models);
        pages.clear();
        int addedPages = 0;
        for (OptionPageModel model : dispatched) {
            OptionPage original = originals.get(model);
            if (original != null) {
                pages.add(original);
            } else {
                pages.add(CurrentOptionMapper.createPage(model));
                addedPages++;
            }
        }
        CeleritasVintage.logger().info("Collected {} legacy Celeritas option pages: {}", addedPages,
                dispatched.stream().map(page -> page.id().namespace() + ":" + page.id().path()).toList());
    }
}
