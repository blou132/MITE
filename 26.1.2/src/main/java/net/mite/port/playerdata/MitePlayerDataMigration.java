package net.mite.port.playerdata;

public final class MitePlayerDataMigration {
	private MitePlayerDataMigration() {
	}

	public static MitePlayerData migrateToCurrent(MitePlayerData storedData) {
		if (storedData.dataVersion() <= 1) {
			return migrateFromV1(storedData);
		}

		return storedData;
	}

	private static MitePlayerData migrateFromV1(MitePlayerData v1) {
		// Current runtime still targets v1, so this is intentionally a pass-through.
		// It centralizes migration entry for future schema changes.
		return migrateV1ToV2(v1);
	}

	public static MitePlayerData migrateV1ToV2(MitePlayerData v1) {
		// Placeholder for future migration logic.
		return v1;
	}
}
