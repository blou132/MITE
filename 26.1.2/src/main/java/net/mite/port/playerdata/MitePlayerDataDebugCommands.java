package net.mite.port.playerdata;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MitePlayerDataDebugCommands {
	private MitePlayerDataDebugCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(MitePlayerDataDebugCommands::registerCommands);
	}

	private static void registerCommands(
		CommandDispatcher<CommandSourceStack> dispatcher,
		net.minecraft.commands.CommandBuildContext context,
		net.minecraft.commands.Commands.CommandSelection selection
	) {
		dispatcher.register(
			Commands.literal("mite")
				.then(Commands.literal("debug")
					.requires(MitePlayerDataDebugCommands::hasDebugPermission)
					.then(Commands.literal("playerdata").executes(ctx -> dumpPlayerData(ctx.getSource())))
					.then(Commands.literal("dump").executes(ctx -> dumpPlayerData(ctx.getSource())))
					.then(Commands.literal("setnutrition")
						.then(Commands.argument("value", floatArg(0.0F, MitePlayerData.MAX_NUTRITION_LEVEL))
							.executes(ctx -> setNutrition(ctx.getSource(), getFloat(ctx, "value")))
						)
					)
					.then(Commands.literal("setexhaustion")
						.then(Commands.argument("value", floatArg(0.0F))
							.executes(ctx -> setExhaustion(ctx.getSource(), getFloat(ctx, "value")))
						)
					)
					.then(Commands.literal("setticks")
						.then(Commands.argument("value", integer(0))
							.executes(ctx -> setTicks(ctx.getSource(), getInteger(ctx, "value")))
						)
					)
					.then(Commands.literal("savenow").executes(ctx -> saveNow(ctx.getSource())))
					.then(Commands.literal("killtest").executes(ctx -> runKillTest(ctx.getSource(), false)))
					.then(Commands.literal("killtest_hardcore").executes(ctx -> runKillTest(ctx.getSource(), true)))
					.then(Commands.literal("autotest")
						.then(Commands.literal("start").executes(ctx -> startAutoTest(ctx.getSource())))
						.then(Commands.literal("status").executes(ctx -> autoTestStatus(ctx.getSource())))
						.then(Commands.literal("stop").executes(ctx -> stopAutoTest(ctx.getSource())))
					)
				)
		);
	}

	private static boolean hasDebugPermission(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return "Server".equals(source.getTextName());
		}

		MinecraftServer server = source.getServer();
		return server.isSingleplayerOwner(player.nameAndId()) || server.getPlayerList().isOp(player.nameAndId());
	}

	private static int dumpPlayerData(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		MitePlayerData data = MitePlayerDataManager.getOrCreate(server, player);
		MitePlayerDataStore store = MitePlayerDataManager.store(server);
		int foodLevel = player.getFoodData().getFoodLevel();

		source.sendSuccess(() -> Component.literal(
			"mite.playerdata player="
				+ player.getName().getString()
				+ " uuid=" + player.getUUID()
				+ " v=" + data.dataVersion()
				+ " nutrition=" + format(data.nutritionLevel())
				+ " exhaustion=" + format(data.miteExhaustion())
				+ " ticks=" + data.ticksSinceNutritionUpdate()
				+ " hardcore=" + data.hardcoreRulesActive()
				+ " survival=" + data.survivalRulesActive()
				+ " food=" + foodLevel
				+ " trackedPlayers=" + store.playerCount()
				+ " scenario=" + MitePlayerDataDebugScenario.status(player)
		), false);
		return 1;
	}

	private static int setNutrition(CommandSourceStack source, float value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		MitePlayerData current = MitePlayerDataManager.getOrCreate(server, player);
		MitePlayerData updated = new MitePlayerData(
			current.dataVersion(),
			value,
			current.miteExhaustion(),
			current.ticksSinceNutritionUpdate(),
			current.hardcoreRulesActive(),
			current.survivalRulesActive()
		);
		MitePlayerDataManager.update(server, player, updated);
		player.getFoodData().setFoodLevel(Math.round(updated.nutritionLevel()));
		source.sendSuccess(() -> Component.literal("mite.playerdata setnutrition=" + format(updated.nutritionLevel())), false);
		return 1;
	}

	private static int setExhaustion(CommandSourceStack source, float value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		MitePlayerData current = MitePlayerDataManager.getOrCreate(server, player);
		MitePlayerData updated = new MitePlayerData(
			current.dataVersion(),
			current.nutritionLevel(),
			value,
			current.ticksSinceNutritionUpdate(),
			current.hardcoreRulesActive(),
			current.survivalRulesActive()
		);
		MitePlayerDataManager.update(server, player, updated);
		source.sendSuccess(() -> Component.literal("mite.playerdata setexhaustion=" + format(updated.miteExhaustion())), false);
		return 1;
	}

	private static int setTicks(CommandSourceStack source, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		MitePlayerData current = MitePlayerDataManager.getOrCreate(server, player);
		MitePlayerData updated = current.withTicksSinceNutritionUpdate(value);
		MitePlayerDataManager.update(server, player, updated);
		source.sendSuccess(() -> Component.literal("mite.playerdata setticks=" + updated.ticksSinceNutritionUpdate()), false);
		return 1;
	}

	private static int runKillTest(CommandSourceStack source, boolean hardcoreMode)
		throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		source.sendSuccess(() -> Component.literal("mite.playerdata killtest mode=" + (hardcoreMode ? "hardcore" : "dev")), true);
		MitePlayerDataDebugScenario.triggerSingleKillTest(server, player, hardcoreMode);
		return 1;
	}

	private static int startAutoTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = source.getServer();
		MitePlayerDataDebugScenario.start(server, player);
		source.sendSuccess(() -> Component.literal("mite.playerdata autotest started"), false);
		return 1;
	}

	private static int autoTestStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		source.sendSuccess(() -> Component.literal("mite.playerdata autotest status=" + MitePlayerDataDebugScenario.status(player)), false);
		return 1;
	}

	private static int stopAutoTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		MitePlayerDataDebugScenario.stop(player);
		source.sendSuccess(() -> Component.literal("mite.playerdata autotest stopped"), false);
		return 1;
	}

	private static int saveNow(CommandSourceStack source) {
		MitePlayerDataManager.saveNow(source.getServer());
		source.sendSuccess(() -> Component.literal("mite.playerdata saveNow complete"), false);
		return 1;
	}

	private static String format(float value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}
}
