package net.p3pp3rf1y.devclientautomation.scenarios.core;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.devclientautomation.bridge.EndpointRegistry;
import net.p3pp3rf1y.devclientautomation.demo.DemoMouseMotion;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.bool;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.integer;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.jsonProperty;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.longValue;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.readObject;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJson;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.string;

public final class ClientControlEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger("devclientautomation");

	private ClientControlEndpoints() {
	}

	public static void register(EndpointRegistry endpoints) {
		endpoints.register("/state", ClientControlEndpoints::state);
		endpoints.register("/screen", ClientControlEndpoints::screen);
		endpoints.register("/click-widget", ClientControlEndpoints::clickWidget);
		endpoints.register("/key", ClientControlEndpoints::key);
		endpoints.register("/mouse/move", ClientControlEndpoints::moveMouse);
		endpoints.register("/mouse/click", ClientControlEndpoints::clickMouse);
		endpoints.register("/command", ClientControlEndpoints::command);
		endpoints.register("/screen/move-to-slot", ClientControlEndpoints::moveToSlot);
		endpoints.register("/screen/throw-slot", ClientControlEndpoints::throwSlot);
		endpoints.register("/window/maximize", ClientControlEndpoints::maximizeWindow);
		endpoints.register("/wait", ClientControlEndpoints::waitFor);
		endpoints.register("/client/shutdown-world", ClientControlEndpoints::shutdownWorld);
		endpoints.register("/client/stop", ClientControlEndpoints::stopClient);
		endpoints.register("/screenshot", ClientControlEndpoints::screenshot);
	}

	private static void state(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnClient(ClientControlEndpoints::buildStateJson));
	}

	private static void screen(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnClient(ClientControlEndpoints::buildScreenJson));
	}

	private static void clickWidget(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String text = string(request, "text", "");
		boolean contains = bool(request, "contains", false);
		int button = integer(request, "button", 0);
		int index = integer(request, "index", -1);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> clickWidget(text, contains, button, index)));
	}

	private static void key(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String key = string(request, "key", "");
		boolean ctrl = bool(request, "ctrl", false);
		boolean shift = bool(request, "shift", false);
		boolean alt = bool(request, "alt", false);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> pressKey(key, ctrl, shift, alt)));
	}

	private static void moveMouse(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String position = string(request, "position", "");
		int x = integer(request, "x", -1);
		int y = integer(request, "y", -1);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> moveMouse(position, x, y)));
	}

	private static void clickMouse(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int x = integer(request, "x", -1);
		int y = integer(request, "y", -1);
		int button = integer(request, "button", GLFW.GLFW_MOUSE_BUTTON_LEFT);
		boolean shift = bool(request, "shift", false);
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnClient(() -> clickMouse(x, y, button, shift)));
	}

	private static void command(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		String command = string(readObject(exchange), "command", "");
		sendJsonHandling(exchange, LOGGER, () -> AutomationRuntime.runOnServer(player -> runCommand(player, command)));
	}

	private static void moveToSlot(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		int menuSlot = integer(readObject(exchange), "menuSlot", -1);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> moveToSlot(menuSlot)));
	}

	private static void throwSlot(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		int menuSlot = integer(request, "menuSlot", -1);
		boolean fullStack = bool(request, "fullStack", true);
		sendJson(exchange, AutomationRuntime.runOnClient(() -> throwSlot(menuSlot, fullStack)));
	}

	private static void maximizeWindow(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJson(exchange, AutomationRuntime.runOnClient(ClientControlEndpoints::maximizeWindow));
	}

	private static void waitFor(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		JsonObject request = readObject(exchange);
		String condition = string(request, "condition", "");
		String screen = string(request, "screen", "");
		long timeoutMs = longValue(request, "timeoutMs", 60_000L);

		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		BooleanSupplier conditionChecker = () -> AutomationRuntime.runOnClient(() -> matchesCondition(condition, screen));
		while (System.nanoTime() < deadline) {
			if (conditionChecker.getAsBoolean()) {
				sendJson(exchange, "{\"ok\":true,\"timedOut\":false}");
				return;
			}
			sleep(100);
		}
		sendJson(exchange, "{\"ok\":false,\"timedOut\":true}");
	}

	private static void stopClient(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJson(exchange, "{\"ok\":true,\"stopping\":true}");
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
	}

	private static void shutdownWorld(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, () -> {
			MinecraftServer server = AutomationRuntime.runOnClient(Minecraft.getInstance()::getSingleplayerServer);
			if (server == null) {
				throw new IllegalStateException("Singleplayer server is not loaded");
			}
			server.halt(true);
			return "{\"ok\":true,\"shutdown\":true}";
		});
	}

	private static void screenshot(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "GET");
		CompletableFuture<byte[]> screenshotBytes = new CompletableFuture<>();
		Minecraft.getInstance().execute(() -> Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), image -> {
			Path screenshot = null;
			try (image) {
				screenshot = Files.createTempFile("devclientautomation-", ".png");
				image.writeToFile(screenshot);
				screenshotBytes.complete(Files.readAllBytes(screenshot));
			} catch (IOException e) {
				screenshotBytes.completeExceptionally(e);
			} finally {
				if (screenshot != null) {
					try {
						Files.deleteIfExists(screenshot);
					} catch (IOException ignored) {
					}
				}
			}
		}));
		byte[] bytes;
		try {
			bytes = screenshotBytes.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while capturing screenshot", e);
		} catch (ExecutionException | java.util.concurrent.TimeoutException e) {
			throw new IOException("Failed to capture screenshot", e);
		}
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private static String buildStateJson() {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;
		return "{" + jsonProperty("screenClass", screen == null ? null : screen.getClass().getName()) + ","
				+ jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName()) + ","
				+ jsonProperty("screenTitle", screen == null ? null : screen.getTitle().getString()) + ","
				+ jsonProperty("gameDirectory", minecraft.gameDirectory.getAbsolutePath()) + "," + "\"inWorld\":" + (minecraft.level != null) + ","
				+ "\"playerLoaded\":" + (minecraft.player != null) + "," + "\"windowWidth\":" + minecraft.getWindow().getWidth() + ",\"windowHeight\":"
				+ minecraft.getWindow().getHeight() + ",\"guiWidth\":" + minecraft.getWindow().getGuiScaledWidth() + ",\"guiHeight\":"
				+ minecraft.getWindow().getGuiScaledHeight() + "}";
	}

	private static String buildScreenJson() {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;
		StringBuilder json = new StringBuilder("{");
		json.append(jsonProperty("screenClass", screen == null ? null : screen.getClass().getName())).append(',');
		json.append(jsonProperty("screenSimpleName", screen == null ? null : screen.getClass().getSimpleName())).append(',');
		json.append(jsonProperty("title", screen == null ? null : screen.getTitle().getString())).append(',');
		json.append("\"slots\":[");
		if (screen instanceof AbstractContainerScreen<?> containerScreen) {
			boolean first = true;
			for (int i = 0; i < containerScreen.getMenu().slots.size(); i++) {
				Slot slot = containerScreen.getMenu().slots.get(i);
				if (!first) {
					json.append(',');
				}
				first = false;
				ItemStack stack = slot.getItem();
				json.append('{').append("\"menuSlot\":").append(i).append(',').append("\"containerSlot\":").append(slot.getSlotIndex()).append(',')
						.append("\"x\":").append(containerScreen.getGuiLeft() + slot.x).append(',').append("\"y\":")
						.append(containerScreen.getGuiTop() + slot.y).append(',')
						.append(jsonProperty("item", stack.isEmpty() ? null : stack.getHoverName().getString())).append(',').append("\"count\":")
						.append(stack.getCount()).append('}');
			}
		}
		json.append("],");
		json.append("\"widgets\":[");
		if (screen != null) {
			boolean first = true;
			int index = 0;
			for (GuiEventListener child : screen.children()) {
				if (child instanceof AbstractWidget widget) {
					if (!first) {
						json.append(',');
					}
					first = false;
					json.append('{').append("\"index\":").append(index).append(',').append(jsonProperty("type", widget.getClass().getName())).append(',')
							.append(jsonProperty("message", widget.getMessage().getString())).append(',').append("\"x\":").append(widget.getX()).append(',')
							.append("\"y\":").append(widget.getY()).append(',').append("\"width\":").append(widget.getWidth()).append(',').append("\"height\":")
							.append(widget.getHeight()).append(',').append("\"active\":").append(widget.active).append(',').append("\"visible\":")
							.append(widget.visible).append('}');
				}
				index++;
			}
		}
		json.append("],");
		json.append("\"mobCatcherCapturedMobs\":[");
		if (screen instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getMenu() instanceof BackpackContainer backpackContainer) {
			boolean first = true;
			for (CapturedMob capturedMob : MobCatcherStorage.getCapturedMobs(backpackContainer.getStorageWrapper())) {
				if (capturedMob.slot() >= backpackContainer.getNumberOfStorageInventorySlots()) {
					continue;
				}
				Slot slot = backpackContainer.getSlot(capturedMob.slot());
				int x = containerScreen.getGuiLeft() + slot.x - 1;
				int y = containerScreen.getGuiTop() + slot.y - 1;
				int width = capturedMob.width() * 18;
				int height = capturedMob.height() * 18;
				if (!first) {
					json.append(',');
				}
				first = false;
				json.append('{').append(jsonProperty("id", capturedMob.id().toString())).append(',')
						.append(jsonProperty("entityType", capturedMob.entityType().toString())).append(',')
						.append(jsonProperty("displayName", capturedMob.displayName())).append(',').append("\"slot\":").append(capturedMob.slot()).append(',')
						.append("\"x\":").append(x).append(',').append("\"y\":").append(y).append(',').append("\"width\":").append(width).append(',')
						.append("\"height\":").append(height).append(',').append("\"releaseX\":").append(x + width / 2).append(',').append("\"releaseY\":")
						.append(y + height / 2).append('}');
			}
		}
		json.append("]}");
		return json.toString();
	}

	private static String clickWidget(String text, boolean contains, int button, int targetIndex) {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;
		if (screen == null) {
			return "{\"ok\":false,\"error\":\"No screen is open\"}";
		}
		int index = 0;
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget widget && widget.visible && widget.active
					&& (targetIndex == index || targetIndex < 0 && textMatches(widget.getMessage().getString(), text, contains))) {
				double x = widget.getX() + widget.getWidth() / 2.0;
				double y = widget.getY() + widget.getHeight() / 2.0;
				boolean clicked = screen.mouseClicked(x, y, button);
				screen.mouseReleased(x, y, button);
				return "{\"ok\":" + clicked + ",\"index\":" + index + ",\"x\":" + x + ",\"y\":" + y + "}";
			}
			index++;
		}
		return "{\"ok\":false,\"error\":\"Widget not found\"}";
	}

	private static String pressKey(String keyName, boolean ctrl, boolean shift, boolean alt) {
		Minecraft minecraft = Minecraft.getInstance();
		int keyCode = keyCode(keyName);
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
			return "{\"ok\":false,\"error\":\"Unknown key\"}";
		}
		if (keyCode == GLFW.GLFW_KEY_E && minecraft.screen == null && minecraft.player != null) {
			minecraft.setScreen(new InventoryScreen(minecraft.player));
			return "{\"ok\":true,\"handled\":true}";
		}
		if (minecraft.screen != null) {
			int modifiers = (ctrl ? GLFW.GLFW_MOD_CONTROL : 0) | (shift ? GLFW.GLFW_MOD_SHIFT : 0) | (alt ? GLFW.GLFW_MOD_ALT : 0);
			boolean handled = minecraft.screen.keyPressed(keyCode, 0, modifiers);
			if (!handled && keyCode == GLFW.GLFW_KEY_ESCAPE) {
				minecraft.screen.onClose();
				handled = true;
			}
			return "{\"ok\":" + handled + ",\"handled\":" + handled + ",\"modifiers\":" + modifiers + "}";
		}
		return "{\"ok\":false,\"handled\":false,\"error\":\"No screen is open\"}";
	}

	private static String moveToSlot(int menuSlot) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return "{\"ok\":false,\"error\":\"No container screen is open\"}";
		}
		if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
			return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
		}
		Slot slot = containerScreen.getMenu().slots.get(menuSlot);
		int x = containerScreen.getGuiLeft() + slot.x + 8;
		int y = containerScreen.getGuiTop() + slot.y + 8;
		double scale = minecraft.getWindow().getGuiScale();
		GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x * scale, y * scale);
		containerScreen.mouseMoved(x, y);
		return "{\"ok\":true,\"menuSlot\":" + menuSlot + ",\"x\":" + x + ",\"y\":" + y + "}";
	}

	private static String throwSlot(int menuSlot, boolean fullStack) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) || minecraft.player == null || minecraft.gameMode == null) {
			return "{\"ok\":false,\"error\":\"No container screen is open\"}";
		}
		if (menuSlot < 0 || menuSlot >= containerScreen.getMenu().slots.size()) {
			return "{\"ok\":false,\"error\":\"Invalid menu slot\"}";
		}
		minecraft.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, menuSlot, fullStack ? 1 : 0, ClickType.THROW, minecraft.player);
		return "{\"ok\":true,\"menuSlot\":" + menuSlot + ",\"fullStack\":" + fullStack + "}";
	}

	private static String moveMouse(String position, int x, int y) {
		Minecraft minecraft = Minecraft.getInstance();
		int targetX = x;
		int targetY = y;
		if (!position.isEmpty()) {
			int margin = 4;
			switch (position.toLowerCase(Locale.ROOT)) {
				case "top-left" -> {
					targetX = margin;
					targetY = margin;
				}
				case "top-right" -> {
					targetX = minecraft.getWindow().getGuiScaledWidth() - margin;
					targetY = margin;
				}
				case "bottom-left" -> {
					targetX = margin;
					targetY = minecraft.getWindow().getGuiScaledHeight() - margin;
				}
				case "bottom-right" -> {
					targetX = minecraft.getWindow().getGuiScaledWidth() - margin;
					targetY = minecraft.getWindow().getGuiScaledHeight() - margin;
				}
				default -> {
					return "{\"ok\":false,\"error\":\"Unknown mouse position\"}";
				}
			}
		}
		if (targetX < 0 || targetY < 0) {
			return "{\"ok\":false,\"error\":\"Missing mouse coordinates\"}";
		}
		try {
			Object mouseHandler = minecraft.mouseHandler;
			Field xpos = mouseHandler.getClass().getDeclaredField("xpos");
			Field ypos = mouseHandler.getClass().getDeclaredField("ypos");
			xpos.setAccessible(true);
			ypos.setAccessible(true);
			xpos.setDouble(mouseHandler, targetX * (double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth());
			ypos.setDouble(mouseHandler, targetY * (double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to move mouse", e);
		}
		DemoMouseMotion.moveTo(targetX, targetY, 12, () -> {
		});
		return "{\"ok\":true,\"x\":" + targetX + ",\"y\":" + targetY + "}";
	}

	private static String clickMouse(int x, int y, int button, boolean shift) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen == null) {
			return "{\"ok\":false,\"error\":\"No screen is open\"}";
		}
		if (x < 0 || y < 0 || x >= minecraft.getWindow().getGuiScaledWidth() || y >= minecraft.getWindow().getGuiScaledHeight()) {
			return "{\"ok\":false,\"error\":\"Mouse coordinates are outside the screen\"}";
		}

		try {
			MouseHandler mouseHandler = minecraft.mouseHandler;
			Field xpos = MouseHandler.class.getDeclaredField("xpos");
			Field ypos = MouseHandler.class.getDeclaredField("ypos");
			Method onPress = MouseHandler.class.getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
			xpos.setAccessible(true);
			ypos.setAccessible(true);
			onPress.setAccessible(true);
			xpos.setDouble(mouseHandler, x * (double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth());
			ypos.setDouble(mouseHandler, y * (double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight());
			int modifiers = shift ? GLFW.GLFW_MOD_SHIFT : 0;
			onPress.invoke(mouseHandler, minecraft.getWindow().getWindow(), button, GLFW.GLFW_PRESS, modifiers);
			onPress.invoke(mouseHandler, minecraft.getWindow().getWindow(), button, GLFW.GLFW_RELEASE, modifiers);
			return "{\"ok\":true,\"x\":" + x + ",\"y\":" + y + ",\"button\":" + button + ",\"shift\":" + shift + "}";
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to dispatch mouse input", e);
		}
	}

	private static String maximizeWindow() {
		Minecraft minecraft = Minecraft.getInstance();
		GLFW.glfwMaximizeWindow(minecraft.getWindow().getWindow());
		return "{\"ok\":true}";
	}

	private static boolean matchesCondition(String condition, String screenName) {
		Minecraft minecraft = Minecraft.getInstance();
		if ("worldLoaded".equals(condition)) {
			return minecraft.level != null && minecraft.player != null;
		}
		if ("screen".equals(condition)) {
			Screen screen = minecraft.screen;
			return screen != null && (screen.getClass().getName().equals(screenName) || screen.getClass().getSimpleName().equals(screenName));
		}
		if ("noScreen".equals(condition)) {
			return minecraft.screen == null;
		}
		return false;
	}

	private static String runCommand(ServerPlayer player, String command) {
		String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
		player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), normalizedCommand);
		return "{\"ok\":true," + jsonProperty("command", normalizedCommand) + ",\"dispatched\":true}";
	}

	private static boolean textMatches(String actual, String expected, boolean contains) {
		if (contains) {
			return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
		}
		return actual.equals(expected);
	}

	private static int keyCode(String keyName) {
		return switch (keyName.toUpperCase(Locale.ROOT)) {
			case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
			case "E" -> GLFW.GLFW_KEY_E;
			case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
			case "TAB" -> GLFW.GLFW_KEY_TAB;
			case "SPACE" -> GLFW.GLFW_KEY_SPACE;
			case "KP_0", "NUMPAD0" -> GLFW.GLFW_KEY_KP_0;
			case "UP" -> GLFW.GLFW_KEY_UP;
			case "DOWN" -> GLFW.GLFW_KEY_DOWN;
			case "LEFT" -> GLFW.GLFW_KEY_LEFT;
			case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
			default -> keyName.length() == 1 ? Character.toUpperCase(keyName.charAt(0)) : GLFW.GLFW_KEY_UNKNOWN;
		};
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
