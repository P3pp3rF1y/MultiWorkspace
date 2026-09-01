package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class BackpackLifecycleRegression {
	private BackpackLifecycleRegression() {
	}

	static void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			JsonArray tiers = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject()
					.getAsJsonArray("tiers");
			if (tiers == null || tiers.isEmpty()) {
				throw new IllegalArgumentException("Backpack lifecycle regression requires at least one tier");
			}
			send(exchange, AutomationRuntime.runOnServer(player -> run(player, tiers)));
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	private static String run(ServerPlayer player, JsonArray tiers) {
		StringBuilder results = new StringBuilder();
		boolean passed = true;
		for (int index = 0; index < tiers.size(); index++) {
			JsonObject tier = tiers.get(index).getAsJsonObject();
			String name = tier.get("name").getAsString();
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tier.get("item").getAsString())).map(reference -> reference.value()).orElse(null);
			boolean tierPassed = item instanceof BackpackItem backpackItem
					&& runTier(player, backpackItem, tier.get("mainColor").getAsInt(), tier.get("accentColor").getAsInt());
			passed &= tierPassed;
			if (index > 0) {
				results.append(',');
			}
			results.append("{\"name\":").append(quote(name)).append(",\"passed\":").append(tierPassed).append('}');
		}
		return "{\"ok\":" + passed + ",\"results\":[" + results + "]}";
	}

	private static boolean runTier(ServerPlayer player, BackpackItem backpackItem, int mainColor, int accentColor) {
		GameType gameType = player.gameMode.getGameModeForPlayer();
		ServerLevel level = player.level();
		BlockPos breakPos = player.blockPosition().relative(player.getDirection(), 5);
		BlockPos pickupPos = breakPos.east(2);
		try {
			player.closeContainer();
			player.getInventory().clearContent();
			player.setGameMode(GameType.SURVIVAL);
			clear(level, breakPos);
			clear(level, pickupPos);

			BackpackRegressionFixture.Fixture breakFixture = BackpackRegressionFixture.create(backpackItem, mainColor, accentColor);
			place(level, player, breakPos, breakFixture.backpack());
			boolean placed = dataAt(level, breakPos, backpackItem, breakFixture);
			boolean opened = placed
					&& player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(breakPos), Direction.UP, breakPos, false)).consumesAction()
					&& player.containerMenu instanceof BackpackContainer;
			player.closeContainer();
			boolean broken = opened && player.gameMode.destroyBlock(breakPos);
			boolean breakPreserved = broken && BackpackRegressionFixture.dataError(findDrop(level, breakPos, backpackItem), backpackItem, breakFixture) == null;

			BackpackRegressionFixture.Fixture pickupFixture = BackpackRegressionFixture.create(backpackItem, mainColor, accentColor);
			place(level, player, pickupPos, pickupFixture.backpack());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(pickupPos), Direction.UP, pickupPos, false));
			player.setShiftKeyDown(false);
			ItemStack pickedUp = player.getMainHandItem();
			boolean pickupPreserved = level.getBlockState(pickupPos).isAir()
					&& BackpackRegressionFixture.dataError(pickedUp, backpackItem, pickupFixture) == null;
			return placed && opened && breakPreserved && pickupPreserved;
		} finally {
			player.closeContainer();
			player.setShiftKeyDown(false);
			player.getInventory().clearContent();
			player.setGameMode(gameType);
			clear(level, breakPos);
			clear(level, pickupPos);
		}
	}

	private static boolean dataAt(ServerLevel level, BlockPos pos, BackpackItem item, BackpackRegressionFixture.Fixture fixture) {
		return level.getBlockEntity(pos, ModBlocks.BACKPACK_TILE_TYPE.get())
				.map(blockEntity -> BackpackRegressionFixture.dataError(blockEntity.getBackpackWrapper().getBackpack(), item, fixture) == null).orElse(false);
	}

	private static ItemStack findDrop(ServerLevel level, BlockPos pos, Item item) {
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), candidate -> candidate.getItem().is(item))) {
			ItemStack stack = entity.getItem().copy();
			entity.discard();
			return stack;
		}
		return ItemStack.EMPTY;
	}

	private static void place(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos support = pos.below();
		level.setBlock(support, Blocks.DIRT.defaultBlockState(), 3);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
		player.setShiftKeyDown(false);
	}

	private static void clear(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
	}

	private static String quote(String value) {
		return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static void send(HttpExchange exchange, String response) throws IOException {
		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}
}
