package me.decce.gnetum.gui.widgets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TooltipHelper {
    public static List<String> createTooltip(String tooltip) {
        if (tooltip == null) {
            return Collections.emptyList();
        }
        var split = tooltip.split(Pattern.quote("\\n"));
        return Arrays.asList(split);
    }
}
