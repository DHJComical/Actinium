package net.coderbot.iris.apiimpl;

import net.coderbot.iris.Iris;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.config.IrisConfig;
import net.irisshaders.iris.api.v0.IrisApiConfig;
import net.minecraft.client.Minecraft;

import java.io.IOException;

public class IrisApiV0ConfigImpl implements IrisApiConfig {
	@Override
	public boolean areShadersEnabled() {
		return Iris.getIrisConfig().areShadersEnabled();
	}

	@Override
	public void setShadersEnabledAndApply(boolean enabled) {
		IrisConfig config = Iris.getIrisConfig();

		config.setShadersEnabled(enabled);

		try {
			config.save();
		} catch (IOException e) {
			Iris.logger.error("Error saving configuration file!", e);
		}

		try {
			Iris.reload();
		} catch (IOException e) {
			Iris.logger.error("Error reloading shader pack while applying changes!", e);
		}

		BlockRenderingSettings.INSTANCE.reloadRendererIfRequired();
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft != null && minecraft.renderGlobal != null) {
			minecraft.renderGlobal.loadRenderers();
		}
	}
}
