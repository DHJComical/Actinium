package me.decce.gnetum;

public class SuggestedPass {
    public static int get(String name) {
        int pass = switch (name) {
            case Gnetum.HAND_ELEMENT, "minecraft:vignette", "minecraft:helmet", "minecraft:portal" -> 1;
            case "minecraft:hotbar", "minecraft:crosshairs", "minecraft:boss_health", "gnetum:player_left", "gnetum:player_right", "minecraft:sleep_overlay", "minecraft:jump_meter", "minecraft:experience", "minecraft:tool_highlight" -> 2;
            case "minecraft:hud_text", "minecraft:fps_graph", "minecraft:potion_icons", "minecraft:record_overlay", "minecraft:subtitles", "minecraft:title", "minecraft:scoreboard", "minecraft:chat_panel", "minecraft:player_list" -> 3;
            default -> 3;
        };
        if (pass > Gnetum.config.numberOfPasses) pass = Gnetum.config.numberOfPasses;
        return pass;
    }
}
