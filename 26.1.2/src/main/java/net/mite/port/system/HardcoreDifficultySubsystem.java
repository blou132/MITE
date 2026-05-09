package net.mite.port.system;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.mite.port.MitePortMod;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;

public final class HardcoreDifficultySubsystem implements MiteSubsystem {
	@Override
	public String id() {
		return "hardcore_difficulty";
	}

	@Override
	public String status() {
		return "partial: hard difficulty lock + no natural regen + keepInventory=false";
	}

	@Override
	public void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.setDifficulty(Difficulty.HARD, true);
			server.setDifficultyLocked(true);
			server.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, Boolean.FALSE, server);
			server.getGameRules().set(GameRules.KEEP_INVENTORY, Boolean.FALSE, server);

			boolean naturalRegen = server.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
			boolean keepInventory = server.getGameRules().get(GameRules.KEEP_INVENTORY);

			if (!server.getWorldData().isHardcore()) {
				MitePortMod.LOGGER.warn(
					"World is not flagged hardcore. Enforced hardcore-like rules are active, but true hardcore permadeath is not forced."
				);
			}

			MitePortMod.LOGGER.info(
				"Hardcore defaults applied: difficulty={}, locked={}, naturalRegen={}, keepInventory={}",
				server.getWorldData().getDifficulty(),
				server.getWorldData().isDifficultyLocked(),
				naturalRegen,
				keepInventory
			);
			MitePortMod.LOGGER.info("MITE hardcore defaults enforced on server start");
		});
	}
}
