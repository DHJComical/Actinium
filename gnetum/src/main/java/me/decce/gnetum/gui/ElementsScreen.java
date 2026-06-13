package me.decce.gnetum.gui;

import com.google.common.collect.Maps;
import me.decce.gnetum.CacheSetting;
import me.decce.gnetum.Gnetum;
import me.decce.gnetum.ModListHelper;
import me.decce.gnetum.gui.widgets.GuiButtonEx;
import me.decce.gnetum.gui.widgets.IntSlider;
import me.decce.gnetum.gui.widgets.MultilineStringWidget;
import me.decce.gnetum.gui.widgets.StringWidget;
import me.decce.gnetum.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.util.Locale;
import java.util.Map;

public class ElementsScreen extends BaseScreen {
    public static final int BTN_PREV_PAGE = 1;
    public static final int BTN_NEXT_PAGE = 2;
    public static final int TXT_PAGE = 3;
    public static final int TXT_NOTICE = 4;

    private Map<String, CacheSetting> map;
    private boolean vanilla;
    private int page;
    private int pageCount;

    private GuiButtonEx btnPrevPage;
    private GuiButtonEx btnNextPage;

    public ElementsScreen(Map<String, CacheSetting> map, boolean vanilla) {
        super();
        this.map = Maps.filterValues(map, c -> !c.hidden);
        this.vanilla = vanilla;
        this.pageCount = this.map.size() % 10 == 0 ? this.map.size() / 10 : this.map.size() / 10 + 1;
    }

    @Override
    public void initGui() {
        this.setPage(0);
    }

    @Override
    protected void rebuild() {
        super.rebuild();

        int max = Math.min(map.size(), page * 10 + 10);

        int wb = 130;
        int ws = 50;
        int extrawb = width - wb * 2 - ws * 2 - 60;
        extrawb = Math.max(0, Math.min(60, extrawb));
        wb += extrawb;
        int h = 20;
        int margin = 5;
        int xlb = width / 2 - 2 - ws - wb;
        int xls = width / 2 - 2 - ws;
        int xrb = width / 2 + 2;
        int xrs = width / 2 + 2 + wb;
        int y = height / 2 - h / 2 - margin - h - margin - h - 12;
        int i = 0;

        String noticeString = vanilla ? "" : I18n.format("gnetum.config.element.notice");
        var notice = new MultilineStringWidget(TXT_NOTICE, xlb, y - 28, 2 * (wb + ws) + margin, noticeString);

        boolean left = true;
        int id = 11;
        for (var entry : map.entrySet()) {
            if (i >= 10 * page && i < max) {
                var btn = new ToggleButton(id++, left ? xlb : xrb, y, wb, h, (map.get(entry.getKey())).enabled, () -> beautifyString(entry.getKey()) + ": %s");
                btn.setTooltip(() -> {
                    String line1 = btn.displayString + "\n(" + entry.getKey() + ")";
                    String line2 = I18n.format("gnetum.config.tooltip." + entry.getValue().enabled.value.name().toLowerCase(Locale.ROOT), entry.getValue().enabled.effectiveText());
                    return line1 + "\n\n" + line2;
                });
                this.addButton(btn);
                var slider = new IntSlider(id++, left ? xls : xrs, y, ws, h, () -> I18n.format("gnetum.config.pass") + ": %s", 1, Gnetum.config.numberOfPasses, (entry.getValue()).pass, true, pass -> ((CacheSetting)entry.getValue()).pass = pass);
                this.addButton(slider);
                if (!left) {
                    y += h + margin;
                }
                left = !left;
            }
            i++;
        }

        y = height / 2 + h / + margin + h + margin + h + margin;
        int lineHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        int currentPage = map.isEmpty() ? 0 : this.page + 1;
        String pageString = currentPage + " / " + pageCount;
        var page = new StringWidget(TXT_PAGE, width / 2, y + lineHeight / 2, pageString);
        btnPrevPage = new GuiButtonEx(BTN_PREV_PAGE, xlb, y, 20, 20, "<", () -> setPage(this.page - 1));
        btnNextPage = new GuiButtonEx(BTN_NEXT_PAGE, xrs + ws - 20, y, 20, 20, ">", () -> setPage(this.page + 1));
        btnPrevPage.enabled = this.page > 0;
        btnNextPage.enabled = this.page < pageCount - 1;
        this.addButton(notice);
        this.addButton(page);
        this.addButton(btnPrevPage);
        this.addButton(btnNextPage);
    }

    private void setPage(int newPage) {
        this.page = newPage;
        this.rebuild();
    }

    @Override
    public void close() {
        super.close();
    }

    private String beautifyString(String string) {
        if (this.vanilla) {
            String key1 = string;
            if (!string.startsWith("gnetum.packedElement")) {
                key1 = "gnetum.config.element." + string.replace(':', '.');
            }
            if (I18n.hasKey(key1)) {
                return I18n.format(key1);
            }
            String left = string.substring(0, string.indexOf(':'));
            String right = string.substring(string.indexOf(':') + 1);

            var name = ModListHelper.getModName(left);
            if (name != null) {
                left = "[" + name + "]";
            }
            else left = "[" + left + "]";

            right = Character.toUpperCase(right.charAt(0)) + right.substring(1);
            int i;
            while ((i = right.indexOf('_')) != -1) {
                right = right.substring(0, i) + " " + Character.toUpperCase(right.charAt(i + 1)) + right.substring(i + 2);
            }
            return left + " " + right;
        }
        else {
            if (Gnetum.OTHER_MODS.equals(string)) return I18n.format("gnetum.config.element.gnetum_unknown");
            var name = ModListHelper.getModName(string);
            return name == null ? string : name;
        }
    }
}
