package net.coderbot.iris.compat.rfp2;

import com.gtnewhorizon.gtnhlib.compat.Mods;
import net.minecraft.entity.Entity;

/**
 * Compatibility checks for Real First Person 2 that do not load its classes directly.
 */
public final class Rfp2Compat {
    private static final String PLAYER_DUMMY_CLASS = "com.rejahtavi.rfp2.EntityPlayerDummy";

    private Rfp2Compat() {
    }

    /** Returns whether the entity is RFP2's dummy used to render the local player. */
    public static boolean isPlayerDummy(Entity entity) {
        return Mods.RFP2 && entity.getClass().getName().equals(PLAYER_DUMMY_CLASS);
    }
}
