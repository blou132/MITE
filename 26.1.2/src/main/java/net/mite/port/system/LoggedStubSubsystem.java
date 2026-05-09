package net.mite.port.system;

import net.mite.port.MitePortMod;

public final class LoggedStubSubsystem implements MiteSubsystem {
	private final String id;
	private final String status;

	public LoggedStubSubsystem(String id, String status) {
		this.id = id;
		this.status = status;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String status() {
		return status;
	}

	@Override
	public void initialize() {
		MitePortMod.LOGGER.debug("Initializing subsystem {}", id);
	}
}
