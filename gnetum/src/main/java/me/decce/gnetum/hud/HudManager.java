package me.decce.gnetum.hud;

import java.util.ArrayList;
import java.util.List;

public class HudManager {
    public static List<Hud> huds = new ArrayList<>();

    public static void register(Hud hud) {
        huds.add(hud);
    }
}
