package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class BackpackGuiRegressionRun {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final UUID SUB_MOB_CATCHER_PARENT_MOB_ID = new UUID(0L, 101L);
	private static final UUID SUB_MOB_CATCHER_SUB_MOB_ID = new UUID(0L, 102L);

	private BackpackGuiRegressionRun() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requirePost(exchange);
		try {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			JsonObject request = body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
			sendJson(exchange, run(request));
		} catch (RuntimeException e) {
			LOGGER.error("Automation endpoint failed", e);
			sendJson(exchange, "{\"ok\":false," + jsonProperty("error", e.getMessage()) + "}");
		}
	}

	private static String run(JsonObject request) {
		String name = request.has("name") ? request.get("name").getAsString() : "unnamed";
		String type = request.has("type") ? request.get("type").getAsString() : "columnUpgradeSync";
		return switch (type) {
			case "subMobCatcherImmediateOpen" -> runSubMobCatcherImmediateOpenRegression(name);
			case "advancedCompactingHighStack" -> runAdvancedCompactingHighStackRegression(name, request);
			case "depositLimitedBarrelGuiState" -> runDepositLimitedBarrelGuiStateRegression(name, request);
			case "columnUpgradeSync" -> runColumnUpgradeSyncRegression(name, request);
			default -> throw new IllegalArgumentException("Unknown backpack GUI regression type " + type);
		};
	}

	private static String runColumnUpgradeSyncRegression(String name, JsonObject request) {
		BackpackGuiRegressionContext context = BackpackGuiRegressionContext.fromName(request.has("context") ? request.get("context").getAsString() : "");
		resetState();
		prepare(context);
		waitForOpenMenu(context);
		PlacedColumnUpgradeClickExpectation expectation = clickWhenReady(context);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		PlacedColumnUpgradeState state;
		do {
			state = AutomationRuntime.runOnClient(() -> getState(context));
			if (state.matches(expectation)) {
				return columnJson(name, true, expectation, state, null);
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		return columnJson(name, false, expectation, state, "Timed out waiting for " + context.jsonName() + " backpack column sync");
	}

	private static String runAdvancedCompactingHighStackRegression(String name, JsonObject request) {
		resetState();
		AdvancedCompactingHighStackRegressionResult result = AutomationRuntime.runOnServer(player -> compact(player, name,
				getInt(request, "firstSlotCount", 16_384), getInt(request, "secondSlotCount", 16_000), getInt(request, "triggerCount", 8),
				getInt(request, "expectedNuggets", 12_936), getInt(request, "expectedIngots", 4), getInt(request, "expectedBlocks", 1_820)));
		return compactJson(result);
	}

	private static AdvancedCompactingHighStackRegressionResult compact(ServerPlayer player, String name, int first, int second, int trigger, int nuggets,
			int ingots, int blocks) {
		player.getInventory().clearContent();
		ItemStack backpack = createBackpack(80);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		upgrades.setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
		upgrades.setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_4.get()));
		upgrades.saveInventory();
		InventoryHandler inventory = wrapper.getInventoryHandler();
		inventory.setStackInSlot(0, new ItemStack(Items.IRON_NUGGET, first));
		inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, second));
		inventory.setStackInSlot(2, new ItemStack(Items.IRON_BLOCK));
		inventory.setStackInSlot(3, new ItemStack(Items.IRON_NUGGET));
		for (int slot = 4; slot < inventory.size(); slot++) {
			inventory.setStackInSlot(slot, new ItemStack(Items.STONE));
		}
		inventory.saveInventory();
		RecipeHelper.onRecipesUpdated(null);
		upgrades.setStackInSlot(2, new ItemStack(ModItems.ADVANCED_COMPACTING_UPGRADE.get()));
		upgrades.saveInventory();
		ItemStack remainder = inventory.insertItemOnlyToSlot(3, new ItemStack(Items.IRON_NUGGET, trigger));
		int actualNuggets = count(inventory, Items.IRON_NUGGET) - 1;
		int actualIngots = count(inventory, Items.IRON_INGOT);
		int actualBlocks = count(inventory, Items.IRON_BLOCK) - 1;
		boolean passed = remainder.isEmpty() && actualNuggets == nuggets && actualIngots == ingots && actualBlocks == blocks;
		inventory.saveInventory();
		player.getInventory().setItem(0, backpack);
		player.getInventory().setChanged();
		return new AdvancedCompactingHighStackRegressionResult(name, passed, first, second, trigger, nuggets, actualNuggets, ingots, actualIngots, blocks,
				actualBlocks, remainder.getCount(), passed ? null : "Unexpected compacting result");
	}

	private static String runDepositLimitedBarrelGuiStateRegression(String name, JsonObject request) {
		int depositCount = getInt(request, "depositCount", 64);
		int targetFreeSpace = getInt(request, "targetFreeSpace", 16);
		boolean locked = request.has("locked") ? request.get("locked").getAsBoolean() : true;
		boolean inventoryFilter = request.has("inventoryFilter") ? request.get("inventoryFilter").getAsBoolean() : true;
		List<DepositLimitedBarrelGuiStateResult> results = AutomationRuntime
				.runOnServer(player -> deposit(player, name, depositCount, targetFreeSpace, locked, inventoryFilter));
		boolean passed = results.stream().allMatch(DepositLimitedBarrelGuiStateResult::passed);
		StringBuilder json = new StringBuilder("{\"ok\":").append(passed).append(',').append(jsonProperty("name", name)).append(",\"depositCount\":")
				.append(depositCount).append(",\"targetFreeSpace\":").append(targetFreeSpace).append(",\"locked\":").append(locked)
				.append(",\"inventoryFilter\":").append(inventoryFilter).append(",\"results\":[");
		for (int i = 0; i < results.size(); i++) {
			DepositLimitedBarrelGuiStateResult result = results.get(i);
			if (i > 0) {
				json.append(',');
			}
			json.append('{').append(jsonProperty("scenario", result.scenario())).append(",\"passed\":").append(result.passed()).append(",\"backpackOpened\":")
					.append(result.backpackOpened()).append(",\"barrelOpened\":").append(result.barrelOpened()).append(",\"handled\":").append(result.handled())
					.append(",\"slotLimit\":").append(result.slotLimit()).append(",\"backpackBefore\":").append(result.backpackBefore())
					.append(",\"backpackAfter\":").append(result.backpackAfter()).append(",\"barrelBefore\":").append(result.barrelBefore())
					.append(",\"barrelAfter\":").append(result.barrelAfter()).append(",\"totalBefore\":").append(result.totalBefore())
					.append(",\"totalAfter\":").append(result.totalAfter()).append(',').append(jsonProperty("error", result.error())).append('}');
		}
		return json.append("]}").toString();
	}

	private static List<DepositLimitedBarrelGuiStateResult> deposit(ServerPlayer player, String name, int count, int free, boolean locked, boolean filter) {
		player.getInventory().clearContent();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos base = player.blockPosition().offset(3, 0, 0);
		List<DepositLimitedBarrelGuiStateResult> results = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			boolean backpackOpened = i == 1 || i == 3;
			boolean barrelOpened = i == 2 || i == 3;
			String scenario = switch (i) {
				case 0 -> "neitherOpened";
				case 1 -> "backpackOpenedOnly";
				case 2 -> "barrelOpenedOnly";
				default -> "bothOpened";
			};
			results.add(
					depositScenario(player, name + ":" + scenario, level, base.offset(i * 2, 0, 0), count, free, locked, filter, backpackOpened, barrelOpened));
		}
		return results;
	}

	private static DepositLimitedBarrelGuiStateResult depositScenario(ServerPlayer player, String name, ServerLevel level, BlockPos pos, int count, int free,
			boolean locked, boolean filter, boolean backpackOpened, boolean barrelOpened) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		place(level, player, pos, new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_1_ITEM.get()));
		StorageBlockEntity storage = level.getBlockEntity(pos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get())
				.map(be -> (StorageBlockEntity) be).orElseThrow(() -> new IllegalStateException("Missing limited barrel storage at " + pos));
		InventoryHandler barrel = storage.getStorageWrapper().getInventoryHandler();
		ItemStack diamonds = new ItemStack(Items.DIAMOND);
		int limit = barrel.getCapacityAsInt(0, ItemResource.of(diamonds));
		barrel.setStackInSlot(0, new ItemStack(Items.DIAMOND, Math.max(0, limit - free)));
		barrel.saveInventory();
		if (locked && !storage.isLocked()) {
			storage.toggleLock();
		}
		ItemStack backpack = createBackpack(9);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		ItemStack upgrade = new ItemStack(ModItems.DEPOSIT_UPGRADE.get());
		upgrade.set(ModDataComponents.FILTER_BY_INVENTORY, filter);
		wrapper.getUpgradeHandler().setStackInSlot(0, upgrade);
		wrapper.getUpgradeHandler().saveInventory();
		wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, count));
		wrapper.getInventoryHandler().saveInventory();
		player.getInventory().setItem(0, backpack);
		player.getInventory().setChanged();
		if (backpackOpened) {
			new BackpackContainer(0, player, new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0)).removed(player);
			backpack = player.getInventory().getItem(0);
		}
		if (barrelOpened) {
			new StorageContainerMenu(0, player, pos).removed(player);
		}
		int backpackBefore = count(BackpackWrapper.fromStack(backpack).getInventoryHandler(), Items.DIAMOND);
		int barrelBefore = count(barrel, Items.DIAMOND);
		boolean handled = InventoryInteractionHelper.tryInventoryInteraction(pos, level, backpack, Direction.NORTH, player);
		int backpackAfter = count(BackpackWrapper.fromStack(player.getInventory().getItem(0)).getInventoryHandler(), Items.DIAMOND);
		int barrelAfter = count(barrel, Items.DIAMOND);
		int totalBefore = backpackBefore + barrelBefore;
		int totalAfter = backpackAfter + barrelAfter;
		boolean passed = handled && totalBefore == totalAfter;
		return new DepositLimitedBarrelGuiStateResult(name, backpackOpened, barrelOpened, handled, limit, backpackBefore, backpackAfter, barrelBefore,
				barrelAfter, totalBefore, totalAfter, passed, passed ? null : "Deposit interaction did not preserve item count");
	}

	private static String runSubMobCatcherImmediateOpenRegression(String name) {
		try {
			resetState();
			Item parent = AutomationRuntime.runOnServer(BackpackGuiRegressionRun::setupParent);
			waitForInventory(0, parent, "mob catcher parent backpack");
			AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openParent);
			waitParent();
			AutomationRuntime.runOnServer(BackpackGuiRegressionRun::insertSub);
			SubMobCatcherRegressionState parentState = waitParentState();
			if (!parentState.parentMatches()) {
				return mobJson(name, false, parentState, parentState, "Parent backpack mob catcher data did not stay separate after inserting sub backpack");
			}
			AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openSub);
			waitSub();
			SubMobCatcherRegressionState subState = AutomationRuntime.runOnClient(BackpackGuiRegressionRun::subState);
			return mobJson(name, subState.subMatches(), parentState, subState,
					subState.subMatches() ? null : "Sub backpack did not open with its own mob catcher data");
		} catch (RuntimeException e) {
			SubMobCatcherRegressionState state = AutomationRuntime.runOnClient(BackpackGuiRegressionRun::safeState);
			return mobJson(name, false, state, state, e.getMessage());
		}
	}

	private static void resetState() {
		AutomationRuntime.runOnServer(player -> {
			player.containerMenu.setCarried(ItemStack.EMPTY);
			player.closeContainer();
			player.getInventory().setChanged();
			return true;
		});
		AutomationRuntime.runOnClient(() -> {
			if (Minecraft.getInstance().player != null) {
				Minecraft.getInstance().player.containerMenu.setCarried(ItemStack.EMPTY);
			}
			return true;
		});
		waitUntil(
				() -> Minecraft.getInstance().gui.screen() == null
						&& (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer)),
				"Timed out closing backpack screen");
		waitForServerBackpackMenuClose();
	}
	private static void waitForServerBackpackMenuClose() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> !(player.containerMenu instanceof BackpackContainer))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out closing backpack menu on server");
	}

	private static void prepare(BackpackGuiRegressionContext context) {
		switch (context) {
			case PLACED -> {
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::setupPlaced);
				AutomationRuntime.runOnClient(BackpackGuiRegressionRun::setupClientPlaced);
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openPlaced);
			}
			case CURIOS -> {
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::setupCurios);
				AutomationRuntime.runOnClient(BackpackGuiRegressionRun::setupClientCurios);
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openCurios);
			}
			case SUB -> {
				Item parent = AutomationRuntime.runOnServer(BackpackGuiRegressionRun::setupSub);
				waitForInventory(0, parent, "sub backpack parent");
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openParent);
				waitParent();
				AutomationRuntime.runOnServer(BackpackGuiRegressionRun::openSub);
			}
		}
	}

	private static Boolean setupPlaced(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos pos = position(player);
		level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING,
				player.getDirection().getOpposite()), 3);
		WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to place regression backpack block")).setBackpack(columnBackpack());
		return true;
	}
	private static Boolean setupClientPlaced() {
		if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
			throw new IllegalStateException("Client level/player is not available");
		}
		BlockPos pos = position(Minecraft.getInstance().player);
		Minecraft.getInstance().level.setBlock(pos, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.GOLD_BACKPACK.get().defaultBlockState()
				.setValue(BackpackBlock.FACING, Minecraft.getInstance().player.getDirection().getOpposite()), 3);
		WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Failed to create client regression backpack block")).setBackpack(columnBackpack());
		return true;
	}
	private static Boolean openPlaced(ServerPlayer player) {
		BackpackContext.Block context = new BackpackContext.Block(position(player));
		player.openMenu(new SimpleMenuProvider((id, inventory, openPlayer) -> new BackpackContainer(id, openPlayer, context),
				Component.literal("Placed Column Regression")), context::toBuffer);
		return true;
	}
	private static Boolean setupCurios(ServerPlayer player) {
		ItemStack backpack = columnBackpack();
		String id = curiosId(player, backpack);
		ensureCurios(player, id);
		setCurios(player, id, backpack);
		return true;
	}
	private static Boolean setupClientCurios() {
		if (Minecraft.getInstance().player == null) {
			throw new IllegalStateException("Client player is not available");
		}
		ItemStack backpack = columnBackpack();
		setCurios(Minecraft.getInstance().player, curiosId(Minecraft.getInstance().player, backpack), backpack);
		return true;
	}
	private static Boolean openCurios(ServerPlayer player) {
		String id = curiosId(player, columnBackpack());
		BackpackContext.Item context = new BackpackContext.Item(CompatModIds.CURIOS, id, 0);
		player.openMenu(new SimpleMenuProvider((window, inventory, openPlayer) -> new BackpackContainer(window, openPlayer, context),
				Component.literal("Curios Column Regression")), context::toBuffer);
		return true;
	}
	private static Item setupSub(ServerPlayer player) {
		ItemStack backpack = parentBackpack();
		player.getInventory().setItem(0, backpack);
		player.getInventory().setChanged();
		return backpack.getItem();
	}
	private static Item setupParent(ServerPlayer player) {
		ItemStack backpack = mobBackpack(81, 3, SUB_MOB_CATCHER_PARENT_MOB_ID, 0, "Parent Pig");
		player.getInventory().setItem(0, backpack);
		player.getInventory().setChanged();
		return backpack.getItem();
	}
	private static Boolean openParent(ServerPlayer player) {
		BackpackContext.Item context = new BackpackContext.Item(PlayerInventoryProvider.MAIN_INVENTORY, "", 0);
		player.openMenu(new SimpleMenuProvider((id, inventory, openPlayer) -> new BackpackContainer(id, openPlayer, context),
				Component.literal("Parent Backpack Regression")), context::toBuffer);
		return true;
	}
	private static Boolean openSub(ServerPlayer player) {
		BackpackContext.ItemSubBackpack context = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0, true);
		if (player.openMenu(new SimpleMenuProvider((id, inventory, openPlayer) -> new BackpackContainer(id, openPlayer, context),
				Component.literal("Sub Column Regression")), context::toBuffer).isEmpty()) {
			throw new IllegalStateException("Server refused to open sub backpack column regression menu");
		}
		return true;
	}
	private static Boolean insertSub(ServerPlayer player) {
		if (!(player.containerMenu instanceof BackpackContainer menu) || menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
			throw new IllegalStateException("Parent backpack menu is not open on server");
		}
		InventoryHandler inventory = menu.getStorageWrapper().getInventoryHandler();
		inventory.setStackInSlot(0, mobBackpack(144, 7, SUB_MOB_CATCHER_SUB_MOB_ID, 10, "Sub Cow"));
		inventory.saveInventory();
		menu.getStorageWrapper().onContentsUpdated();
		menu.broadcastChanges();
		return true;
	}

	private static void waitForOpenMenu(BackpackGuiRegressionContext context) {
		switch (context) {
			case PLACED -> waitPlaced();
			case CURIOS -> waitCurios();
			case SUB -> waitSub();
		}
	}
	private static PlacedColumnUpgradeClickExpectation clickWhenReady(BackpackGuiRegressionContext context) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		RuntimeException error = null;
		do {
			try {
				waitForOpenMenu(context);
				return AutomationRuntime.runOnClient(() -> click(context));
			} catch (RuntimeException e) {
				error = e;
				sleep(50);
			}
		} while (System.nanoTime() < deadline);
		throw error == null ? new IllegalStateException("Timed out waiting to click " + context.jsonName() + " backpack upgrade") : error;
	}
	private static PlacedColumnUpgradeClickExpectation click(BackpackGuiRegressionContext context) {
		return clickUpgrade(menu(context), context.jsonName());
	}
	private static PlacedColumnUpgradeState getState(BackpackGuiRegressionContext context) {
		BackpackContainer menu = menu(context);
		return new PlacedColumnUpgradeState(menu.getStorageWrapper().getColumnsTaken(), menu.getNumberOfStorageInventorySlots(),
				menu.getStorageWrapper().getInventoryHandler().size(), menu.upgradeSlots.get(1).getItem().isEmpty(), !menu.getCarried().isEmpty());
	}
	private static BackpackContainer menu(BackpackGuiRegressionContext context) {
		if (!(Minecraft.getInstance().gui.screen() instanceof BackpackScreen)
				|| !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu)) {
			throw new IllegalStateException(context.jsonName() + " backpack screen is not open");
		}
		if (context == BackpackGuiRegressionContext.PLACED && menu.getBlockPosition().isEmpty()) {
			throw new IllegalStateException("Open backpack is not a placed backpack");
		}
		if (context == BackpackGuiRegressionContext.CURIOS && menu.getBlockPosition().isPresent()) {
			throw new IllegalStateException("Open backpack is not a Curios/item backpack");
		}
		if (context == BackpackGuiRegressionContext.SUB && menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_SUB_BACKPACK) {
			throw new IllegalStateException("Open backpack is not an item sub backpack");
		}
		return menu;
	}
	private static PlacedColumnUpgradeClickExpectation clickUpgrade(BackpackContainer menu, String name) {
		if (menu.getNumberOfUpgradeSlots() < 2 || !menu.getCarried().isEmpty()) {
			throw new IllegalStateException(name + " backpack is not ready for column upgrade click");
		}
		Slot slot = menu.upgradeSlots.get(1);
		if (slot.getItem().isEmpty() || !(slot.getItem().getItem() instanceof IUpgradeItem<?> upgrade) || upgrade.getInventoryColumnsTaken() == 0) {
			throw new IllegalStateException(name + " backpack upgrade slot 1 must contain a column-taking upgrade");
		}
		int before = menu.getStorageWrapper().getColumnsTaken();
		int expectedColumns = before - upgrade.getInventoryColumnsTaken();
		int rows = menu.getStorageWrapper().getNumberOfSlotRows();
		int slots = menu.getStorageWrapper().getInventoryHandler().size();
		int base = slots / rows == (slots <= 81 ? 9 : 12) ? slots : slots + before * rows;
		clickSlot(Minecraft.getInstance().gui.screen(), slot);
		return new PlacedColumnUpgradeClickExpectation(expectedColumns, base - expectedColumns * rows);
	}
	private static void clickSlot(Screen screen, Slot slot) {
		int left = field(screen, "leftPos");
		int top = field(screen, "topPos");
		double x = left + slot.x + 8.0;
		double y = top + slot.y + 8.0;
		MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
		if (!screen.mouseClicked(event, false)) {
			throw new IllegalStateException("Backpack upgrade slot click was not handled");
		}
		screen.mouseReleased(event);
	}
	private static int field(Screen screen, String name) {
		for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.getInt(screen);
			} catch (NoSuchFieldException ignored) {
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Unable to read screen field " + name, e);
			}
		}
		throw new IllegalStateException("Unable to find screen field " + name);
	}

	private static ItemStack columnBackpack() {
		ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(81, 3);
		wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
		wrapper.getUpgradeHandler().saveInventory();
		wrapper.setColumnsTaken(2, false);
		wrapper.onContentsUpdated();
		return backpack;
	}
	private static ItemStack parentBackpack() {
		ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 81);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 3);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(81, 3);
		wrapper.getInventoryHandler().setStackInSlot(0, columnBackpack());
		wrapper.getInventoryHandler().saveInventory();
		wrapper.onContentsUpdated();
		return backpack;
	}
	private static ItemStack mobBackpack(int slots, int upgrades, UUID mob, int mobSlot, String name) {
		ItemStack backpack = new ItemStack(slots > 81 ? ModItems.DIAMOND_BACKPACK.get() : ModItems.GOLD_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, slots);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, upgrades);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(slots, upgrades);
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.MOB_CATCHER_UPGRADE.get()));
		wrapper.getUpgradeHandler().saveInventory();
		MobCatcherStorage.addCapturedMob(wrapper,
				new CapturedMob(mob, Identifier.parse("minecraft:pig"), new CompoundTag(), mobSlot, 1, 1, 1, false, name, 10, 10));
		wrapper.onContentsUpdated();
		return backpack;
	}
	private static ItemStack createBackpack(int slots) {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, slots);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
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
	private static BlockPos position(LivingEntity player) {
		return player.blockPosition().relative(player.getDirection(), 2);
	}
	private static void place(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
		BlockPos support = pos.below();
		level.setBlock(support, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
	}
	private static String curiosId(LivingEntity player, ItemStack backpack) {
		for (String id : curiosTypes(backpack, player).keySet()) {
			return id;
		}
		if (player instanceof Player inventoryPlayer) {
			PlayerInventoryHandler handler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
					.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
			for (String id : handler.getIdentifiers(inventoryPlayer)) {
				if (handler.getSlotCount(inventoryPlayer, id) > 0) {
					return id;
				}
			}
		}
		return "back";
	}
	@SuppressWarnings("unchecked")
	private static Map<String, ?> curiosTypes(ItemStack backpack, LivingEntity player) {
		try {
			Class<?> type = Class.forName("top.theillusivec4.curios.api.CuriosSlotTypes");
			return (Map<String, ?>) type.getMethod("getItemSlotTypes", ItemStack.class, LivingEntity.class).invoke(null, backpack, player);
		} catch (ReflectiveOperationException e) {
			return Map.of();
		}
	}
	private static void ensureCurios(ServerPlayer player, String id) {
		PlayerInventoryHandler handler = PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS)
				.orElseThrow(() -> new IllegalStateException("Curios inventory handler is not registered"));
		if (handler.getSlotCount(player, id) < 1) {
			MinecraftServer server = player.level().getServer();
			server.getCommands().performPrefixedCommand(
					server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput(),
					"curios add " + id + " " + player.getGameProfile().name() + " 1");
		}
		if (handler.getSlotCount(player, id) < 1) {
			throw new IllegalStateException("Unable to configure Curios slot " + id);
		}
	}
	private static void setCurios(LivingEntity player, String id, ItemStack backpack) {
		try {
			Class<?> type = Class.forName("top.theillusivec4.curios.api.CuriosApi");
			Optional<?> inventory = (Optional<?>) type.getMethod("getCuriosInventory", LivingEntity.class).invoke(null, player);
			Object curiosInventory = inventory.orElseThrow(() -> new IllegalStateException("Player has no Curios inventory"));
			curiosInventory.getClass().getMethod("setEquippedCurio", String.class, int.class, ItemStack.class).invoke(curiosInventory, id, 0, backpack);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to set Curios backpack stack", e);
		}
	}

	private static void waitPlaced() {
		waitUntil(
				() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBlockPosition().isPresent(),
				"Timed out waiting for placed backpack screen to open");
	}
	private static void waitCurios() {
		waitUntil(
				() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBlockPosition().isEmpty(),
				"Timed out waiting for curios backpack screen to open");
	}
	private static void waitParent() {
		waitUntil(
				() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK,
				"Timed out waiting for parent backpack screen to open");
	}
	private static void waitSub() {
		waitUntil(
				() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
						&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK,
				"Timed out waiting for sub backpack screen to open");
	}
	private static void waitForInventory(int slot, Item item, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		long matching = 0;
		do {
			boolean matches = AutomationRuntime
					.runOnClient(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getItem(slot).getItem() == item);
			if (matches) {
				if (matching == 0) {
					matching = System.nanoTime();
				} else if (System.nanoTime() - matching >= TimeUnit.MILLISECONDS.toNanos(250)) {
					return;
				}
			} else {
				matching = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for client inventory slot " + slot + " to contain " + description);
	}
	private static void waitUntil(BooleanSupplier condition, String error) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		long since = 0;
		do {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				if (since == 0) {
					since = System.nanoTime();
				} else if (System.nanoTime() - since >= TimeUnit.MILLISECONDS.toNanos(250)) {
					return;
				}
			} else {
				since = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(error);
	}
	private static SubMobCatcherRegressionState waitParentState() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		SubMobCatcherRegressionState state = AutomationRuntime.runOnClient(BackpackGuiRegressionRun::safeState);
		do {
			state = AutomationRuntime.runOnClient(BackpackGuiRegressionRun::safeState);
			if (state.parentMatches()) {
				return state;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		return state;
	}
	private static SubMobCatcherRegressionState subState() {
		BackpackContainer menu = menu(BackpackGuiRegressionContext.SUB);
		return mobState(menu, null);
	}
	private static SubMobCatcherRegressionState safeState() {
		if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu) {
			IBackpackWrapper nested = menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK ? nested(menu) : null;
			return mobState(menu, nested);
		}
		return new SubMobCatcherRegressionState("none", 0, false, 0, null, 0, null);
	}
	private static IBackpackWrapper nested(BackpackContainer menu) {
		ItemStack stack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
		if (!(stack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("Parent backpack slot 0 does not contain a backpack");
		}
		return BackpackWrapper.fromStack(stack);
	}
	private static SubMobCatcherRegressionState mobState(BackpackContainer menu, IBackpackWrapper nested) {
		List<CapturedMob> current = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
		List<CapturedMob> nestedMobs = nested == null ? List.of() : MobCatcherStorage.getCapturedMobs(nested);
		return new SubMobCatcherRegressionState(menu.getBackpackContext().getType().name(), menu.getStorageWrapper().getInventoryHandler().size(),
				nested != null, current.size(), current.isEmpty() ? null : current.get(0).id().toString(), nestedMobs.size(),
				nestedMobs.isEmpty() ? null : nestedMobs.get(0).id().toString());
	}

	private static String compactJson(AdvancedCompactingHighStackRegressionResult result) {
		return "{\"ok\":" + result.passed() + "," + jsonProperty("name", result.name()) + ",\"firstSlotCount\":" + result.firstSlotCount()
				+ ",\"secondSlotCount\":" + result.secondSlotCount() + ",\"triggerCount\":" + result.triggerCount() + ",\"expectedNuggets\":"
				+ result.expectedNuggets() + ",\"actualNuggets\":" + result.actualNuggets() + ",\"expectedIngots\":" + result.expectedIngots()
				+ ",\"actualIngots\":" + result.actualIngots() + ",\"expectedBlocks\":" + result.expectedBlocks() + ",\"actualBlocks\":" + result.actualBlocks()
				+ ",\"insertRemainder\":" + result.insertRemainder() + "," + jsonProperty("error", result.error()) + "}";
	}
	private static String columnJson(String name, boolean ok, PlacedColumnUpgradeClickExpectation expected, PlacedColumnUpgradeState state, String error) {
		return "{\"ok\":" + ok + "," + jsonProperty("name", name) + ",\"expectedColumnsTaken\":" + expected.expectedColumnsTaken() + ",\"actualColumnsTaken\":"
				+ state.columnsTaken() + ",\"expectedStorageSlots\":" + expected.expectedStorageSlots() + ",\"actualStorageSlots\":" + state.storageSlots()
				+ ",\"actualInventoryHandlerSlots\":" + state.inventoryHandlerSlots() + ",\"upgradeSlotEmpty\":" + state.upgradeSlotEmpty()
				+ ",\"carriedNotEmpty\":" + state.carriedNotEmpty() + "," + jsonProperty("error", error) + "}";
	}
	private static String mobJson(String name, boolean ok, SubMobCatcherRegressionState parent, SubMobCatcherRegressionState sub, String error) {
		return "{\"ok\":" + ok + "," + jsonProperty("name", name) + "," + jsonProperty("parentContext", parent.context()) + ",\"parentStorageSlots\":"
				+ parent.storageSlots() + ",\"parentSlot0Backpack\":" + parent.slot0Backpack() + ",\"parentMobCount\":" + parent.currentMobCount() + ","
				+ jsonProperty("parentMobId", parent.currentMobId()) + ",\"parentNestedMobCount\":" + parent.nestedMobCount() + ","
				+ jsonProperty("parentNestedMobId", parent.nestedMobId()) + "," + jsonProperty("subContext", sub.context()) + ",\"subStorageSlots\":"
				+ sub.storageSlots() + ",\"subMobCount\":" + sub.currentMobCount() + "," + jsonProperty("subMobId", sub.currentMobId()) + ","
				+ jsonProperty("error", error) + "}";
	}
	private static int getInt(JsonObject request, String property, int fallback) {
		return request.has(property) ? request.get(property).getAsInt() : fallback;
	}
	private static void requirePost(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			byte[] response = "{\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(405, response.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(response);
			}
			throw new IllegalStateException("Method not allowed");
		}
	}
	private static void sendJson(HttpExchange exchange, String json) throws IOException {
		byte[] response = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(200, response.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(response);
		}
	}
	private static String jsonProperty(String name, String value) {
		return "\"" + name + "\":"
				+ (value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"");
	}
	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record PlacedColumnUpgradeClickExpectation(int expectedColumnsTaken, int expectedStorageSlots) {
	}
	private record PlacedColumnUpgradeState(int columnsTaken, int storageSlots, int inventoryHandlerSlots, boolean upgradeSlotEmpty, boolean carriedNotEmpty) {
		private boolean matches(PlacedColumnUpgradeClickExpectation expected) {
			return columnsTaken == expected.expectedColumnsTaken() && storageSlots == expected.expectedStorageSlots()
					&& inventoryHandlerSlots == expected.expectedStorageSlots() && upgradeSlotEmpty && carriedNotEmpty;
		}
	}
	private record AdvancedCompactingHighStackRegressionResult(String name, boolean passed, int firstSlotCount, int secondSlotCount, int triggerCount,
			int expectedNuggets, int actualNuggets, int expectedIngots, int actualIngots, int expectedBlocks, int actualBlocks, int insertRemainder,
			String error) {
	}
	private record DepositLimitedBarrelGuiStateResult(String scenario, boolean backpackOpened, boolean barrelOpened, boolean handled, int slotLimit,
			int backpackBefore, int backpackAfter, int barrelBefore, int barrelAfter, int totalBefore, int totalAfter, boolean passed, String error) {
	}
	private record SubMobCatcherRegressionState(String context, int storageSlots, boolean slot0Backpack, int currentMobCount, String currentMobId,
			int nestedMobCount, String nestedMobId) {
		private boolean parentMatches() {
			return BackpackContext.ContextType.ITEM_BACKPACK.name().equals(context) && slot0Backpack && storageSlots == 81 && currentMobCount == 1
					&& SUB_MOB_CATCHER_PARENT_MOB_ID.toString().equals(currentMobId);
		}
		private boolean subMatches() {
			return BackpackContext.ContextType.ITEM_SUB_BACKPACK.name().equals(context) && storageSlots == 144 && currentMobCount == 1
					&& SUB_MOB_CATCHER_SUB_MOB_ID.toString().equals(currentMobId);
		}
	}
	private enum BackpackGuiRegressionContext {
		PLACED("placed"), CURIOS("curios"), SUB("sub");
		private final String jsonName;
		BackpackGuiRegressionContext(String jsonName) {
			this.jsonName = jsonName;
		}
		private String jsonName() {
			return jsonName;
		}
		private static BackpackGuiRegressionContext fromName(String name) {
			for (BackpackGuiRegressionContext context : values()) {
				if (context.jsonName.equals(name)) {
					return context;
				}
			}
			throw new IllegalArgumentException("Unknown backpack GUI regression context " + name);
		}
	}
}
