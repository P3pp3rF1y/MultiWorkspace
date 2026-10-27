package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeWrapper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Exercises the installed production pump against a physical held item in a live Forge server. */
public final class TransferLinkedPumpFluidAction extends TypedRegressionAction<TransferLinkedPumpFluidAction.Arguments> {
	public record Arguments(int hostSlot, String direction, String held) {
	}

	public TransferLinkedPumpFluidAction() {
		super("capability.transferLinkedPumpFluid", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("hostSlot", "direction", "held"), description);
		int slot = optionalNonNegativeInt(args, "hostSlot", 8, description);
		if (slot > 35) {
			throw new IllegalArgumentException(description + " hostSlot must be between 0 and 35");
		}
		String direction = requireString(args, "direction", description);
		String held = requireString(args, "held", description);
		if (!Set.of("input", "output").contains(direction) || !Set.of("otherGroup", "bucket").contains(held)) {
			throw new IllegalArgumentException(description + " requires direction input/output and held otherGroup/bucket");
		}
		return new Arguments(slot, direction, held);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack host = player.getInventory().getItem(arguments.hostSlot());
						ItemStack hand = player.getMainHandItem();
						LinkedStorageEndpointData hostEndpoint = LinkedStorageStackData.getEndpoint(host);
						if (!(host.getItem() instanceof BackpackItem) || hostEndpoint == null || host == hand) {
							throw RegressionActionFailure.infrastructure("Pump host must be a separate linked backpack endpoint");
						}
						boolean otherGroup = arguments.held().equals("otherGroup");
						if (otherGroup) {
							LinkedStorageEndpointData handEndpoint = LinkedStorageStackData.getEndpoint(hand);
							if (!(hand.getItem() instanceof BackpackItem) || handEndpoint == null || hostEndpoint.groupId().equals(handEndpoint.groupId())) {
								throw RegressionActionFailure.infrastructure("Held item must be a linked backpack in a different group");
							}
						} else if (!hand.is(arguments.direction().equals("input") ? Items.WATER_BUCKET : Items.BUCKET)) {
							throw RegressionActionFailure.infrastructure("Held bucket has incorrect initial fluid state");
						}
						IFluidHandlerItem handFluid = hand.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
								.orElseThrow(() -> RegressionActionFailure.infrastructure("Held item has no Forge item fluid capability"));
						if (otherGroup && handFluid.getContainer() != hand) {
							throw RegressionActionFailure.assertion("Held linked item capability does not retain the physical endpoint");
						}
						var hostWrapper = BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), host);
						IFluidHandler hostFluid = hostWrapper.getFluidHandler()
								.orElseThrow(() -> RegressionActionFailure.infrastructure("Host has no canonical fluid handler"));
						IFluidHandler sourceOrDestination = otherGroup
								? BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), hand).getFluidHandler()
										.orElseThrow(() -> RegressionActionFailure.infrastructure("Other group has no canonical fluid handler"))
								: handFluid;
						boolean input = arguments.direction().equals("input");
						int initialHost = input ? 0 : 1000;
						int initialHand = input ? 1000 : 0;
						if (hostFluid.getFluidInTank(0).getAmount() != initialHost || sourceOrDestination.getFluidInTank(0).getAmount() != initialHand) {
							throw RegressionActionFailure.assertion("Pump starting fluid amounts differ: host=" + hostFluid.getFluidInTank(0).getAmount()
									+ ", held=" + sourceOrDestination.getFluidInTank(0).getAmount());
						}
						// Install and tick on the same server task: inventoryTick must not race the before/after assertions.
						hostWrapper.getInventoryHandler();
						hostWrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.ADVANCED_PUMP_UPGRADE.get()));
						var wrappers = hostWrapper.getUpgradeHandler().getTypeWrappers(ModItems.ADVANCED_PUMP_UPGRADE.get().getType());
						if (wrappers.size() != 1) {
							throw RegressionActionFailure.infrastructure("Expected one installed advanced pump on host, found " + wrappers.size());
						}
						PumpUpgradeWrapper pump = wrappers.get(0);
						if (!pump.shouldInteractWithHand()) {
							throw RegressionActionFailure.infrastructure("Advanced pump has hand interaction disabled");
						}
						pump.setIsInput(input);
						pump.tick(player, player.serverLevel(), player.blockPosition());
						ItemStack afterHand = player.getMainHandItem();
						int afterHeld = otherGroup
								? BackpackLinkedStorageResolver.resolveOrCreate(player.serverLevel(), afterHand).getFluidHandler().orElseThrow()
										.getFluidInTank(0).getAmount()
								: afterHand.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
										.orElseThrow(() -> RegressionActionFailure.assertion("Transferred bucket lost its Forge item fluid capability"))
										.getFluidInTank(0).getAmount();
						if (hostFluid.getFluidInTank(0).getAmount() != (input ? 1000 : 0) || afterHeld != (input ? 0 : 1000)
								|| otherGroup && (afterHand != hand || LinkedStorageStackData.getEndpoint(afterHand) == null)
								|| !otherGroup && !afterHand.is(input ? Items.BUCKET : Items.WATER_BUCKET)) {
							throw RegressionActionFailure.assertion("Pump did not transfer 1000 mB through held item: host="
									+ hostFluid.getFluidInTank(0).getAmount() + ", held=" + afterHeld + ", item=" + afterHand);
						}
					});
				}
				return await(future);
			}
		};
	}
}
