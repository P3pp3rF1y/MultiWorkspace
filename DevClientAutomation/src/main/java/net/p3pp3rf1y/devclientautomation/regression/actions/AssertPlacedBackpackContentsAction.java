package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertPlacedBackpackContentsAction extends TypedRegressionAction<AssertPlacedBackpackContentsAction.Arguments> {
	public record Arguments(String item, int count, boolean filterUpgrade, boolean client) {
	}

	public AssertPlacedBackpackContentsAction() {
		super("assert.placedBackpackContents", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "count", "upgrade", "client"), description);
		String upgrade = optionalString(args, "upgrade", "none", description);
		if (!Set.of("none", "filter").contains(upgrade)) {
			throw new IllegalArgumentException(description + ": unsupported backpack upgrade " + upgrade);
		}
		if (args.has("client") && (!args.get("client").isJsonPrimitive() || !args.get("client").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException(description + ": client must be a boolean");
		}
		return new Arguments(requireString(args, "item", description), requirePositiveInt(args, "count", description), upgrade.equals("filter"),
				args.has("client") && args.get("client").getAsBoolean());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.level().getBlockEntity(context.target()) instanceof BackpackBlockEntity block)) {
							throw new IllegalStateException("Expected the restored backpack block at the original position");
						}
						Item expected = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						ItemStack backpack = block.getBackpackWrapper().getBackpack();
						ItemStack contents = block.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
						BackpackWrapper packed = new BackpackWrapper(backpack);
						ItemStack packedContents = packed.getInventoryHandler().getStackInSlot(0);
						ItemStack upgrade = block.getBackpackWrapper().getUpgradeHandler().getStackInSlot(1);
						ItemStack packedUpgrade = packed.getUpgradeHandler().getStackInSlot(1);
						context.putDiagnostic("restoredBackpackItem", BuiltInRegistries.ITEM.getKey(backpack.getItem()).toString());
						context.putDiagnostic("restoredSlot", contents.toString());
						context.putDiagnostic("restoredPackedSlot", packedContents.toString());
						context.putDiagnostic("restoredUpgrade", upgrade.toString());
						context.putDiagnostic("restoredPackedUpgrade", packedUpgrade.toString());
						if (!backpack.is(ModItems.IRON_BACKPACK.get()) || !contents.is(expected) || contents.getCount() != arguments.count()
								|| !packedContents.is(expected) || packedContents.getCount() != arguments.count() || arguments.filterUpgrade()
										&& (!upgrade.is(ModItems.FILTER_UPGRADE.get()) || !packedUpgrade.is(ModItems.FILTER_UPGRADE.get()))) {
							throw new IllegalStateException("Placed backpack lost its tier, upgrade, or stored contents");
						}
					});
				}
				RegressionActionProgress server = await(future);
				if (server != RegressionActionProgress.PASSED || !arguments.client()) {
					return server;
				}
				if (context.minecraft().level.getBlockEntity(context.target()) instanceof BackpackBlockEntity clientBlock) {
					Item expected = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
							.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
					ItemStack clientBackpack = clientBlock.getBackpackWrapper().getBackpack();
					ItemStack clientContents = clientBlock.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
					context.putDiagnostic("clientRestoredBackpackItem", clientBackpack.getItem().toString());
					context.putDiagnostic("clientRestoredBackpackSlot", clientContents.toString());
					if (clientBackpack.is(ModItems.IRON_BACKPACK.get()) && clientContents.is(expected) && clientContents.getCount() == arguments.count()
							&& (!arguments.filterUpgrade()
									|| clientBlock.getBackpackWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.FILTER_UPGRADE.get()))) {
						return RegressionActionProgress.PASSED;
					}
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Restored backpack block contents did not synchronize to the client")
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
