package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Bounded authoritative assertions for synthetic menu regions, player inventory, and held backpacks. */
public final class AssertInventoryInteractionCountAction extends TypedRegressionAction<AssertInventoryInteractionCountAction.Arguments> {
	public record Arguments(String target, @Nullable String item, int count, int stacks, @Nullable String ref, int timeoutTicks) {
	}

	public AssertInventoryInteractionCountAction() {
		super("assert.inventoryInteractionCount", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("target", "item", "count", "stacks", "ref", "timeoutTicks"), description);
		String target = requireString(args, "target", description);
		if (!Set.of("container", "player", "backpack", "craftingRegion", "furnaceRegion").contains(target)) {
			throw new IllegalArgumentException("Invalid inventory interaction target on " + description);
		}
		boolean region = target.endsWith("Region");
		if (region == args.has("item")) {
			throw new IllegalArgumentException("item is required exactly for item-count targets on " + description);
		}
		if ((target.equals("container") || region) != args.has("ref")) {
			throw new IllegalArgumentException("ref is required exactly for synthetic menu targets on " + description);
		}
		int count = requireNonNegativeInt(args, "count", description);
		if (region && count != 0 || region && args.has("stacks")) {
			throw new IllegalArgumentException("Synthetic untouched regions require count 0 and no stacks on " + description);
		}
		return new Arguments(target, region ? null : requireString(args, "item", description), count,
				args.has("stacks") ? requireNonNegativeInt(args, "stacks", description) : -1,
				args.has("ref") ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null,
				optionalPositiveInt(args, "timeoutTicks", 100, description));
	}

	@Override
	protected Set<String> dependencies(Arguments arguments) {
		return arguments.target().equals("backpack") ? Set.of("minecraft:client", "sophisticatedbackpacks") : Set.of("minecraft:client");
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.ref() == null ? RegressionValueContract.EMPTY : RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.VANILLA_MENU);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> observation;
			private boolean matched;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				}
				if (observation == null) {
					observation = context.onServer(player -> observe(player));
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(observation);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (matched) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return failAssertion("Inventory interaction assertion did not match within " + arguments.timeoutTicks() + " ticks: " + expectedActual());
				}
				observation = null;
				return RegressionActionProgress.WAITING;
			}

			private void observe(ServerPlayer player) {
				Count actual = count(player);
				matched = actual.count() == arguments.count() && (arguments.stacks() < 0 || actual.stacks() == arguments.stacks());
				observe(Map.of("count", arguments.count(), "stacks", arguments.stacks()), Map.of("count", actual.count(), "stacks", actual.stacks()));
				context.putDiagnostic("inventoryInteractionCount", Map.of("target", arguments.target(), "count", actual.count(), "stacks", actual.stacks()));
			}

			private Count count(ServerPlayer player) {
				if (arguments.target().equals("craftingRegion") || arguments.target().equals("furnaceRegion")) {
					AbstractContainerMenu menu = requireMenu(player);
					int end = arguments.target().equals("craftingRegion") ? 10 : 3;
					long nonEmpty = 0;
					for (int slot = 0; slot < end; slot++) {
						if (!menu.getSlot(slot).getItem().isEmpty()) {
							nonEmpty++;
						}
					}
					return new Count(nonEmpty, -1);
				}
				Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
						.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
				if (arguments.target().equals("container")) {
					AbstractContainerMenu menu = requireMenu(player);
					if (!(menu instanceof ChestMenu chest) || !(chest.getContainer() instanceof SimpleContainer inventory)) {
						throw RegressionActionFailure.infrastructure("Expected synthetic chest menu");
					}
					return count(inventory, item);
				}
				if (arguments.target().equals("player")) {
					return count(player.getInventory().items, item);
				}
				if (!(player.getMainHandItem().getItem() instanceof BackpackItem)) {
					throw RegressionActionFailure.infrastructure("Expected held backpack");
				}
				var inventory = BackpackWrapper.fromStack(player.getMainHandItem()).getInventoryHandler();
				long count = 0;
				long stacks = 0;
				for (int slot = 0; slot < inventory.getSlots(); slot++) {
					ItemStack stack = inventory.getStackInSlot(slot);
					if (stack.is(item)) {
						count += stack.getCount();
						stacks++;
					}
				}
				return new Count(count, stacks);
			}

			private AbstractContainerMenu requireMenu(ServerPlayer player) {
				VanillaMenuHandle handle = context.values().require(arguments.ref(), VanillaMenuHandle.class);
				AbstractContainerMenu menu = player.containerMenu;
				if (menu.containerId != handle.containerId() || !matches(menu, handle.kind())) {
					throw RegressionActionFailure.infrastructure("Server menu does not match synthetic menu handle");
				}
				return menu;
			}
		};
	}

	private static Count count(Iterable<ItemStack> stacks, Item item) {
		long count = 0;
		long cardinality = 0;
		for (ItemStack stack : stacks) {
			if (stack.is(item)) {
				count += stack.getCount();
				cardinality++;
			}
		}
		return new Count(count, cardinality);
	}

	private static Count count(SimpleContainer inventory, Item item) {
		long count = 0;
		long stacks = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
				stacks++;
			}
		}
		return new Count(count, stacks);
	}

	private static boolean matches(AbstractContainerMenu menu, VanillaMenuHandle.Kind kind) {
		return switch (kind) {
			case CHEST -> menu instanceof ChestMenu;
			case CRAFTING -> menu instanceof CraftingMenu;
			case FURNACE -> menu instanceof FurnaceMenu;
		};
	}

	private record Count(long count, long stacks) {
	}
}
