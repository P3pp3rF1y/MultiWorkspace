package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class BackpackAccessRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);

	private BackpackAccessRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, BackpackAccessRegression::run);
	}

	public static void handleCurios(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, BackpackAccessRegression::runCurios);
	}

	private static String run() {
		boolean heldOpened = false;
		boolean heldDataPreserved = false;
		boolean nestedOpened = false;
		boolean nestedDataPreserved = false;
		String error = null;
		try {
			resetGuiState();
			AccessFixture fixture = AutomationRuntime.runOnServer(BackpackAccessRegression::setup);
			waitForClientPlayerInventorySlot(0, fixture.parent().backpack().getItem(), "held regression backpack");

			heldOpened = AutomationRuntime.runOnServer(player -> {
				openMainBackpack(player);
				return player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK;
			});
			waitForOpenParentBackpackMenu();
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedBackpackMenu();
			heldDataPreserved = AutomationRuntime.runOnServer(player -> dataError(player.getInventory().getItem(0), fixture, false) == null);

			AutomationRuntime.runOnServer(player -> {
				openMainBackpack(player);
				return true;
			});
			waitForOpenParentBackpackMenu();
			nestedOpened = AutomationRuntime.runOnServer(player -> {
				openNestedBackpack(player, 0);
				return player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK;
			});
			waitForOpenSubBackpackMenu();
			AutomationRuntime.runOnServer(player -> {
				BackpackContainer menu = (BackpackContainer) player.containerMenu;
				menu.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.EMERALD, 3));
				menu.getStorageWrapper().getInventoryHandler().saveInventory();
				menu.getStorageWrapper().onContentsUpdated();
				menu.broadcastChanges();
				player.closeContainer();
				return true;
			});
			waitForClosedBackpackMenu();
			nestedDataPreserved = AutomationRuntime.runOnServer(player -> dataError(player.getInventory().getItem(0), fixture, true) == null);
			if (!heldOpened || !heldDataPreserved || !nestedOpened || !nestedDataPreserved) {
				error = "Backpack access path did not retain its configured state";
			}
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				return true;
			});
		}

		boolean passed = heldOpened && heldDataPreserved && nestedOpened && nestedDataPreserved;
		return "{\"ok\":" + passed + ",\"heldOpened\":" + heldOpened + ",\"heldDataPreserved\":" + heldDataPreserved + ",\"nestedOpened\":" + nestedOpened
				+ ",\"nestedDataPreserved\":" + nestedDataPreserved + "," + jsonProperty("error", error) + "}";
	}

	private static String runCurios() {
		boolean available = false;
		boolean opened = false;
		boolean dataPreserved = false;
		String curioId = null;
		String error = null;
		try {
			resetGuiState();
			CuriosFixture fixture = AutomationRuntime.runOnServer(BackpackAccessRegression::setupCurios);
			if (fixture == null) {
				return "{\"ok\":true,\"skipped\":true,\"available\":false}";
			}
			available = true;
			curioId = fixture.id();
			String configuredCurioId = curioId;
			AutomationRuntime.runOnClient(() -> setupClientCurios(configuredCurioId, fixture.backpack().backpack().copy()));
			opened = AutomationRuntime.runOnServer(player -> openCurios(player, configuredCurioId, fixture.backpack().storageUuid()));
			waitForOpenCuriosBackpackMenu();
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedBackpackMenu();
			dataPreserved = AutomationRuntime.runOnServer(player -> {
				ItemStack backpack = getCurios(player, configuredCurioId);
				return BackpackRegressionFixture.dataError(backpack, (BackpackItem) fixture.backpack().backpack().getItem(), fixture.backpack()) == null;
			});
			if (!opened || !dataPreserved) {
				error = "Curios backpack access path did not retain its configured state";
			}
		} catch (RuntimeException e) {
			error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		} finally {
			String cleanupCurioId = curioId;
			if (cleanupCurioId != null) {
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					setCurios(player, cleanupCurioId, ItemStack.EMPTY);
					return true;
				});
				AutomationRuntime.runOnClient(() -> {
					if (Minecraft.getInstance().player != null) {
						setCurios(Minecraft.getInstance().player, cleanupCurioId, ItemStack.EMPTY);
					}
					return true;
				});
			}
		}

		boolean passed = available && opened && dataPreserved;
		return "{\"ok\":" + passed + ",\"skipped\":false,\"available\":" + available + ",\"opened\":" + opened + ",\"dataPreserved\":" + dataPreserved + ","
				+ jsonProperty("error", error) + "}";
	}

	private static CuriosFixture setupCurios(ServerPlayer player) {
		if (PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS).isEmpty()) {
			return null;
		}
		player.closeContainer();
		BackpackRegressionFixture.Fixture backpack = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x3978B5, 0xF29D38);
		String id = curiosId(player, backpack.backpack());
		ensureCurios(player, id);
		setCurios(player, id, backpack.backpack());
		return new CuriosFixture(id, backpack);
	}

	private static Boolean setupClientCurios(String id, ItemStack backpack) {
		if (Minecraft.getInstance().player == null) {
			throw new IllegalStateException("Client player is not available");
		}
		setCurios(Minecraft.getInstance().player, id, backpack);
		return true;
	}

	private static boolean openCurios(ServerPlayer player, String id, UUID storageUuid) {
		BackpackContext.Item context = new BackpackContext.Item(CompatModIds.CURIOS, id, 0);
		ItemStack backpack = getCurios(player, id);
		if (!storageUuid.equals(backpack.get(ModCoreDataComponents.STORAGE_UUID))) {
			return false;
		}
		return player.openMenu(new SophisticatedMenuProvider((window, inventory, openPlayer) -> new BackpackContainer(window, openPlayer, context),
				context.getDisplayName(player), false), context::toBuffer).isPresent();
	}

	private static ItemStack getCurios(Player player, String id) {
		return PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS).map(handler -> handler.getStackInSlot(player, id, 0))
				.orElse(ItemStack.EMPTY);
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

	private static AccessFixture setup(ServerPlayer player) {
		player.closeContainer();
		player.getInventory().clearContent();
		BackpackRegressionFixture.Fixture parent = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x225588, 0xE2A100);
		BackpackRegressionFixture.Fixture nested = BackpackRegressionFixture.create(ModItems.GOLD_BACKPACK.get(), 0x8B3F00, 0x3A8FB7);
		IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parent.backpack());
		parentWrapper.getInventoryHandler().setStackInSlot(0, nested.backpack());
		parentWrapper.getInventoryHandler().saveInventory();
		parentWrapper.onContentsUpdated();
		player.getInventory().setItem(0, parent.backpack());
		player.getInventory().setSelectedSlot(0);
		player.getInventory().setChanged();
		return new AccessFixture(parent, nested);
	}

	private static String dataError(ItemStack parentBackpack, AccessFixture fixture, boolean requireNestedMutation) {
		BackpackRegressionFixture.Fixture parentFixture = fixture.parent();
		if (!parentBackpack.is(parentFixture.backpack().getItem())) {
			return "held backpack item changed";
		}
		if (!parentFixture.storageUuid().equals(parentBackpack.get(ModCoreDataComponents.STORAGE_UUID))) {
			return "held backpack UUID changed";
		}
		if (BackpackItem.getMainColor(parentBackpack) != parentFixture.mainColor()
				|| BackpackItem.getAccentColor(parentBackpack) != parentFixture.accentColor()) {
			return "held backpack colors changed";
		}
		if (!Integer.valueOf(1).equals(parentBackpack.get(ModCoreDataComponents.OPEN_TAB_ID))) {
			return "held backpack custom component changed";
		}
		IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parentBackpack);
		if (!parentWrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_STARTER_TIER.get())) {
			return "held backpack upgrade changed";
		}
		ItemStack nestedBackpack = parentWrapper.getInventoryHandler().getStackInSlot(0);
		if (!nestedBackpack.is(fixture.nested().backpack().getItem())) {
			return "nested backpack item changed";
		}
		String nestedError = BackpackRegressionFixture.dataError(nestedBackpack, (BackpackItem) fixture.nested().backpack().getItem(), fixture.nested());
		if (nestedError != null) {
			return "nested backpack " + nestedError;
		}
		if (!requireNestedMutation) {
			return null;
		}
		ItemStack insertedContents = BackpackWrapper.fromStack(nestedBackpack).getInventoryHandler().getStackInSlot(1);
		return insertedContents.is(Items.EMERALD) && insertedContents.getCount() == 3 ? null : "nested backpack mutation was not retained";
	}

	private static void resetGuiState() {
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
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		long closedSince = 0;
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() == null
					&& (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer)))) {
				if (closedSince == 0) {
					closedSince = System.nanoTime();
				} else if (System.nanoTime() - closedSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
					return;
				}
			} else {
				closedSince = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out resetting backpack GUI state; " + AutomationRuntime.runOnClient(BackpackAccessRegression::clientOpenState));
	}

	private static void waitForClientPlayerInventorySlot(int slot, Item item, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		long matchingSince = 0;
		do {
			if (AutomationRuntime.runOnClient(() -> {
				if (Minecraft.getInstance().player == null) {
					return false;
				}
				ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
				return !stack.isEmpty() && stack.getItem() == item;
			})) {
				if (matchingSince == 0) {
					matchingSince = System.nanoTime();
				} else if (System.nanoTime() - matchingSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
					return;
				}
			} else {
				matchingSince = 0;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);

		throw new IllegalStateException("Timed out waiting for client inventory slot " + slot + " to contain " + description + "; "
				+ AutomationRuntime.runOnClient(BackpackAccessRegression::clientOpenState));
	}

	private static void waitForOpenParentBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for parent backpack screen to open");
	}

	private static void waitForOpenSubBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for sub backpack screen to open; " + AutomationRuntime.runOnClient(BackpackAccessRegression::clientOpenState));
	}

	private static void waitForOpenCuriosBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() instanceof BackpackScreen && Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK && menu.getBlockPosition().isEmpty())) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for Curios backpack screen to open; " + AutomationRuntime.runOnClient(BackpackAccessRegression::clientOpenState));
	}

	private static void waitForClosedBackpackMenu() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().gui.screen() == null && Minecraft.getInstance().player != null
					&& !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for backpack screen to close");
	}

	private static void openMainBackpack(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem backpackItem)) {
			throw new IllegalStateException("No parent backpack in player inventory slot 0");
		}
		player.getInventory().setSelectedSlot(0);
		backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	private static void openNestedBackpack(ServerPlayer player, int nestedSlot) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No parent backpack in player inventory slot 0");
		}
		ItemStack nestedBackpack = BackpackWrapper.fromStack(mainBackpack).getInventoryHandler().getStackInSlot(nestedSlot);
		if (!(nestedBackpack.getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("No nested backpack in parent backpack slot " + nestedSlot);
		}
		BackpackContext context = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, nestedSlot, true);
		player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
				Component.literal("Nested automation backpack")), context::toBuffer);
	}

	private static String clientOpenState() {
		Minecraft minecraft = Minecraft.getInstance();
		String screen = minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getSimpleName();
		String menu = minecraft.player == null ? "none" : minecraft.player.containerMenu.getClass().getSimpleName();
		String context = "none";
		int storageSlots = 0;
		if (minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer backpackContainer) {
			context = backpackContainer.getBackpackContext().getType().name();
			storageSlots = backpackContainer.getNumberOfStorageInventorySlots();
		}
		String slot0Item = "none";
		String nestedSlot0Item = "none";
		if (minecraft.player != null) {
			ItemStack slot0 = minecraft.player.getInventory().getItem(0);
			slot0Item = BuiltInRegistries.ITEM.getKey(slot0.getItem()).toString();
			if (slot0.getItem() instanceof BackpackItem) {
				ItemStack nested = BackpackWrapper.fromStack(slot0).getInventoryHandler().getStackInSlot(0);
				nestedSlot0Item = BuiltInRegistries.ITEM.getKey(nested.getItem()).toString();
			}
		}
		return "screen=" + screen + ", menu=" + menu + ", context=" + context + ", storageSlots=" + storageSlots + ", slot0=" + slot0Item + ", nestedSlot0="
				+ nestedSlot0Item;
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

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record AccessFixture(BackpackRegressionFixture.Fixture parent, BackpackRegressionFixture.Fixture nested) {
	}

	private record CuriosFixture(String id, BackpackRegressionFixture.Fixture backpack) {
	}
}
