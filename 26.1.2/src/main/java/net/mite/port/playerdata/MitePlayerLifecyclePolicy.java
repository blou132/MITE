package net.mite.port.playerdata;

public final class MitePlayerLifecyclePolicy {
	private MitePlayerLifecyclePolicy() {
	}

	public static MitePlayerData onFatalDamage(MitePlayerData current, boolean hardcoreRulesActive) {
		return current.withGameplayFlags(hardcoreRulesActive, current.survivalRulesActive());
	}

	public static MitePlayerData onRespawn(
		MitePlayerData previous,
		boolean hardcoreRulesActive,
		boolean survivalRulesActive,
		boolean fromAlivePlayer
	) {
		MitePlayerData base = previous.withGameplayFlags(hardcoreRulesActive, survivalRulesActive);

		if (hardcoreRulesActive || fromAlivePlayer) {
			return base;
		}

		// Non-hardcore/dev respawn keeps state but clears transient tick accumulator.
		return base.withTicksSinceNutritionUpdate(0);
	}
}
