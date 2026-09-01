package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class BackpackAccessRegression {
	private BackpackAccessRegression() {
	}

	static void handle(HttpExchange exchange) throws IOException {
		handle(exchange, false);
	}

	static void handleCurios(HttpExchange exchange) throws IOException {
		handle(exchange, true);
	}

	private static void handle(HttpExchange exchange, boolean curios) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			send(exchange, "{\"ok\":false,\"error\":\"Method not allowed\"}");
			return;
		}
		try {
			send(exchange, curios ? runCurios() : run());
		} catch (RuntimeException e) {
			send(exchange, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
		}
	}

	private static String run() {
		try {
			resetGui();
			Fixture fixture = AutomationRuntime.runOnServer(BackpackAccessRegression::setup);
			waitForClientBackpack();
			boolean heldOpened = AutomationRuntime.runOnServer(player -> {
				openMain(player);
				return player.containerMenu instanceof BackpackContainer;
			});
			waitForMenu(BackpackContext.ContextType.ITEM_BACKPACK);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedMenu();
			String heldError = AutomationRuntime.runOnServer(player -> dataError(player.getInventory().getItem(0), fixture, false));
			boolean heldPreserved = heldError == null;

			AutomationRuntime.runOnServer(player -> {
				openMain(player);
				return true;
			});
			waitForMenu(BackpackContext.ContextType.ITEM_BACKPACK);
			boolean nestedOpened = AutomationRuntime.runOnServer(player -> {
				BackpackContext context = new BackpackContext.ItemSubBackpack(PlayerInventoryProvider.MAIN_INVENTORY, "", 0, false, 0, true);
				return player.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> new BackpackContainer(id, menuPlayer, context),
						Component.literal("Nested automation backpack")), context::toBuffer).isPresent();
			});
			waitForMenu(BackpackContext.ContextType.ITEM_SUB_BACKPACK);
			AutomationRuntime.runOnServer(player -> {
				BackpackContainer menu = (BackpackContainer) player.containerMenu;
				menu.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.EMERALD, 3));
				menu.getStorageWrapper().getInventoryHandler().saveInventory();
				menu.getStorageWrapper().onContentsUpdated();
				menu.broadcastChanges();
				player.closeContainer();
				return true;
			});
			waitForClosedMenu();
			String nestedError = AutomationRuntime.runOnServer(player -> dataError(player.getInventory().getItem(0), fixture, true));
			boolean nestedPreserved = nestedError == null;
			return "{\"ok\":" + (heldOpened && heldPreserved && nestedOpened && nestedPreserved) + ",\"heldOpened\":" + heldOpened + ",\"heldDataPreserved\":"
					+ heldPreserved + ",\"heldError\":" + quote(heldError) + ",\"nestedOpened\":" + nestedOpened + ",\"nestedDataPreserved\":" + nestedPreserved
					+ ",\"nestedError\":" + quote(nestedError) + "}";
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				return true;
			});
		}
	}

	private static String runCurios() {
		if (PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS).isEmpty()) {
			return "{\"ok\":true,\"skipped\":true,\"available\":false}";
		}
		String id = null;
		try {
			resetGui();
			CuriosFixture fixture = AutomationRuntime.runOnServer(BackpackAccessRegression::setupCurios);
			id = fixture.id();
			String curioId = id;
			AutomationRuntime.runOnClient(() -> {
				setCurios(Minecraft.getInstance().player, curioId, fixture.fixture().backpack());
				return true;
			});
			boolean opened = AutomationRuntime.runOnServer(player -> openCurios(player, curioId, fixture.fixture().storageUuid()));
			waitForMenu(BackpackContext.ContextType.ITEM_BACKPACK);
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				return true;
			});
			waitForClosedMenu();
			boolean preserved = AutomationRuntime.runOnServer(player -> BackpackRegressionFixture.dataError(getCurios(player, curioId),
					(BackpackItem) fixture.fixture().backpack().getItem(), fixture.fixture()) == null);
			return "{\"ok\":" + (opened && preserved) + ",\"skipped\":false,\"available\":true,\"opened\":" + opened + ",\"dataPreserved\":" + preserved + "}";
		} finally {
			if (id != null) {
				String curioId = id;
				AutomationRuntime.runOnServer(player -> {
					player.closeContainer();
					setCurios(player, curioId, ItemStack.EMPTY);
					return true;
				});
				AutomationRuntime.runOnClient(() -> {
					if (Minecraft.getInstance().player != null) {
						setCurios(Minecraft.getInstance().player, curioId, ItemStack.EMPTY);
					}
					return true;
				});
			}
		}
	}

	private static Fixture setup(ServerPlayer player) {
		BackpackRegressionFixture.Fixture parent = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x225588, 0xE2A100);
		BackpackRegressionFixture.Fixture nested = BackpackRegressionFixture.create(ModItems.GOLD_BACKPACK.get(), 0x8B3F00, 0x3A8FB7);
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(parent.backpack());
		wrapper.getInventoryHandler().setStackInSlot(0, nested.backpack());
		wrapper.getInventoryHandler().saveInventory();
		wrapper.onContentsUpdated();
		player.getInventory().clearContent();
		player.getInventory().setItem(0, parent.backpack());
		player.getInventory().setSelectedSlot(0);
		return new Fixture(parent, nested);
	}

	private static CuriosFixture setupCurios(ServerPlayer player) {
		BackpackRegressionFixture.Fixture fixture = BackpackRegressionFixture.create(ModItems.DIAMOND_BACKPACK.get(), 0x3978B5, 0xF29D38);
		String id = curiosId(player, fixture.backpack());
		ensureCurios(player, id);
		setCurios(player, id, fixture.backpack());
		return new CuriosFixture(id, fixture);
	}

	private static boolean openCurios(ServerPlayer player, String id, UUID storageUuid) {
		ItemStack backpack = getCurios(player, id);
		if (!storageUuid.equals(backpack.get(ModCoreDataComponents.STORAGE_UUID))) {
			return false;
		}
		BackpackContext.Item context = new BackpackContext.Item(CompatModIds.CURIOS, id, 0);
		return player.openMenu(new SophisticatedMenuProvider((window, inventory, openPlayer) -> new BackpackContainer(window, openPlayer, context),
				context.getDisplayName(player), false), context::toBuffer).isPresent();
	}

	private static void openMain(ServerPlayer player) {
		ItemStack backpack = player.getInventory().getItem(0);
		if (!(backpack.getItem() instanceof BackpackItem backpackItem)) {
			throw new IllegalStateException("No parent backpack in player inventory slot 0");
		}
		player.getInventory().setSelectedSlot(0);
		backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	private static String dataError(ItemStack parent, Fixture fixture, boolean requireNestedMutation) {
		BackpackRegressionFixture.Fixture parentFixture = fixture.parent();
		if (!parent.is(parentFixture.backpack().getItem())) {
			return "held backpack item changed";
		}
		if (!parentFixture.storageUuid().equals(parent.get(ModCoreDataComponents.STORAGE_UUID))) {
			return "held backpack UUID changed";
		}
		if (BackpackItem.getMainColor(parent) != parentFixture.mainColor() || BackpackItem.getAccentColor(parent) != parentFixture.accentColor()) {
			return "held backpack colors changed";
		}
		if (!Integer.valueOf(1).equals(parent.get(ModCoreDataComponents.OPEN_TAB_ID))) {
			return "held backpack custom component changed";
		}
		IBackpackWrapper parentWrapper = BackpackWrapper.fromStack(parent);
		if (!parentWrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_STARTER_TIER.get())) {
			return "held backpack upgrade changed";
		}
		ItemStack nested = parentWrapper.getInventoryHandler().getStackInSlot(0);
		if (!nested.is(fixture.nested().backpack().getItem())) {
			return "nested backpack item changed";
		}
		String error = BackpackRegressionFixture.dataError(nested, (BackpackItem) fixture.nested().backpack().getItem(), fixture.nested());
		if (error != null || !requireNestedMutation) {
			return error == null ? null : "nested backpack " + error;
		}
		ItemStack mutation = BackpackWrapper.fromStack(nested).getInventoryHandler().getStackInSlot(1);
		return mutation.is(Items.EMERALD) && mutation.getCount() == 3 ? null : "nested backpack mutation was not retained";
	}

	private static void resetGui() {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return true;
		});
		waitForClosedMenu();
	}

	private static void waitForClientBackpack() {
		waitFor(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().getItem(0).getItem() instanceof BackpackItem,
				"client backpack inventory sync");
	}

	private static void waitForMenu(BackpackContext.ContextType context) {
		waitFor(() -> Minecraft.getInstance().screen instanceof BackpackScreen && Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.containerMenu instanceof BackpackContainer menu && menu.getBackpackContext().getType() == context,
				"backpack menu");
	}

	private static void waitForClosedMenu() {
		waitFor(() -> Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null
				&& !(Minecraft.getInstance().player.containerMenu instanceof BackpackContainer), "backpack menu to close");
	}

	private static void waitFor(java.util.function.BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				return;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static ItemStack getCurios(Player player, String id) {
		return PlayerInventoryProvider.get().getPlayerInventoryHandler(CompatModIds.CURIOS).map(handler -> handler.getStackInSlot(player, id, 0))
				.orElse(ItemStack.EMPTY);
	}

	private static String curiosId(LivingEntity player, ItemStack backpack) {
		for (String id : curiosTypes(backpack, player).keySet()) {
			return id;
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
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
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
			inventory.orElseThrow(() -> new IllegalStateException("Player has no Curios inventory")).getClass()
					.getMethod("setEquippedCurio", String.class, int.class, ItemStack.class).invoke(inventory.get(), id, 0, backpack);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to set Curios backpack stack", e);
		}
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

	private record Fixture(BackpackRegressionFixture.Fixture parent, BackpackRegressionFixture.Fixture nested) {
	}

	private record CuriosFixture(String id, BackpackRegressionFixture.Fixture fixture) {
	}
}
