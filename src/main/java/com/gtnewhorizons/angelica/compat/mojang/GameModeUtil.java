package com.gtnewhorizons.angelica.compat.mojang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.GameType;

public final class GameModeUtil {
    private GameModeUtil() {
    }

    public static boolean isSpectator() {
        final PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
        if (controller == null) {
            return false;
        }
        return controller.getCurrentGameType() == GameType.SPECTATOR;
    }
}
