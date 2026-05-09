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

	private static <T> T unwrap(DataResult<T> result) {
		return result.result().orElseThrow(() -> new AssertionError("Codec operation failed"));
	}
}
