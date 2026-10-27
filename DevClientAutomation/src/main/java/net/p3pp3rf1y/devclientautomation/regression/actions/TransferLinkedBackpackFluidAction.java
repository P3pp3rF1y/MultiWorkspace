package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class TransferLinkedBackpackFluidAction extends TypedRegressionAction<TransferLinkedBackpackFluidAction.Arguments> {
	public record Arguments(int slot, String operation, int amount, int expected, int canonicalAmount) {
	}

	public TransferLinkedBackpackFluidAction() {
		super("capability.transferLinkedBackpackFluid", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "operation", "amount", "expected", "canonicalAmount"), description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		if (slot > 35) {
			throw new IllegalArgumentException(description + " slot must be between 0 and 35");
		}
		String operation = requireString(args, "operation", description);
		if (!Set.of("fill", "drain").contains(operation)) {
			throw new IllegalArgumentException(description + " operation must be fill or drain");
		}
		return new Arguments(slot, operation, requirePositiveInt(args, "amount", description), optionalNonNegativeInt(args, "expected", 0, description),
				requireNonNegativeInt(args, "canonicalAmount", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack stack = player.getInventory().getItem(arguments.slot());
						if (!(stack.getItem() instanceof BackpackItem) || LinkedStorageStackData.getEndpoint(stack) == null) {
							throw RegressionActionFailure.infrastructure("Slot " + arguments.slot() + " is not a linked backpack endpoint");
						}
						IFluidHandlerItem itemHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
								.orElseThrow(() -> RegressionActionFailure.infrastructure("Linked backpack has no item fluid capability"));
						if (itemHandler.getContainer() != stack) {
							throw RegressionActionFailure.assertion("Item fluid handler did not retain the physical endpoint stack");
						}
						IFluidHandler canonical = BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), stack).getFluidHandler()
								.orElseThrow(() -> RegressionActionFailure.infrastructure("Linked group has no canonical fluid handler"));
						int transferred = arguments.operation().equals("fill")
								? itemHandler.fill(new FluidStack(Fluids.WATER, arguments.amount()), IFluidHandler.FluidAction.EXECUTE)
								: itemHandler.drain(arguments.amount(), IFluidHandler.FluidAction.EXECUTE).getAmount();
						if (transferred != arguments.expected() || canonical.getFluidInTank(0).getAmount() != arguments.canonicalAmount()) {
							throw RegressionActionFailure.assertion("Physical item transferred " + transferred + " (expected " + arguments.expected()
									+ "), canonical amount " + canonical.getFluidInTank(0).getAmount() + " (expected " + arguments.canonicalAmount() + ")");
						}
					});
				}
				return await(future);
			}
		};
	}
}
