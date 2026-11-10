package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Verifies the converted held bag's identity, dimensions, contents, upgrade, and player marker on both sides. */
public final class AssertHeldBackpackStateAction extends TypedRegressionAction<AssertHeldBackpackStateAction.Arguments> {
	public record Content(int slot, String item, int count) {
	}
	public record Arguments(String uuid, String tier, int storageSlots, int upgradeSlots, List<Content> contents, int upgradeSlot, String upgrade,
			int playerSlot, String playerItem, int playerCount, int timeoutTicks) {
	}

	public AssertHeldBackpackStateAction() {
		super("assert.heldBackpackState", "2", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("uuid", "tier", "storageSlots", "upgradeSlots", "contents", "upgradeSlot", "upgrade", "playerSlot", "playerItem",
				"playerCount", "timeoutTicks"), description);
		if (!args.has("contents") || !args.get("contents").isJsonArray() || args.getAsJsonArray("contents").isEmpty()) {
			throw new IllegalArgumentException("Argument contents on " + description + " must be a non-empty array");
		}
		List<Content> contents = args.getAsJsonArray("contents").asList().stream().map(value -> {
			if (!value.isJsonObject())
				throw new IllegalArgumentException("Each contents entry on " + description + " must be an object");
			JsonObject content = value.getAsJsonObject();
			requireOnly(content, Set.of("slot", "item", "count"), description + " contents entry");
			return new Content(requireNonNegativeInt(content, "slot", description), requireString(content, "item", description),
					requirePositiveInt(content, "count", description));
		}).toList();
		int storageSlots = requirePositiveInt(args, "storageSlots", description);
		int upgradeSlots = requirePositiveInt(args, "upgradeSlots", description);
		int upgradeSlot = optionalNonNegativeInt(args, "upgradeSlot", 0, description);
		int playerSlot = requireNonNegativeInt(args, "playerSlot", description);
		if (contents.stream().anyMatch(content -> content.slot() >= storageSlots))
			throw new IllegalArgumentException("contents slot must be less than storageSlots on " + description);
		if (upgradeSlot >= upgradeSlots)
			throw new IllegalArgumentException("upgradeSlot must be less than upgradeSlots on " + description);
		if (playerSlot >= 36)
			throw new IllegalArgumentException("playerSlot must be less than 36 on " + description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "uuid", description)), requireString(args, "tier", description), storageSlots,
				upgradeSlots, contents, upgradeSlot, requireString(args, "upgrade", description), playerSlot, requireString(args, "playerItem", description),
				requirePositiveInt(args, "playerCount", description), optionalPositiveInt(args, "timeoutTicks", 100, description));
	}
	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.uuid(), RegressionValueTypes.UUID);
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> server;
			private String serverDiagnostic = "not observed";
			private String clientDiagnostic = "not observed";
			private int deadline = -1;
			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0)
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				if (server == null) {
					server = context.onServer(
							player -> serverDiagnostic = stateDiagnostic(player.getMainHandItem(), player.getInventory().getItem(arguments.playerSlot()),
									context.values().require(arguments.uuid(), UUID.class), arguments, player.containerMenu));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(server);
				if (result != RegressionActionProgress.PASSED)
					return result;
				clientDiagnostic = !hasType(context.minecraft().screen, "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase")
						? screenDiagnostic(context.minecraft().screen)
						: stateDiagnostic(context.minecraft().player.getMainHandItem(),
								context.minecraft().player.getInventory().getItem(arguments.playerSlot()),
								context.values().require(arguments.uuid(), UUID.class), arguments, context.minecraft().player.containerMenu);
				if (serverDiagnostic == null && clientDiagnostic == null)
					return RegressionActionProgress.PASSED;
				if (context.minecraft().player.tickCount >= deadline)
					return failAssertion("Held backpack state mismatch: server=" + serverDiagnostic + "; client=" + clientDiagnostic);
				server = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}
	static String screenDiagnostic(Object screen) {
		return "screen expected=StorageScreenBase actual=" + (screen == null ? "<none>" : screen.getClass().getName());
	}
	private static String stateDiagnostic(ItemStack backpack, ItemStack playerMarker, UUID uuid, Arguments a, Object menu) {
		if (!BuiltInRegistries.ITEM.getKey(backpack.getItem()).toString().equals("sophisticatedbackpacks:" + a.tier() + "_backpack"))
			return "tier expected=" + a.tier() + " actual=" + BuiltInRegistries.ITEM.getKey(backpack.getItem());
		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		Item upgrade = item(a.upgrade());
		Item marker = item(a.playerItem());
		if (!wrapper.getContentsUuid().filter(uuid::equals).isPresent())
			return "uuid expected=" + uuid + " actual=" + wrapper.getContentsUuid().orElse(null);
		if (wrapper.getInventoryHandler().size() != a.storageSlots())
			return "storageSlots expected=" + a.storageSlots() + " actual=" + wrapper.getInventoryHandler().size();
		if (wrapper.getUpgradeHandler().size() != a.upgradeSlots())
			return "upgradeSlots expected=" + a.upgradeSlots() + " actual=" + wrapper.getUpgradeHandler().size();
		for (Content content : a.contents())
			if (!same(wrapper.getInventoryHandler().getStackInSlot(content.slot()), item(content.item()), content.count()))
				return "contents[" + content.slot() + "] expected=" + content.item() + " x" + content.count() + " actual="
						+ wrapper.getInventoryHandler().getStackInSlot(content.slot());
		if (!wrapper.getUpgradeHandler().getStackInSlot(a.upgradeSlot()).is(upgrade))
			return "upgrade[" + a.upgradeSlot() + "] expected=" + a.upgrade() + " actual=" + wrapper.getUpgradeHandler().getStackInSlot(a.upgradeSlot());
		if (!same(playerMarker, marker, a.playerCount()))
			return "playerSlot[" + a.playerSlot() + "] expected=" + a.playerItem() + " x" + a.playerCount() + " actual=" + playerMarker;
		return menuMatches(menu, a)
				? null
				: "menu expected=backpack storage=" + a.storageSlots() + ", upgrades=" + a.upgradeSlots() + " actual=" + menu.getClass().getName();
	}
	private static boolean menuMatches(Object candidate, Arguments arguments) {
		try {
			if (!hasType(candidate, "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer"))
				return false;
			Method storageSlots = candidate.getClass().getMethod("getNumberOfStorageInventorySlots");
			Method upgradeSlots = candidate.getClass().getMethod("getNumberOfUpgradeSlots");
			int playerStart = (int) storageSlots.invoke(candidate);
			if (playerStart != arguments.storageSlots() || (int) upgradeSlots.invoke(candidate) != arguments.upgradeSlots())
				return false;
			Field slots = candidate.getClass().getField("slots");
			List<?> menuSlots = (List<?>) slots.get(candidate);
			return menuSlots.size() >= playerStart + 36
					&& menuSlots.subList(playerStart, playerStart + 36).stream().allMatch(AssertHeldBackpackStateAction::isPlayerSlot);
		} catch (ReflectiveOperationException exception) {
			return false;
		}
	}
	private static boolean isPlayerSlot(Object slot) {
		try {
			return hasType(slot.getClass().getField("container").get(slot), "net.minecraft.world.entity.player.Inventory");
		} catch (ReflectiveOperationException exception) {
			return false;
		}
	}
	private static boolean hasType(Object object, String typeName) {
		for (Class<?> type = object == null ? null : object.getClass(); type != null; type = type.getSuperclass()) {
			if (type.getName().equals(typeName))
				return true;
		}
		return false;
	}
	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElseThrow();
	}
	private static boolean same(ItemStack stack, Item item, int count) {
		return stack.is(item) && stack.getCount() == count;
	}
}
