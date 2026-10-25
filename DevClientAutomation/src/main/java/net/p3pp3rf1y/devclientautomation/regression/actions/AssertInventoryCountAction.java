package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Observes aggregate authoritative inventory contents, including an expected empty inventory. */
public final class AssertInventoryCountAction extends TypedRegressionAction<AssertInventoryCountAction.Arguments> {
	public record Arguments(String target, String item, int count, int x, int y, int z, boolean client, String fullItem) {
	}

	public AssertInventoryCountAction() {
		super("assert.inventoryCount", "2", Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("target", "item", "count", "offset", "client", "fullItem"), description);
		String target = requireString(args, "target", description);
		if (!Set.of("backpack", "storage", "player").contains(target)) {
			throw new IllegalArgumentException("Invalid inventory target on " + description);
		}
		String item = requireString(args, "item", description);
		if (!args.has("count") || !args.get("count").isJsonPrimitive() || !args.get("count").getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException("Expected integer count on " + description);
		}
		int count;
		try {
			count = Integer.parseInt(args.get("count").getAsString());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Expected integer count on " + description, e);
		}
		if (count < 0) {
			throw new IllegalArgumentException("Expected nonnegative count on " + description);
		}
		boolean client = optionalBoolean(args, "client", false, description);
		String fullItem = optionalString(args, "fullItem", "", description);
		if (!fullItem.isEmpty() && !target.equals("backpack") || client && target.equals("storage")) {
			throw new IllegalArgumentException("Full requires backpack; client requires backpack/player on " + description);
		}
		if (!target.equals("storage")) {
			if (args.has("offset")) {
				throw new IllegalArgumentException("Player/Backpack inventory has no block offset on " + description);
			}
			return new Arguments(target, item, count, 0, 0, 0, client, fullItem);
		}
		// Use the existing strict block-offset binder; offsets are relative to the fixture origin.
		JsonObject positionArgs = new JsonObject();
		positionArgs.addProperty("block", "minecraft:air");
		positionArgs.add("offset", args.get("offset"));
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(target, item, count, position.x(), position.y(), position.z(), false, "");
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					deadline = context.minecraft().player.tickCount + 100;
					future = context.onServer(player -> {
						Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown inventory item " + arguments.item()));
						long actual;
						boolean full = true;
						if (arguments.target().equals("backpack")) {
							if (!(player.getMainHandItem().getItem() instanceof BackpackItem)) {
								throw RegressionActionFailure.infrastructure("Main hand does not contain a backpack");
							}
							var inventory = new BackpackWrapper(player.getMainHandItem()).getInventoryHandler();
							actual = count(inventory, item);
							full = arguments.fullItem().isEmpty() || isFull(inventory, arguments.fullItem());
						} else if (arguments.target().equals("player")) {
							actual = countPlayer(player, item);
						} else {
							var position = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
							if (!(player.level().getBlockEntity(position) instanceof IControllableStorage storage)) {
								throw RegressionActionFailure.infrastructure("No live storage at " + position);
							}
							actual = count(storage.getStorageWrapper().getInventoryHandler(), item);
						}
						observe(Map.of("count", arguments.count(), "fullItem", arguments.fullItem()), Map.of("count", actual, "full", full));
						context.putDiagnostic("inventoryCount", arguments.target() + ":" + arguments.item() + "=" + actual);
						if (actual != arguments.count() || !full) {
							throw RegressionActionFailure
									.assertion(arguments.target() + " " + arguments.item() + ": expected " + arguments.count() + ", actual " + actual);
						}
					});
				}
				RegressionActionProgress result = await(future);
				if (result != RegressionActionProgress.PASSED || !arguments.client()) {
					return result;
				}
				Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
						.orElseThrow(() -> new IllegalArgumentException("Unknown inventory item " + arguments.item()));
				long actual;
				boolean full = true;
				if (arguments.target().equals("backpack")) {
					if (!context.isExpectedMenu("backpackItem")) {
						return fail("Client backpack count requires the synchronized held-backpack menu");
					}
					var inventory = ((BackpackContainer) context.minecraft().player.containerMenu).getStorageWrapper().getInventoryHandler();
					actual = count(inventory, item);
					full = arguments.fullItem().isEmpty() || isFull(inventory, arguments.fullItem());
				} else {
					actual = countPlayer(context.minecraft().player, item);
				}
				observe(Map.of("count", arguments.count(), "fullItem", arguments.fullItem()), Map.of("count", actual, "full", full));
				context.putDiagnostic("clientInventoryCount-" + arguments.target(), Map.of("item", arguments.item(), "count", actual, "full", full));
				if (actual == arguments.count() && full) {
					return RegressionActionProgress.PASSED;
				}
				return context.minecraft().player.tickCount >= deadline
						? failAssertion("Client inventory count/fullness did not synchronize: " + expectedActual())
						: RegressionActionProgress.WAITING;
			}
		};
	}

	private static long count(InventoryHandler inventory, Item item) {
		long count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			var stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static long countPlayer(Player player, Item item) {
		long count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean isFull(InventoryHandler inventory, String itemId) {
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
				.orElseThrow(() -> new IllegalArgumentException("Unknown full inventory item " + itemId));
		for (int slot = 0; slot < inventory.size(); slot++) {
			var stack = inventory.getStackInSlot(slot);
			if (!stack.is(item) || stack.getCount() != stack.getMaxStackSize()) {
				return false;
			}
		}
		return true;
	}
}
