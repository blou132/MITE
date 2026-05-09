package net.mite.port.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.mite.port.MitePortMod;

public final class MiteSubsystemRegistry {
	private final List<MiteSubsystem> subsystems = new ArrayList<>();

	public static MiteSubsystemRegistry bootstrap() {
		MiteSubsystemRegistry registry = new MiteSubsystemRegistry();
		registry.register(new HardcoreDifficultySubsystem());
		registry.register(new HungerNutritionSubsystem());
		registry.register(new LoggedStubSubsystem("health_damage", "stub: pending gameplay parity"));
		registry.register(new LoggedStubSubsystem("progression_crafts", "stub: pending gameplay parity"));
		registry.register(new LoggedStubSubsystem("mobs_ai", "stub: pending gameplay parity"));
		registry.register(new LoggedStubSubsystem("blocks_items", "stub: pending gameplay parity"));
		registry.register(new LoggedStubSubsystem("worldgen", "stub: pending gameplay parity"));
		registry.register(new PlayerDataSubsystem());
		registry.register(new LoggedStubSubsystem("generation_balance", "stub: pending balance migration"));

		return registry;
	}

	private void register(MiteSubsystem subsystem) {
		subsystems.add(subsystem);
	}

	public void initializeAll() {
		for (MiteSubsystem subsystem : subsystems) {
			subsystem.initialize();
			MitePortMod.LOGGER.info("Subsystem {} => {}", subsystem.id(), subsystem.status());
		}
	}

	public List<MiteSubsystem> subsystems() {
		return Collections.unmodifiableList(subsystems);
	}
}
