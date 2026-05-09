package net.mite.port.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.mite.port.MitePortMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class MitePlayerDataStore extends SavedData {
	private static final Codec<Map<UUID, MitePlayerData>> PLAYERS_CODEC = Codec.unboundedMap(
		UUIDUtil.STRING_CODEC,
		MitePlayerData.CODEC
	);

	private static final Codec<MitePlayerDataStore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PLAYERS_CODEC.optionalFieldOf("players", Map.of()).forGetter(MitePlayerDataStore::playersForCodec)
	).apply(instance, MitePlayerDataStore::fromCodec));

	public static final SavedDataType<MitePlayerDataStore> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(MitePortMod.MOD_ID, "player_data"),
		MitePlayerDataStore::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, MitePlayerData> players;

	public MitePlayerDataStore() {
		this.players = new HashMap<>();
	}

	private MitePlayerDataStore(Map<UUID, MitePlayerData> players) {
		this.players = new HashMap<>(players);
	}

	private static MitePlayerDataStore fromCodec(Map<UUID, MitePlayerData> players) {
		Map<UUID, MitePlayerData> migrated = new HashMap<>();
		for (Map.Entry<UUID, MitePlayerData> entry : players.entrySet()) {
			migrated.put(entry.getKey(), MitePlayerDataMigration.migrateToCurrent(entry.getValue()));
		}
		return new MitePlayerDataStore(migrated);
	}

	private Map<UUID, MitePlayerData> playersForCodec() {
		return players;
	}

	static Codec<MitePlayerDataStore> codec() {
		return CODEC;
	}

	public MitePlayerData getOrCreate(UUID playerId, Supplier<MitePlayerData> defaultFactory) {
		MitePlayerData existing = players.get(playerId);
		if (existing != null) {
			MitePlayerData migrated = MitePlayerDataMigration.migrateToCurrent(existing);
			if (!migrated.equals(existing)) {
				players.put(playerId, migrated);
				setDirty();
			}
			return migrated;
		}

		MitePlayerData created = defaultFactory.get();
		players.put(playerId, created);
		setDirty();
		return created;
	}

	public MitePlayerData get(UUID playerId) {
		return players.get(playerId);
	}

	public void put(UUID playerId, MitePlayerData data) {
		MitePlayerData migrated = MitePlayerDataMigration.migrateToCurrent(data);
		MitePlayerData previous = players.put(playerId, migrated);
		if (!migrated.equals(previous)) {
			setDirty();
		}
	}

	public int playerCount() {
		return players.size();
	}
}
