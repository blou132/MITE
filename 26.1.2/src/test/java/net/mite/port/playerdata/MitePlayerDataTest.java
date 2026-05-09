package net.mite.port.playerdata;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MitePlayerDataTest {
	@Test
	void codecRoundTripPreservesValues() {
		MitePlayerData initial = new MitePlayerData(1, 17.5F, 1.25F, 24, true, true);

		JsonElement encoded = unwrap(MitePlayerData.CODEC.encodeStart(JsonOps.INSTANCE, initial));
		MitePlayerData decoded = unwrap(MitePlayerData.CODEC.parse(JsonOps.INSTANCE, encoded));

		Assertions.assertEquals(initial, decoded);
	}

	@Test
	void constructorNormalizesOutOfRangeValues() {
		MitePlayerData normalized = new MitePlayerData(0, 30.0F, -4.0F, -10, false, true);

		Assertions.assertEquals(MitePlayerData.CURRENT_DATA_VERSION, normalized.dataVersion());
		Assertions.assertEquals(MitePlayerData.MAX_NUTRITION_LEVEL, normalized.nutritionLevel());
		Assertions.assertEquals(0.0F, normalized.miteExhaustion());
		Assertions.assertEquals(0, normalized.ticksSinceNutritionUpdate());
	}

	@Test
	void storeCreatesAndUpdatesPerPlayerData() {
		MitePlayerDataStore store = new MitePlayerDataStore();
		UUID playerId = UUID.randomUUID();

		MitePlayerData created = store.getOrCreate(playerId, () -> MitePlayerData.createDefault(true, true));
		Assertions.assertEquals(1, store.playerCount());
		Assertions.assertEquals(created, store.get(playerId));

		MitePlayerData updated = created.withNutritionCheckpoint(0.35F, 15.0F);
		store.put(playerId, updated);
		Assertions.assertEquals(updated, store.get(playerId));
		Assertions.assertEquals(1, store.playerCount());
	}

	@Test
	void storeCodecRoundTripPreservesPlayers() {
		MitePlayerDataStore store = new MitePlayerDataStore();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();

		store.put(first, new MitePlayerData(1, 18.0F, 0.35F, 12, true, true));
		store.put(second, new MitePlayerData(1, 9.0F, 1.1F, 3, false, false));

		JsonElement encoded = unwrap(MitePlayerDataStore.codec().encodeStart(JsonOps.INSTANCE, store));
		MitePlayerDataStore decoded = unwrap(MitePlayerDataStore.codec().parse(JsonOps.INSTANCE, encoded));

		Assertions.assertEquals(2, decoded.playerCount());
		Assertions.assertEquals(store.get(first), decoded.get(first));
		Assertions.assertEquals(store.get(second), decoded.get(second));
	}

	@Test
	void migrationV1ToV2PlaceholderIsStable() {
		MitePlayerData v1 = new MitePlayerData(1, 16.0F, 0.6F, 8, true, true);
		MitePlayerData migrated = MitePlayerDataMigration.migrateV1ToV2(v1);
		Assertions.assertEquals(v1, migrated);
	}

	@Test
	void nonHardcoreRespawnResetsTickAccumulatorOnly() {
		MitePlayerData beforeDeath = new MitePlayerData(1, 14.0F, 1.4F, 39, false, true);

		MitePlayerData afterRespawn = MitePlayerLifecyclePolicy.onRespawn(beforeDeath, false, true, false);

		Assertions.assertEquals(14.0F, afterRespawn.nutritionLevel());
		Assertions.assertEquals(1.4F, afterRespawn.miteExhaustion());
		Assertions.assertEquals(0, afterRespawn.ticksSinceNutritionUpdate());
		Assertions.assertFalse(afterRespawn.hardcoreRulesActive());
		Assertions.assertTrue(afterRespawn.survivalRulesActive());
	}

	@Test
	void hardcoreRespawnKeepsStrictState() {
		MitePlayerData beforeDeath = new MitePlayerData(1, 12.0F, 2.2F, 25, true, true);

		MitePlayerData afterRespawn = MitePlayerLifecyclePolicy.onRespawn(beforeDeath, true, true, false);

		Assertions.assertEquals(beforeDeath.nutritionLevel(), afterRespawn.nutritionLevel());
		Assertions.assertEquals(beforeDeath.miteExhaustion(), afterRespawn.miteExhaustion());
		Assertions.assertEquals(beforeDeath.ticksSinceNutritionUpdate(), afterRespawn.ticksSinceNutritionUpdate());
		Assertions.assertTrue(afterRespawn.hardcoreRulesActive());
	}

	@Test
	void deathHookPreservesDataAndRefreshesHardcoreFlag() {
		MitePlayerData liveData = new MitePlayerData(1, 19.0F, 0.2F, 11, false, true);
		MitePlayerData afterDeath = MitePlayerLifecyclePolicy.onFatalDamage(liveData, true);

		Assertions.assertEquals(liveData.nutritionLevel(), afterDeath.nutritionLevel());
		Assertions.assertEquals(liveData.miteExhaustion(), afterDeath.miteExhaustion());
		Assertions.assertEquals(liveData.ticksSinceNutritionUpdate(), afterDeath.ticksSinceNutritionUpdate());
		Assertions.assertTrue(afterDeath.hardcoreRulesActive());
	}

	@Test
	void respawnDataPersistsAcrossStoreSaveReload() {
		UUID playerId = UUID.randomUUID();
		MitePlayerData beforeDeath = new MitePlayerData(1, 10.0F, 1.0F, 22, false, true);
		MitePlayerData afterRespawn = MitePlayerLifecyclePolicy.onRespawn(beforeDeath, false, true, false);

		MitePlayerDataStore store = new MitePlayerDataStore();
		store.put(playerId, afterRespawn);

		JsonElement encoded = unwrap(MitePlayerDataStore.codec().encodeStart(JsonOps.INSTANCE, store));
		MitePlayerDataStore reloaded = unwrap(MitePlayerDataStore.codec().parse(JsonOps.INSTANCE, encoded));
		MitePlayerData persisted = reloaded.get(playerId);

		Assertions.assertNotNull(persisted);
		Assertions.assertEquals(afterRespawn, persisted);
		Assertions.assertEquals(0, persisted.ticksSinceNutritionUpdate());
	}

	private static <T> T unwrap(DataResult<T> result) {
		return result.result().orElseThrow(() -> new AssertionError("Codec operation failed"));
	}
}
