package net.mite.port.system;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.mite.port.MitePortMod;
import net.mite.port.playerdata.MitePlayerData;
import net.mite.port.playerdata.MitePlayerDataManager;
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
		return "partial: periodic extra exhaustion persisted via player_data";
	}

	@Override
	public void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				MitePlayerData current = MitePlayerDataManager.getOrCreate(server, player);

				boolean survivalRulesActive = player.gameMode().isSurvival();
				MitePlayerData updatedFlags = current.withGameplayFlags(current.hardcoreRulesActive(), survivalRulesActive);
				if (!updatedFlags.equals(current)) {
					current = MitePlayerDataManager.update(server, player, updatedFlags);
				}

				if (player.isCreative() || player.isSpectator() || !current.survivalRulesActive()) {
					continue;
				}

				MitePlayerData progressed = current.withTickProgress();
				if (progressed.ticksSinceNutritionUpdate() >= EXHAUSTION_INTERVAL_TICKS) {
					player.getFoodData().addExhaustion(EXTRA_EXHAUSTION);
					progressed = progressed.withNutritionCheckpoint(EXTRA_EXHAUSTION, player.getFoodData().getFoodLevel());
					MitePortMod.LOGGER.debug(
						"MITE hunger checkpoint for {} ({}) nutrition={}, miteExhaustion={}",
						player.getName().getString(),
						player.getUUID(),
						progressed.nutritionLevel(),
						progressed.miteExhaustion()
					);
				}

				MitePlayerDataManager.update(server, player, progressed);
			}
		});
	}
}
