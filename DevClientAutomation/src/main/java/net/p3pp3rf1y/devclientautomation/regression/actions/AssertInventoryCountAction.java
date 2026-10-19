package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Observes aggregate authoritative contents, including an expected empty inventory. */
public final class AssertInventoryCountAction extends TypedRegressionAction<AssertInventoryCountAction.Arguments> {
	public record Arguments(String target, String item, int count, int x, int y, int z) {
	}

	public AssertInventoryCountAction() {
		super("assert.inventoryCount", "1", Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("target", "item", "count", "offset"), description);
		String target = requireString(args, "target", description);
		if (!Set.of("backpack", "storage").contains(target)) {
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
		if (target.equals("backpack")) {
			if (args.has("offset")) {
				throw new IllegalArgumentException("Backpack inventory has no block offset on " + description);
			}
			return new Arguments(target, item, count, 0, 0, 0);
		}
		JsonObject positionArgs = new JsonObject();
		positionArgs.addProperty("block", "minecraft:air");
		positionArgs.add("offset", args.get("offset"));
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(target, item, count, position.x(), position.y(), position.z());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private @Nullable CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						InventoryHandler inventory;
						if (arguments.target().equals("backpack")) {
							if (!(player.getMainHandItem().getItem() instanceof BackpackItem)) {
								throw new IllegalStateException("Main hand does not contain a backpack");
							}
							inventory = new BackpackWrapper(player.getMainHandItem()).getInventoryHandler();
						} else {
							var position = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
							if (!(player.level().getBlockEntity(position) instanceof IControllableStorage storage)) {
								throw new IllegalStateException("No live storage at " + position);
							}
							inventory = storage.getStorageWrapper().getInventoryHandler();
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown inventory item " + arguments.item()));
						long actual = 0;
						for (int slot = 0; slot < inventory.size(); slot++) {
							var stack = inventory.getStackInSlot(slot);
							if (stack.is(item)) {
								actual += stack.getCount();
							}
						}
						context.putDiagnostic("inventoryCount", arguments.target() + ":" + arguments.item() + "=" + actual);
						if (actual != arguments.count()) {
							throw new IllegalStateException(
									arguments.target() + " " + arguments.item() + ": expected " + arguments.count() + ", actual " + actual);
						}
					});
				}
				return await(future);
			}
		};
	}
}
