package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class BackpackLifecycleRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackLifecycleRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String body = readBody(exchange);
		sendJsonHandling(exchange, () -> AutomationRuntime.runOnServer(player -> run(player, body)));
	}

	private static String run(ServerPlayer player, String body) {
		JsonArray tiers = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("tiers");
		if (tiers == null || tiers.isEmpty()) {
			throw new IllegalArgumentException("Backpack lifecycle regression requires at least one tier");
		}

		List<RegressionResult> results = new ArrayList<>();
		for (JsonElement tierElement : tiers) {
			JsonObject tier = tierElement.getAsJsonObject();
			String name = tier.get("name").getAsString();
			String itemId = tier.get("item").getAsString();
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null);
			if (!(item instanceof BackpackItem backpackItem)) {
				results.add(new RegressionResult(name, false, false, false, false, false, false, false, "Configured item is not a backpack: " + itemId));
				continue;
			}
			results.add(run(player, new RegressionSpec(name, backpackItem, tier.get("mainColor").getAsInt(), tier.get("accentColor").getAsInt())));
		}

		boolean passed = results.stream().allMatch(RegressionResult::passed);
		StringBuilder json = new StringBuilder("{\"ok\":").append(passed).append(",\"results\":[");
		for (int i = 0; i < results.size(); i++) {
			if (i > 0) {
				json.append(',');
			}
			RegressionResult result = results.get(i);
			json.append('{').append(jsonProperty("name", result.name())).append(",\"passed\":").append(result.passed()).append(",\"placed\":")
					.append(result.placed()).append(",\"opened\":").append(result.opened()).append(",\"broken\":").append(result.broken())
					.append(",\"breakDataPreserved\":").append(result.breakDataPreserved()).append(",\"pickedUp\":").append(result.pickedUp())
					.append(",\"pickupDataPreserved\":").append(result.pickupDataPreserved()).append(',').append(jsonProperty("error", result.error()))
					.append('}');
		}
		return json.append("]}").toString();
	}

	private static RegressionResult run(ServerPlayer player, RegressionSpec spec) {
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		ServerLevel level = player.level();
		BlockPos breakPos = regressionBackpackPos(player).offset(0, 0, 3);
		BlockPos pickupPos = breakPos.offset(2, 0, 0);
		boolean placed = false;
		boolean opened = false;
		boolean broken = false;
		boolean breakDataPreserved = false;
		boolean pickedUp = false;
		boolean pickupDataPreserved = false;
		String error = null;
		try {
			player.closeContainer();
			player.getInventory().clearContent();
			player.setGameMode(GameType.SURVIVAL);
			clearArea(level, breakPos);
			clearArea(level, pickupPos);

			BackpackRegressionFixture.Fixture breakFixture = BackpackRegressionFixture.create(spec.backpackItem(), spec.mainColor(), spec.accentColor());
			player.setShiftKeyDown(true);
			placeBlockWithItem(level, player, breakPos, breakFixture.backpack());
			player.setShiftKeyDown(false);
			placed = hasData(level, breakPos, spec, breakFixture);
			if (!placed) {
				error = "Placement did not preserve the backpack fixture";
			} else {
				player.setShiftKeyDown(false);
				BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(breakPos), Direction.UP, breakPos, false);
				opened = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitResult).consumesAction()
						&& player.containerMenu instanceof BackpackContainer;
				player.closeContainer();
				if (!opened) {
					error = "Placed backpack did not open through block interaction";
				} else {
					broken = player.gameMode.destroyBlock(breakPos);
					ItemStack droppedBackpack = findAndRemoveDroppedBackpack(level, breakPos, spec.backpackItem());
					breakDataPreserved = broken && BackpackRegressionFixture.dataError(droppedBackpack, spec.backpackItem(), breakFixture) == null;
					if (!breakDataPreserved) {
						error = broken ? "Broken backpack did not retain its fixture data" : "Player block destruction failed";
					}
				}
			}

			BackpackRegressionFixture.Fixture pickupFixture = BackpackRegressionFixture.create(spec.backpackItem(), spec.mainColor(), spec.accentColor());
			player.setShiftKeyDown(true);
			placeBlockWithItem(level, player, pickupPos, pickupFixture.backpack());
			player.setShiftKeyDown(false);
			if (!hasData(level, pickupPos, spec, pickupFixture)) {
				error = error == null ? "Pickup fixture placement did not retain backpack data" : error;
			} else {
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setShiftKeyDown(true);
				BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pickupPos), Direction.UP, pickupPos, false);
				player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitResult);
				player.setShiftKeyDown(false);
				ItemStack pickedUpBackpack = player.getMainHandItem();
				pickedUp = level.getBlockState(pickupPos).isAir() && pickedUpBackpack.is(spec.backpackItem());
				pickupDataPreserved = pickedUp && BackpackRegressionFixture.dataError(pickedUpBackpack, spec.backpackItem(), pickupFixture) == null;
				if (!pickupDataPreserved) {
					error = error == null ? "Shift-pickup did not retain the backpack fixture data" : error;
				}
			}
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			player.closeContainer();
			player.setShiftKeyDown(false);
			player.getInventory().clearContent();
			player.setGameMode(originalGameMode);
			clearArea(level, breakPos);
			clearArea(level, pickupPos);
		}

		boolean passed = placed && opened && broken && breakDataPreserved && pickedUp && pickupDataPreserved;
		return new RegressionResult(spec.name(), passed, placed, opened, broken, breakDataPreserved, pickedUp, pickupDataPreserved, error);
	}

	private static boolean hasData(ServerLevel level, BlockPos pos, RegressionSpec spec, BackpackRegressionFixture.Fixture fixture) {
		return level.getBlockEntity(pos, ModBlocks.BACKPACK_TILE_TYPE.get())
				.map(blockEntity -> BackpackRegressionFixture.dataError(blockEntity.getBackpackWrapper().getBackpack(), spec.backpackItem(), fixture) == null)
				.orElse(false);
	}

	private static ItemStack findAndRemoveDroppedBackpack(ServerLevel level, BlockPos pos, Item backpackItem) {
		AABB area = new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
		for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(backpackItem))) {
			ItemStack droppedBackpack = itemEntity.getItem().copy();
			itemEntity.discard();
			return droppedBackpack;
		}
		return ItemStack.EMPTY;
	}

	private static BlockPos regressionBackpackPos(ServerPlayer player) {
		return player.blockPosition().relative(player.getDirection(), 2);
	}

	private static void clearArea(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
	}

	private static void placeBlockWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);
	}

	private static String readBody(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private static void requireMethod(HttpExchange exchange, String method) throws IOException {
		if (!method.equals(exchange.getRequestMethod())) {
			byte[] response = "{\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(405, response.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(response);
			}
			throw new IllegalStateException("Method not allowed");
		}
	}

	private static void sendJsonHandling(HttpExchange exchange, Supplier<String> jsonSupplier) throws IOException {
		try {
			sendJson(exchange, jsonSupplier.get());
		} catch (RuntimeException e) {
			LOGGER.error("Automation endpoint failed", e);
			sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
		}
	}

	private static void sendJson(HttpExchange exchange, String json) throws IOException {
		byte[] response = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, response.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(response);
		}
	}

	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":" + (value == null ? "null" : "\"" + escapeJson(value) + "\"");
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private record RegressionSpec(String name, BackpackItem backpackItem, int mainColor, int accentColor) {
	}

	private record RegressionResult(String name, boolean passed, boolean placed, boolean opened, boolean broken, boolean breakDataPreserved, boolean pickedUp,
			boolean pickupDataPreserved, String error) {
	}
}
