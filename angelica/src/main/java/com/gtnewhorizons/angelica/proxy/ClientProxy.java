package com.gtnewhorizons.angelica.proxy;

import com.dhj.actinium.config.AnimationMode;
import com.dhj.actinium.config.ManagedEnum;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import com.dhj.actinium.runtime.ActiniumRuntime;

public final class ClientProxy {
    public static final ManagedEnum<AnimationMode> animationsMode = new ManagedEnum<>(AnimationMode.VISIBLE_ONLY);

    private ClientProxy() {
    }

    public static SodiumGameOptions options() {
        return ActiniumRuntime.options();
    }
}
