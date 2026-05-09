package net.mite.port;

import net.fabricmc.api.ModInitializer;
import net.mite.port.system.MiteSubsystemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MitePortMod implements ModInitializer {
	public static final String MOD_ID = "mite";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Bootstrapping MITE legacy port for Minecraft 26.1.2");
		MiteSubsystemRegistry.bootstrap().initializeAll();
	}
}
