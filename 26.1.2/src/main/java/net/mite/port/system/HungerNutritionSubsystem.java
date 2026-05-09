package net.mite.port.system;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public final class HungerNutritionSubsystem implements MiteSubsystem {
	private static final int EXHAUSTION_INTERVAL_TICKS = 40;
	private static final float EXTRA_EXHAUSTION = 0.35F;

	@Override
	public String id() {
		return "hunger_nutrition";
	}

	@Override
	public String status() {
		return "partial: periodic extra exhaustion for non-creative players";
	}

	@Override
	public void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EXHAUSTION_INTERVAL_TICKS != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isCreative() || player.isSpectator()) {
					continue;
				}

				player.getFoodData().addExhaustion(EXTRA_EXHAUSTION);
			}
		});
	}
}
