package net.mite.port.system;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mite.port.MitePortMod;
import net.mite.port.playerdata.MitePlayerData;
import net.mite.port.playerdata.MitePlayerDataManager;
import net.mite.port.playerdata.MitePlayerDataStore;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerDataSubsystem implements MiteSubsystem {
	@Override
	public String id() {
		return "player_data";
	}

	@Override
	public String status() {
		return "partial: per-player MITE data persisted in world saved data";
	}

	@Override
	public void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MitePlayerDataStore store = MitePlayerDataManager.store(server);
			MitePortMod.LOGGER.info("MITE player_data loaded: trackedPlayers={}", store.playerCount());
		});

		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
			MitePlayerDataStore store = MitePlayerDataManager.store(server);
			MitePortMod.LOGGER.debug(
				"MITE player_data save checkpoint: trackedPlayers={}, flush={}, force={}",
				store.playerCount(),
				flush,
				force
			);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(server, handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(server, handler.player));
	}

	private static void onJoin(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
		MitePlayerDataStore store = MitePlayerDataManager.store(server);
		boolean created = store.get(player.getUUID()) == null;
		MitePlayerData data = MitePlayerDataManager.refreshFlags(server, player);
		if (created) {
			MitePortMod.LOGGER.info(
				"MITE player_data created for {} ({}) version={}",
				player.getName().getString(),
				player.getUUID(),
				data.dataVersion()
			);
		} else {
			MitePortMod.LOGGER.debug(
				"MITE player_data loaded for {} ({}) nutrition={}, miteExhaustion={}, ticksSinceUpdate={}",
				player.getName().getString(),
				player.getUUID(),
				data.nutritionLevel(),
				data.miteExhaustion(),
				data.ticksSinceNutritionUpdate()
			);
		}
	}

	private static void onDisconnect(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
		MitePlayerData data = MitePlayerDataManager.refreshFlags(server, player);
		MitePortMod.LOGGER.debug(
			"MITE player_data detached for {} ({}) nutrition={}, miteExhaustion={}, ticksSinceUpdate={}",
			player.getName().getString(),
			player.getUUID(),
			data.nutritionLevel(),
			data.miteExhaustion(),
			data.ticksSinceNutritionUpdate()
		);
	}
}
