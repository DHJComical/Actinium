package com.gtnewhorizons.angelica.glsm.hooks.events;

import net.minecraftforge.eventbus.api.event.MutableEvent;

public final class TextureBindEvent extends MutableEvent {
    public int unit;
    public int target;
    public int textureId;
}
