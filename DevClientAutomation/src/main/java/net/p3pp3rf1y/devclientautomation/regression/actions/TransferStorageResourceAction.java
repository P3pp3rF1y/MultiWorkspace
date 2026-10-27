package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class TransferStorageResourceAction extends TypedRegressionAction<TransferStorageResourceAction.Arguments> {
	public record Arguments(String ref, String operation, int amount, int expected, String fluid, boolean simulate, int inventorySlot) {
	}

	public TransferStorageResourceAction() {
		super("capability.transferStorageResource", "1", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "operation", "amount", "expected", "fluid", "simulate", "inventorySlot"), description);
		String operation = requireString(args, "operation", description);
		if (!Set.of("fill", "drain", "receive", "extract").contains(operation)) {
			throw new IllegalArgumentException("Unknown resource transfer " + operation);
		}
		String fluid = optionalString(args, "fluid", "water", description);
		if (!Set.of("water", "lava").contains(fluid)) {
			throw new IllegalArgumentException("Unknown transfer fluid " + fluid);
		}
		if (args.has("simulate") && (!args.get("simulate").isJsonPrimitive() || !args.get("simulate").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument simulate on " + description + " must be boolean");
		}
		int amount = requirePositiveInt(args, "amount", description);
		int inventorySlot = args.has("inventorySlot") ? requireNonNegativeInt(args, "inventorySlot", description) : -1;
		if (inventorySlot > 35 || (inventorySlot >= 0 && (args.has("ref") || operation.equals("receive") || operation.equals("extract")))) {
			throw new IllegalArgumentException("Item fluid transfers require a slot between 0 and 35, no entity ref, and fill or drain on " + description);
		}
		return new Arguments(optionalString(args, "ref", "", description), operation, amount, optionalNonNegativeInt(args, "expected", amount, description),
				fluid, args.has("simulate") && args.get("simulate").getAsBoolean(), inventorySlot);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.ref().isEmpty()
				? RegressionValueContract.EMPTY
				: RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						Entity entity = arguments.ref().isEmpty()
								? null
								: context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
						ItemStack stack = arguments.inventorySlot() >= 0 ? player.getInventory().getItem(arguments.inventorySlot()) : ItemStack.EMPTY;
						ItemResource original = arguments.inventorySlot() >= 0 ? ItemResource.of(stack) : ItemResource.EMPTY;
						int originalCount = stack.getCount();
						int transferred;
						try (Transaction transaction = Transaction.openRoot()) {
							if (arguments.operation().equals("fill") || arguments.operation().equals("drain")) {
								ItemAccess itemAccess = arguments.inventorySlot() >= 0 && !stack.isEmpty()
										? ItemAccess.forPlayerSlot(player, arguments.inventorySlot())
										: null;
								ResourceHandler<FluidResource> fluid = itemAccess != null
										? itemAccess.getCapability(Capabilities.Fluid.ITEM)
										: arguments.inventorySlot() >= 0
												? null
												: entity == null
														? player.level().getCapability(Capabilities.Fluid.BLOCK, context.target(), Direction.UP)
														: entity.getCapability(Capabilities.Fluid.ENTITY, Direction.UP);
								if (fluid == null) {
									throw new IllegalStateException("No fluid capability at endpoint");
								}
								FluidResource resource = FluidResource.of(arguments.fluid().equals("water") ? Fluids.WATER : Fluids.LAVA);
								transferred = arguments.operation().equals("fill")
										? fluid.insert(resource, arguments.amount(), transaction)
										: drainAny(fluid, arguments.amount(), transaction);
							} else {
								EnergyHandler energy = entity == null
										? player.level().getCapability(Capabilities.Energy.BLOCK, context.target(), Direction.UP)
										: entity.getCapability(Capabilities.Energy.ENTITY, Direction.UP);
								if (energy == null) {
									throw new IllegalStateException("No energy capability at endpoint");
								}
								transferred = arguments.operation().equals("receive")
										? energy.insert(arguments.amount(), transaction)
										: energy.extract(arguments.amount(), transaction);
							}
							if (transferred != arguments.expected()) {
								throw new IllegalStateException("Transferred " + transferred + ", expected " + arguments.expected());
							}
							if (!arguments.simulate()) {
								transaction.commit();
							}
						}
						if (arguments.inventorySlot() >= 0) {
							ItemStack current = player.getInventory().getItem(arguments.inventorySlot());
							if (original.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)
									&& (current != stack || current.getItem() != original.toStack().getItem() || current.getCount() != originalCount
											|| !original.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)
													.equals(current.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)))) {
								throw RegressionActionFailure.assertion("Item fluid capability returned a different physical linked endpoint");
							}
							if (arguments.simulate() && !original.equals(ItemResource.of(current))) {
								throw RegressionActionFailure.assertion("Simulated item fluid transfer changed the inventory endpoint");
							}
							if (!arguments.simulate()) {
								player.inventoryMenu.broadcastFullState();
							}
						}
					});
				}
				return await(future);
			}
		};
	}

	private static int drainAny(ResourceHandler<FluidResource> fluid, int amount, Transaction transaction) {
		for (int index = 0; index < fluid.size(); index++) {
			FluidResource resource = fluid.getResource(index);
			if (!resource.isEmpty()) {
				return fluid.extract(resource, amount, transaction);
			}
		}
		return 0;
	}
}
