package net.mite.port.playerdata;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import net.mite.port.MitePortMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;

public final class MitePlayerDataDebugScenario {
	private static final int KILL_DELAY_TICKS = 20;
	private static final int FORCE_RESPAWN_AFTER_TICKS = 20;
	private static final int WAIT_TIMEOUT_TICKS = 20 * 20;
	public static final String AUTO_TEST_WORLD_NAME = "MITE_DEATH_RESPAWN_TEST";
	private static final String AUTO_TEST_FLAG_FILE = ".mite_playerdata_autotest.flag";

	private static final float DEV_NUTRITION = 13.0F;
	private static final float DEV_EXHAUSTION = 1.8F;
	private static final int DEV_TICKS = 37;

	private static final float HARDCORE_NUTRITION = 8.0F;
	private static final float HARDCORE_EXHAUSTION = 3.2F;
	private static final int HARDCORE_TICKS = 19;

	private static final float EPSILON = 0.0001F;

	private static final Map<UUID, ScenarioState> STATES = new HashMap<>();

	private MitePlayerDataDebugScenario() {
	}

	public static void start(MinecraftServer server, ServerPlayer player) {
		UUID playerId = player.getUUID();
		STATES.put(playerId, ScenarioState.scheduleDevKill(KILL_DELAY_TICKS));
		MitePortMod.LOGGER.info(
			"MITE debug scenario started for {} ({}) in world {}",
			player.getName().getString(),
			playerId,
			server.getWorldData().getLevelName()
		);
	}

	public static void stop(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	public static String status(ServerPlayer player) {
		ScenarioState state = STATES.get(player.getUUID());
		if (state == null) {
			return "idle";
		}

		return state.describe();
	}

	public static void maybeStartAutoScenario(MinecraftServer server, ServerPlayer player) {
		String levelName = server.getWorldData().getLevelName();
		Path worldRoot = server.getWorldPath(LevelResource.ROOT);
		String worldFolderName = worldRoot.getFileName().toString();
		boolean flagEnabled = Files.exists(worldRoot.resolve(AUTO_TEST_FLAG_FILE));
		if (!AUTO_TEST_WORLD_NAME.equals(levelName) && !AUTO_TEST_WORLD_NAME.equals(worldFolderName) && !flagEnabled) {
			return;
		}

		if (STATES.containsKey(player.getUUID())) {
			return;
		}

		MitePortMod.LOGGER.info(
			"MITE debug scenario auto-triggered for {} ({}) levelName={} worldFolder={} flagEnabled={}",
			player.getName().getString(),
			player.getUUID(),
			levelName,
			worldFolderName,
			flagEnabled
		);
		start(server, player);
	}

	public static void tick(MinecraftServer server) {
		if (STATES.isEmpty()) {
			return;
		}

		Map<UUID, ScenarioState> snapshot = new HashMap<>(STATES);
		for (Map.Entry<UUID, ScenarioState> entry : snapshot.entrySet()) {
			UUID playerId = entry.getKey();
			ScenarioState state = entry.getValue();
			ScenarioState latest = STATES.get(playerId);
			if (latest != state) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null) {
				continue;
			}

			if (state.phase == Phase.SCHEDULE_DEV_KILL || state.phase == Phase.SCHEDULE_HARDCORE_KILL) {
				int nextTicks = state.ticksUntilAction - 1;
				if (nextTicks > 0) {
					STATES.put(playerId, state.withTicks(nextTicks));
					continue;
				}

				if (state.phase == Phase.SCHEDULE_DEV_KILL) {
					triggerKill(server, player, false, DEV_NUTRITION, DEV_EXHAUSTION, DEV_TICKS);
				} else {
					triggerKill(server, player, true, HARDCORE_NUTRITION, HARDCORE_EXHAUSTION, HARDCORE_TICKS);
				}
			}

			if (state.phase == Phase.WAIT_DEV_RESPAWN || state.phase == Phase.WAIT_HARDCORE_RESPAWN) {
				handleWaitPhase(server, player, playerId, state);
			}
		}
	}

	public static void onAfterRespawn(MinecraftServer server, ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean fromAlivePlayer) {
		ScenarioState state = STATES.get(newPlayer.getUUID());
		if (state == null || fromAlivePlayer) {
			return;
		}

		validateWaitState(server, newPlayer, state, "event");
	}

	public static void triggerSingleKillTest(MinecraftServer server, ServerPlayer player, boolean hardcoreMode) {
		float nutrition = hardcoreMode ? HARDCORE_NUTRITION : DEV_NUTRITION;
		float exhaustion = hardcoreMode ? HARDCORE_EXHAUSTION : DEV_EXHAUSTION;
		int ticks = hardcoreMode ? HARDCORE_TICKS : DEV_TICKS;
		triggerKill(server, player, hardcoreMode, nutrition, exhaustion, ticks);
	}

	private static void triggerKill(
		MinecraftServer server,
		ServerPlayer player,
		boolean hardcoreMode,
		float nutrition,
		float exhaustion,
		int ticks
	) {
		if (hardcoreMode) {
			server.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, Boolean.FALSE, server);
			server.getGameRules().set(GameRules.KEEP_INVENTORY, Boolean.FALSE, server);
		} else {
			server.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, Boolean.TRUE, server);
			server.getGameRules().set(GameRules.KEEP_INVENTORY, Boolean.TRUE, server);
		}
		server.getGameRules().set(GameRules.IMMEDIATE_RESPAWN, Boolean.TRUE, server);

		MitePlayerData prepared = new MitePlayerData(
			MitePlayerData.CURRENT_DATA_VERSION,
			nutrition,
			exhaustion,
			ticks,
			MitePlayerDataManager.isHardcoreRulesActiveForServer(server),
			true
		);
		MitePlayerData stored = MitePlayerDataManager.update(server, player, prepared);
		player.getFoodData().setFoodLevel(Math.round(stored.nutritionLevel()));
		player.getFoodData().setSaturation(0.0F);

		ScenarioState next = hardcoreMode
			? ScenarioState.waitHardcoreRespawn(stored.nutritionLevel(), stored.miteExhaustion(), stored.ticksSinceNutritionUpdate())
			: ScenarioState.waitDevRespawn(stored.nutritionLevel(), stored.miteExhaustion());
		STATES.put(player.getUUID(), next);

		MitePortMod.LOGGER.info(
			"MITE debug killtest {} for {} ({}) -> nutrition={}, exhaustion={}, ticks={}, immediateRespawn=true",
			hardcoreMode ? "HARDCORE" : "DEV",
			player.getName().getString(),
			player.getUUID(),
			format(stored.nutritionLevel()),
			format(stored.miteExhaustion()),
			stored.ticksSinceNutritionUpdate()
		);

		player.die(player.level().damageSources().genericKill());
	}

	private static void handleWaitPhase(MinecraftServer server, ServerPlayer player, UUID playerId, ScenarioState state) {
		int waitTicks = state.waitTicks + 1;
		ScenarioState waiting = state.withWaitTicks(waitTicks);
		STATES.put(playerId, waiting);

		if (waitTicks == FORCE_RESPAWN_AFTER_TICKS) {
			ScenarioState forcedRespawnState = waiting.markDeathObserved();
			STATES.put(playerId, forcedRespawnState);
			ServerPlayer respawned = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
			MitePortMod.LOGGER.info(
				"MITE debug scenario forced respawn for {} ({}) after {} ticks waiting",
				respawned.getName().getString(),
				respawned.getUUID(),
				waitTicks
			);
			return;
		}

		if (!player.isAlive()) {
			ScenarioState deadObserved = waiting.markDeathObserved();
			STATES.put(playerId, deadObserved);

			if (waitTicks >= WAIT_TIMEOUT_TICKS) {
				STATES.put(playerId, ScenarioState.failed("respawn timeout after " + waitTicks + " ticks"));
				MitePortMod.LOGGER.error(
					"MITE debug scenario FAIL timeout for {} ({}) phase={} waitedTicks={}",
					player.getName().getString(),
					playerId,
					state.phase,
					waitTicks
				);
			}
			return;
		}

		if (!waiting.deathObserved) {
			return;
		}

		validateWaitState(server, player, waiting, "tick");
	}

	private static void validateWaitState(MinecraftServer server, ServerPlayer player, ScenarioState state, String source) {
		MitePlayerData data = MitePlayerDataManager.getOrCreate(server, player);
		if (state.phase == Phase.WAIT_DEV_RESPAWN) {
			boolean ok = approximatelyEqual(data.nutritionLevel(), state.expectedNutrition)
				&& data.miteExhaustion() + EPSILON >= state.expectedExhaustion
				&& data.ticksSinceNutritionUpdate() == 0
				&& !data.hardcoreRulesActive();

			if (ok) {
				STATES.put(player.getUUID(), ScenarioState.scheduleHardcoreKill(KILL_DELAY_TICKS));
				MitePortMod.LOGGER.info(
					"MITE debug scenario DEV respawn PASS ({}) for {} ({}) -> nutrition={}, exhaustion={}, ticks={}",
					source,
					player.getName().getString(),
					player.getUUID(),
					format(data.nutritionLevel()),
					format(data.miteExhaustion()),
					data.ticksSinceNutritionUpdate()
				);
			} else {
				STATES.put(player.getUUID(), ScenarioState.failed("dev respawn validation failed"));
				MitePortMod.LOGGER.error(
					"MITE debug scenario DEV respawn FAIL ({}) for {} ({}) -> nutrition={}, exhaustion={}, ticks={}, hardcore={}",
					source,
					player.getName().getString(),
					player.getUUID(),
					format(data.nutritionLevel()),
					format(data.miteExhaustion()),
					data.ticksSinceNutritionUpdate(),
					data.hardcoreRulesActive()
				);
			}
			return;
		}

		if (state.phase == Phase.WAIT_HARDCORE_RESPAWN) {
			boolean checkpointWrapped = data.ticksSinceNutritionUpdate() == 0
				&& data.miteExhaustion() > state.expectedExhaustion + EPSILON;
			boolean ok = approximatelyEqual(data.nutritionLevel(), state.expectedNutrition)
				&& data.miteExhaustion() + EPSILON >= state.expectedExhaustion
				&& (data.ticksSinceNutritionUpdate() >= state.expectedTicks || checkpointWrapped)
				&& data.hardcoreRulesActive();

			if (ok) {
				STATES.put(player.getUUID(), ScenarioState.completed());
				MitePlayerDataManager.saveNow(server);
				MitePortMod.LOGGER.info(
					"MITE debug scenario HARDCORE respawn PASS ({}) for {} ({}) -> nutrition={}, exhaustion={}, ticks={}",
					source,
					player.getName().getString(),
					player.getUUID(),
					format(data.nutritionLevel()),
					format(data.miteExhaustion()),
					data.ticksSinceNutritionUpdate()
				);
				MitePortMod.LOGGER.info(
					"MITE debug scenario save flush complete for {} ({})",
					player.getName().getString(),
					player.getUUID()
				);
			} else {
				STATES.put(player.getUUID(), ScenarioState.failed("hardcore respawn validation failed"));
				MitePortMod.LOGGER.error(
					"MITE debug scenario HARDCORE respawn FAIL ({}) for {} ({}) -> nutrition={}, exhaustion={}, ticks={}, hardcore={}",
					source,
					player.getName().getString(),
					player.getUUID(),
					format(data.nutritionLevel()),
					format(data.miteExhaustion()),
					data.ticksSinceNutritionUpdate(),
					data.hardcoreRulesActive()
				);
			}
		}
	}

	private static boolean approximatelyEqual(float a, float b) {
		return Math.abs(a - b) <= EPSILON;
	}

	private static String format(float value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}

	private enum Phase {
		SCHEDULE_DEV_KILL,
		WAIT_DEV_RESPAWN,
		SCHEDULE_HARDCORE_KILL,
		WAIT_HARDCORE_RESPAWN,
		COMPLETED,
		FAILED
	}

	private static final class ScenarioState {
		private final Phase phase;
		private final int ticksUntilAction;
		private final int waitTicks;
		private final boolean deathObserved;
		private final float expectedNutrition;
		private final float expectedExhaustion;
		private final int expectedTicks;
		private final String details;

		private ScenarioState(
			Phase phase,
			int ticksUntilAction,
			int waitTicks,
			boolean deathObserved,
			float expectedNutrition,
			float expectedExhaustion,
			int expectedTicks,
			String details
		) {
			this.phase = phase;
			this.ticksUntilAction = ticksUntilAction;
			this.waitTicks = waitTicks;
			this.deathObserved = deathObserved;
			this.expectedNutrition = expectedNutrition;
			this.expectedExhaustion = expectedExhaustion;
			this.expectedTicks = expectedTicks;
			this.details = details;
		}

		private static ScenarioState scheduleDevKill(int ticksUntilAction) {
			return new ScenarioState(Phase.SCHEDULE_DEV_KILL, ticksUntilAction, 0, false, 0.0F, 0.0F, 0, "scheduled dev kill");
		}

		private static ScenarioState waitDevRespawn(float expectedNutrition, float expectedExhaustion) {
			return new ScenarioState(
				Phase.WAIT_DEV_RESPAWN,
				0,
				0,
				false,
				expectedNutrition,
				expectedExhaustion,
				0,
				"waiting dev respawn"
			);
		}

		private static ScenarioState scheduleHardcoreKill(int ticksUntilAction) {
			return new ScenarioState(
				Phase.SCHEDULE_HARDCORE_KILL,
				ticksUntilAction,
				0,
				false,
				0.0F,
				0.0F,
				0,
				"scheduled hardcore kill"
			);
		}

		private static ScenarioState waitHardcoreRespawn(float expectedNutrition, float expectedExhaustion, int expectedTicks) {
			return new ScenarioState(
				Phase.WAIT_HARDCORE_RESPAWN,
				0,
				0,
				false,
				expectedNutrition,
				expectedExhaustion,
				expectedTicks,
				"waiting hardcore respawn"
			);
		}

		private static ScenarioState completed() {
			return new ScenarioState(Phase.COMPLETED, 0, 0, false, 0.0F, 0.0F, 0, "completed");
		}

		private static ScenarioState failed(String details) {
			return new ScenarioState(Phase.FAILED, 0, 0, false, 0.0F, 0.0F, 0, details);
		}

		private ScenarioState withTicks(int ticksUntilAction) {
			return new ScenarioState(
				phase,
				ticksUntilAction,
				waitTicks,
				deathObserved,
				expectedNutrition,
				expectedExhaustion,
				expectedTicks,
				details
			);
		}

		private ScenarioState withWaitTicks(int waitTicks) {
			return new ScenarioState(
				phase,
				ticksUntilAction,
				waitTicks,
				deathObserved,
				expectedNutrition,
				expectedExhaustion,
				expectedTicks,
				details
			);
		}

		private ScenarioState markDeathObserved() {
			if (deathObserved) {
				return this;
			}

			return new ScenarioState(
				phase,
				ticksUntilAction,
				waitTicks,
				true,
				expectedNutrition,
				expectedExhaustion,
				expectedTicks,
				details
			);
		}

		private String describe() {
			if (phase == Phase.SCHEDULE_DEV_KILL || phase == Phase.SCHEDULE_HARDCORE_KILL) {
				return phase + " in " + ticksUntilAction + " ticks";
			}
			if (phase == Phase.WAIT_DEV_RESPAWN || phase == Phase.WAIT_HARDCORE_RESPAWN) {
				return phase + " waited=" + waitTicks + " ticks";
			}

			return phase + " (" + details + ")";
		}
	}
}
