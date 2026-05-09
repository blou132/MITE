package net.mite.port.system;

public interface MiteSubsystem {
	String id();

	String status();

	void initialize();
}
