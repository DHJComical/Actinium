package com.gtnewhorizons.angelica.mixins.interfaces;

import net.minecraft.client.gui.ScaledResolution;

public interface GuiIngameAccessor {
	void callRenderVignette(float lightLevel, ScaledResolution scaledRes);

	void callRenderPumpkinOverlay(ScaledResolution scaledRes);

	void callRenderPortal(float timeInPortal, ScaledResolution scaledRes);
}
