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
		return new MitePlayerDataStore(players);
	}

	private Map<UUID, MitePlayerData> playersForCodec() {
		return players;
	}

	public MitePlayerData getOrCreate(UUID playerId, Supplier<MitePlayerData> defaultFactory) {
		MitePlayerData existing = players.get(playerId);
		if (existing != null) {
			return existing;
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
		MitePlayerData previous = players.put(playerId, data);
		if (!data.equals(previous)) {
			setDirty();
		}
	}

	public int playerCount() {
		return players.size();
	}
}
