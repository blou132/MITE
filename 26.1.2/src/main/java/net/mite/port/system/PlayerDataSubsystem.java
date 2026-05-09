package net.mite.port.system;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.mite.port.MitePortMod;
import net.mite.port.playerdata.MitePlayerData;
import net.mite.port.playerdata.MitePlayerDataManager;
import net.mite.port.playerdata.MitePlayerDataStore;
import net.minecraft.server.MinecraftServer;
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

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			MitePlayerDataStore store = MitePlayerDataManager.store(server);
			MitePortMod.LOGGER.info("MITE player_data server stopping: trackedPlayers={}", store.playerCount());
			MitePlayerDataManager.saveNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MitePortMod.LOGGER.info("MITE player_data server stopped"));

		ServerPlayerEvents.JOIN.register(player -> {
			MinecraftServer server = player.level().getServer();
			if (server != null) {
				onJoin(server, player);
			}
		});
		ServerPlayerEvents.LEAVE.register(player -> {
			MinecraftServer server = player.level().getServer();
			if (server != null) {
				onLeave(server, player);
			}
		});
		ServerPlayerEvents.ALLOW_DEATH.register(PlayerDataSubsystem::onAllowDeath);
		ServerPlayerEvents.COPY_FROM.register(PlayerDataSubsystem::onCopyFrom);
		ServerPlayerEvents.AFTER_RESPAWN.register(PlayerDataSubsystem::onAfterRespawn);
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

	private static void onLeave(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
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

	private static boolean onAllowDeath(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float damageAmount) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}

		MitePlayerData data = MitePlayerDataManager.applyDeathState(server, player);
		if (data.hardcoreRulesActive()) {
			MitePortMod.LOGGER.info(
				"MITE death hook (hardcore rules) for {} ({}), source={}, damage={}",
				player.getName().getString(),
				player.getUUID(),
				source.getMsgId(),
				damageAmount
			);
		} else {
			MitePortMod.LOGGER.debug(
				"MITE death hook for {} ({}) source={}, damage={}",
				player.getName().getString(),
				player.getUUID(),
				source.getMsgId(),
				damageAmount
			);
		}
		return true;
	}

	private static void onCopyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean fromAlivePlayer) {
		MinecraftServer server = newPlayer.level().getServer();
		if (server == null) {
			return;
		}

		MitePlayerData data = MitePlayerDataManager.applyRespawnState(server, oldPlayer, newPlayer, fromAlivePlayer);
		if (!fromAlivePlayer && !MitePlayerDataManager.isHardcoreRulesActiveForServer(server)) {
			MitePortMod.LOGGER.info(
				"MITE respawn restore for {} ({}) nutrition={}, miteExhaustion={}, ticksReset={}",
				newPlayer.getName().getString(),
				newPlayer.getUUID(),
				data.nutritionLevel(),
				data.miteExhaustion(),
				data.ticksSinceNutritionUpdate()
			);
		} else {
			MitePortMod.LOGGER.debug(
				"MITE respawn copy for {} ({}) fromAlive={}, nutrition={}, miteExhaustion={}",
				newPlayer.getName().getString(),
				newPlayer.getUUID(),
				fromAlivePlayer,
				data.nutritionLevel(),
				data.miteExhaustion()
			);
		}
	}

	private static void onAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean fromAlivePlayer) {
		MinecraftServer server = newPlayer.level().getServer();
		if (server == null) {
			return;
		}

		MitePlayerData data = MitePlayerDataManager.refreshFlags(server, newPlayer);
		MitePortMod.LOGGER.debug(
			"MITE post-respawn sync for {} ({}) fromAlive={}, hardcoreRules={}, survivalRules={}",
			newPlayer.getName().getString(),
			newPlayer.getUUID(),
			fromAlivePlayer,
			data.hardcoreRulesActive(),
			data.survivalRulesActive()
		);
	}
}
