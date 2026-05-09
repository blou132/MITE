package net.mite.port.playerdata;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

public final class MitePlayerDataManager {
	private MitePlayerDataManager() {
	}

	public static MitePlayerDataStore store(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(MitePlayerDataStore.TYPE);
	}

	public static MitePlayerData getOrCreate(MinecraftServer server, ServerPlayer player) {
		MitePlayerDataStore store = store(server);
		UUID playerId = player.getUUID();
		return store.getOrCreate(playerId, () -> MitePlayerData.createDefault(
			isHardcoreRulesActive(server),
			player.gameMode().isSurvival()
		));
	}

	public static MitePlayerData update(MinecraftServer server, ServerPlayer player, MitePlayerData data) {
		MitePlayerDataStore store = store(server);
		UUID playerId = player.getUUID();
		store.put(playerId, data);
		return data;
	}

	public static MitePlayerData refreshFlags(MinecraftServer server, ServerPlayer player) {
		MitePlayerData current = getOrCreate(server, player);
		MitePlayerData refreshed = current.withGameplayFlags(isHardcoreRulesActive(server), player.gameMode().isSurvival());
		return update(server, player, refreshed);
	}

	public static MitePlayerData applyDeathState(MinecraftServer server, ServerPlayer player) {
		MitePlayerData current = getOrCreate(server, player);
		MitePlayerData resolved = MitePlayerLifecyclePolicy.onFatalDamage(current, isHardcoreRulesActive(server));
		return update(server, player, resolved);
	}

	public static MitePlayerData applyRespawnState(
		MinecraftServer server,
		ServerPlayer oldPlayer,
		ServerPlayer newPlayer,
		boolean fromAlivePlayer
	) {
		MitePlayerData previous = getOrCreate(server, oldPlayer);
		MitePlayerData resolved = MitePlayerLifecyclePolicy.onRespawn(
			previous,
			isHardcoreRulesActive(server),
			newPlayer.gameMode().isSurvival(),
			fromAlivePlayer
		);
		return update(server, newPlayer, resolved);
	}

	public static void saveNow(MinecraftServer server) {
		server.overworld().getDataStorage().saveAndJoin();
	}

	public static boolean isHardcoreRulesActiveForServer(MinecraftServer server) {
		return isHardcoreRulesActive(server);
	}

	private static boolean isHardcoreRulesActive(MinecraftServer server) {
		boolean naturalRegen = server.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
		boolean keepInventory = server.getGameRules().get(GameRules.KEEP_INVENTORY);
		return server.getWorldData().isHardcore() || (!naturalRegen && !keepInventory);
	}
}
