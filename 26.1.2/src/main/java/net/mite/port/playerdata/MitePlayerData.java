package net.mite.port.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MitePlayerData(
	int dataVersion,
	float nutritionLevel,
	float miteExhaustion,
	int ticksSinceNutritionUpdate,
	boolean hardcoreRulesActive,
	boolean survivalRulesActive
) {
	public static final int CURRENT_DATA_VERSION = 1;
	public static final float MAX_NUTRITION_LEVEL = 20.0F;
	private static final float MIN_NUTRITION_LEVEL = 0.0F;

	public static final Codec<MitePlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(MitePlayerData::dataVersion),
		Codec.FLOAT.optionalFieldOf("nutrition_level", MAX_NUTRITION_LEVEL).forGetter(MitePlayerData::nutritionLevel),
		Codec.FLOAT.optionalFieldOf("mite_exhaustion", 0.0F).forGetter(MitePlayerData::miteExhaustion),
		Codec.INT.optionalFieldOf("ticks_since_nutrition_update", 0).forGetter(MitePlayerData::ticksSinceNutritionUpdate),
		Codec.BOOL.optionalFieldOf("hardcore_rules_active", false).forGetter(MitePlayerData::hardcoreRulesActive),
		Codec.BOOL.optionalFieldOf("survival_rules_active", true).forGetter(MitePlayerData::survivalRulesActive)
	).apply(instance, MitePlayerData::new));

	public MitePlayerData {
		dataVersion = Math.max(CURRENT_DATA_VERSION, dataVersion);
		nutritionLevel = clampNutrition(nutritionLevel);
		miteExhaustion = Math.max(0.0F, miteExhaustion);
		ticksSinceNutritionUpdate = Math.max(0, ticksSinceNutritionUpdate);
	}

	private static float clampNutrition(float nutritionLevel) {
		return Math.max(MIN_NUTRITION_LEVEL, Math.min(MAX_NUTRITION_LEVEL, nutritionLevel));
	}

	public static MitePlayerData createDefault(boolean hardcoreRulesActive, boolean survivalRulesActive) {
		return new MitePlayerData(CURRENT_DATA_VERSION, MAX_NUTRITION_LEVEL, 0.0F, 0, hardcoreRulesActive, survivalRulesActive);
	}

	public MitePlayerData withGameplayFlags(boolean hardcoreRulesActive, boolean survivalRulesActive) {
		return new MitePlayerData(
			dataVersion,
			nutritionLevel,
			miteExhaustion,
			ticksSinceNutritionUpdate,
			hardcoreRulesActive,
			survivalRulesActive
		);
	}

	public MitePlayerData withTickProgress() {
		return new MitePlayerData(
			dataVersion,
			nutritionLevel,
			miteExhaustion,
			ticksSinceNutritionUpdate + 1,
			hardcoreRulesActive,
			survivalRulesActive
		);
	}

	public MitePlayerData withNutritionCheckpoint(float addedExhaustion, float refreshedNutritionLevel) {
		return new MitePlayerData(
			dataVersion,
			refreshedNutritionLevel,
			miteExhaustion + addedExhaustion,
			0,
			hardcoreRulesActive,
			survivalRulesActive
		);
	}
}
