package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class BackpackRegressionEndpoints {
	private BackpackRegressionEndpoints() {
	}

	public static void runFilter(HttpExchange exchange) throws IOException {
		run(exchange, "POST", request -> AutomationRuntime.runOnServer(player -> filter(request)));
	}

	public static void runMagnet(HttpExchange exchange) throws IOException {
		run(exchange, "POST", request -> AutomationRuntime.runOnServer(player -> magnet(player, request)));
	}

	public static void runPickup(HttpExchange exchange) throws IOException {
		run(exchange, "POST", request -> AutomationRuntime.runOnServer(player -> pickup(player, request)));
	}

	public static void runRestock(HttpExchange exchange) throws IOException {
		run(exchange, "POST", request -> AutomationRuntime.runOnServer(player -> restock(player, request)));
	}

	public static void runRefill(HttpExchange exchange) throws IOException {
		run(exchange, "POST", request -> AutomationRuntime.runOnServer(player -> refill(player, request)));
	}

	public static void runLinkedStorage(HttpExchange exchange) throws IOException {
		BackpackLinkedStorageRegression.handle(exchange);
	}

	public static void runLinkedStorageInception(HttpExchange exchange) throws IOException {
		BackpackLinkedStorageRegression.handleInceptionLinkedChild(exchange);
	}

	public static void runLifecycle(HttpExchange exchange) throws IOException {
		BackpackLifecycleRegression.handle(exchange);
	}

	public static void runAccess(HttpExchange exchange) throws IOException {
		BackpackAccessRegression.handle(exchange);
	}

	public static void runCuriosAccess(HttpExchange exchange) throws IOException {
		BackpackAccessRegression.handleCurios(exchange);
	}

	private static void run(HttpExchange exchange, String method, Function<JsonObject, String> action) throws IOException {
		if (!method.equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			send(exchange, action.apply(body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject()));
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	private static String filter(JsonObject request) {
		boolean advanced = "advanced".equals(string(request, "upgrade", "basic"));
		boolean output = "output".equals(string(request, "operation", "input"));
		ItemStack backpack = backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(advanced ? ModItems.ADVANCED_FILTER_UPGRADE.get() : ModItems.FILTER_UPGRADE.get()));
		FilterUpgradeWrapper filter = exactly(upgrades.getWrappersThatImplement(FilterUpgradeWrapper.class), "filter upgrade");
		filter.setDirection(
				output ? net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.OUTPUT : net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction.INPUT);
		filter.getFilterLogic().setDepositFilterType(output ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW);
		Item configured = output ? Items.REDSTONE : Items.DIAMOND;
		filter.getFilterLogic().getFilterHandler().setStackInSlot(filter.getFilterLogic().getFilterHandler().size() - 1, new ItemStack(configured));
		upgrades.saveInventory();
		wrapper.refreshInventoryForInputOutput();
		ITrackedContentsItemResourceHandler inventory = wrapper.getInventoryForInputOutput();
		int first;
		int second;
		try (Transaction transaction = Transaction.openRoot()) {
			if (output) {
				wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.REDSTONE));
				wrapper.getInventoryHandler().setStackInSlot(1, new ItemStack(Items.DIAMOND));
				first = inventory.extract(ItemResource.of(new ItemStack(Items.REDSTONE)), 1, transaction);
				second = inventory.extract(ItemResource.of(new ItemStack(Items.DIAMOND)), 1, transaction);
			} else {
				first = inventory.insert(ItemResource.of(new ItemStack(Items.DIAMOND)), 1, transaction);
				second = inventory.insert(ItemResource.of(new ItemStack(Items.REDSTONE)), 1, transaction);
			}
			transaction.commit();
		}
		boolean passed = output ? first == 0 && second == 1 : first == 1 && second == 0;
		return "{\"ok\":" + passed + ",\"upgrade\":" + quote(advanced ? "advanced" : "basic") + ",\"operation\":" + quote(output ? "output" : "input") + "}";
	}

	private static String magnet(ServerPlayer player, JsonObject request) {
		boolean advanced = "advanced".equals(string(request, "upgrade", "basic"));
		String mode = string(request, "mode", "allow");
		Item item = "block".equals(mode) ? Items.REDSTONE : Items.DIAMOND;
		ItemStack backpack = backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(advanced ? ModItems.ADVANCED_MAGNET_UPGRADE.get() : ModItems.MAGNET_UPGRADE.get()));
		MagnetUpgradeWrapper magnet = exactly(wrapper.getUpgradeHandler().getWrappersThatImplement(MagnetUpgradeWrapper.class), "magnet upgrade");
		magnet.setPickupItems(!"disabled".equals(mode));
		magnet.getFilterLogic().setDepositFilterType("block".equals(mode) ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW);
		magnet.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(item));
		wrapper.getUpgradeHandler().saveInventory();
		player.getInventory().clearContent();
		player.getInventory().setItem(0, backpack);
		ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), new ItemStack(item));
		entity.setPickUpDelay(0);
		player.level().addFreshEntity(entity);
		entity.playerTouch(player);
		int stored = count(wrapper.getInventoryHandler(), item);
		boolean expectedPickup = "allow".equals(mode);
		boolean passed = expectedPickup ? stored > 0 : stored == 0;
		return "{\"ok\":" + passed + ",\"settingsPersisted\":true,\"storedItems\":" + stored + "}";
	}

	private static String pickup(ServerPlayer player, JsonObject request) {
		boolean advanced = "advanced".equals(string(request, "upgrade", "basic"));
		String mode = string(request, "mode", "allow");
		Item item = "block".equals(mode) ? Items.REDSTONE : Items.DIAMOND;
		ItemStack backpack = backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int slot = 0; slot < inventory.size(); slot++) {
			inventory.setStackInSlot(slot, "full".equals(mode) ? new ItemStack(Items.COBBLESTONE, 64) : ItemStack.EMPTY);
		}
		inventory.saveInventory();
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(advanced ? ModItems.ADVANCED_PICKUP_UPGRADE.get() : ModItems.PICKUP_UPGRADE.get()));
		PickupUpgradeWrapper pickup = exactly(wrapper.getUpgradeHandler().getWrappersThatImplement(PickupUpgradeWrapper.class), "pickup upgrade");
		pickup.getFilterLogic().setDepositFilterType("block".equals(mode) ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW);
		pickup.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(item));
		wrapper.getUpgradeHandler().saveInventory();
		player.getInventory().clearContent();
		player.getInventory().setItem(0, backpack);
		ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), new ItemStack(item));
		entity.setPickUpDelay(0);
		player.level().addFreshEntity(entity);
		entity.playerTouch(player);
		boolean expectedBackpack = "allow".equals(mode);
		int stored = count(wrapper.getInventoryHandler(), item);
		int playerItems = countPlayer(player, item);
		boolean passed = expectedBackpack ? stored == 1 && playerItems == 0 : stored == 0 && playerItems == 1;
		return "{\"ok\":" + passed + ",\"storedItems\":" + stored + ",\"playerItems\":" + playerItems + "}";
	}

	private static String restock(ServerPlayer player, JsonObject request) {
		boolean advanced = "advanced".equals(string(request, "upgrade", "basic"));
		String mode = string(request, "mode", "allow");
		ItemStack backpack = backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(advanced ? ModItems.ADVANCED_RESTOCK_UPGRADE.get() : ModItems.RESTOCK_UPGRADE.get()));
		RestockUpgradeWrapper restock = exactly(wrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class), "restock upgrade");
		restock.getFilterLogic().setDepositFilterType("block".equals(mode) ? ContentsFilterType.BLOCK : ContentsFilterType.ALLOW);
		restock.getFilterLogic().getFilterHandler().setStackInSlot(restock.getFilterLogic().getFilterHandler().size() - 1,
				new ItemStack("block".equals(mode) ? Items.REDSTONE : Items.DIAMOND));
		wrapper.getUpgradeHandler().saveInventory();
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition().relative(player.getDirection(), 3);
		level.setBlock(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get().defaultBlockState(), 3);
		StorageBlockEntity barrel = level.getBlockEntity(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get())
				.map(blockEntity -> (StorageBlockEntity) blockEntity).orElseThrow(() -> new IllegalStateException("Missing restock barrel"));
		if (!"empty".equals(mode)) {
			barrel.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
			barrel.getStorageWrapper().getInventoryHandler().saveInventory();
		}
		boolean handled = InventoryInteractionHelper.tryInventoryInteraction(pos, level, backpack, Direction.UP, player);
		int contents = count(wrapper.getInventoryHandler(), Items.DIAMOND);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		boolean passed = handled && ("empty".equals(mode) ? contents == 0 : contents == 5);
		return "{\"ok\":" + passed + ",\"interactionHandled\":" + handled + ",\"backpackDiamonds\":" + contents + "}";
	}

	private static String refill(ServerPlayer player, JsonObject request) {
		boolean advanced = "advanced".equals(string(request, "upgrade", "basic"));
		String mode = string(request, "mode", "allow");
		ItemStack backpack = backpack();
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		if (!"empty".equals(mode)) {
			wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 32));
			wrapper.getInventoryHandler().saveInventory();
		}
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(advanced ? ModItems.ADVANCED_REFILL_UPGRADE.get() : ModItems.REFILL_UPGRADE.get()));
		RefillUpgradeWrapper refill = exactly(wrapper.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class), "refill upgrade");
		int filterSlot = refill.getFilterLogic().getFilterHandler().size() - 1;
		refill.getFilterLogic().getFilterHandler().setStackInSlot(filterSlot, new ItemStack(Items.DIAMOND));
		wrapper.getUpgradeHandler().saveInventory();
		player.getInventory().clearContent();
		player.getInventory().setItem(8, backpack);
		player.getInventory().setItem(0, new ItemStack("mismatch".equals(mode) ? Items.REDSTONE : Items.DIAMOND, 5));
		refill.tick(player, player.level(), player.blockPosition());
		int target = player.getInventory().getItem(0).getCount();
		boolean passed = "empty".equals(mode) || "mismatch".equals(mode) ? target == 5 : target == 37;
		return "{\"ok\":" + passed + ",\"playerTargetCount\":" + target + "}";
	}

	private static String linkedStorage(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition().east(3);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		ItemStack carried = backpack();
		BackpackWrapper.fromStack(carried).getInventoryHandler().setStackInSlot(0, new ItemStack(Items.NETHER_STAR, 7));
		BackpackWrapper.fromStack(carried).getInventoryHandler().saveInventory();
		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
		ItemStack placed = new ItemStack(ModItems.GOLD_BACKPACK.get());
		if (!LinkedStorageService.link(level, linker, carried) || !LinkedStorageService.link(level, linker, placed)) {
			throw new IllegalStateException("Could not create linked storage endpoints");
		}
		level.setBlock(pos, ModBlocks.BACKPACK.get().defaultBlockState(), 3);
		BackpackBlockEntity blockEntity = level.getBlockEntity(pos, ModBlocks.BACKPACK_TILE_TYPE.get()).orElseThrow();
		blockEntity.setBackpack(placed);
		IBackpackWrapper carriedCanonical = BackpackLinkedStorageResolver.resolve(level, carried).orElseThrow();
		IBackpackWrapper placedCanonical = BackpackLinkedStorageResolver.resolve(level, placed).orElseThrow();
		try {
			LinkedStorageEndpointData carriedEndpoint = carried.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			LinkedStorageEndpointData placedEndpoint = placed.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			boolean passed = carriedEndpoint != null && placedEndpoint != null && carriedEndpoint.groupId().equals(placedEndpoint.groupId())
					&& count(carriedCanonical.getInventoryHandler(), Items.NETHER_STAR) == 7
					&& count(placedCanonical.getInventoryHandler(), Items.NETHER_STAR) == 7;
			return "{\"ok\":" + passed + ",\"sharedGroup\":"
					+ (carriedEndpoint != null && placedEndpoint != null && carriedEndpoint.groupId().equals(placedEndpoint.groupId())) + "}";
		} finally {
			if (carriedCanonical instanceof LinkedStorageBackpackWrapper linked) {
				linked.close();
			}
			if (placedCanonical instanceof LinkedStorageBackpackWrapper linked) {
				linked.close();
			}
		}
	}

	private static ItemStack backpack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
	}

	private static <T> T exactly(List<T> values, String description) {
		if (values.size() != 1) {
			throw new IllegalStateException("Expected one " + description + ", found " + values.size());
		}
		return values.getFirst();
	}

	private static int count(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			if (inventory.getStackInSlot(slot).is(item)) {
				count += inventory.getStackInSlot(slot).getCount();
			}
		}
		return count;
	}

	private static int countPlayer(ServerPlayer player, Item item) {
		int count = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static String string(JsonObject request, String name, String fallback) {
		return request.has(name) ? request.get(name).getAsString() : fallback;
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
