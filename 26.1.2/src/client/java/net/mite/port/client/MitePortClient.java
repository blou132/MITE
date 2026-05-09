package net.mite.port.client;

import net.fabricmc.api.ClientModInitializer;
import net.mite.port.MitePortMod;

public final class MitePortClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MitePortMod.LOGGER.info("MITE client bootstrap initialized");
	}
}
