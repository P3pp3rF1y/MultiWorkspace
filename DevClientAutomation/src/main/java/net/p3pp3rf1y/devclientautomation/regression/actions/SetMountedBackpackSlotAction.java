package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpackscreateintegration.backpack.MountedSophisticatedBackpack;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SetMountedBackpackSlotAction extends TypedRegressionAction<SetMountedBackpackSlotAction.Arguments> {
	public record Arguments(String ref, String item, int count) {
	}

	public SetMountedBackpackSlotAction() {
		super("fixture.create.setMountedBackpackSlot", "1", Set.of("minecraft:client", "sophisticatedbackpackscreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "item", "count"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), requireString(args, "item", description),
				requirePositiveInt(args, "count", description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.UUID);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						UUID id = context.values().require(arguments.ref(), UUID.class);
						if (!(player.level().getEntity(id) instanceof AbstractContraptionEntity entity)) {
							throw new IllegalStateException("Mounted Create backpack contraption is missing");
						}
						MountedSophisticatedBackpack storage = ContraptionHelper.getMountedItemStorages(entity).keySet().stream()
								.map(pos -> ContraptionHelper.getMountedStorage(entity, pos)).filter(MountedSophisticatedBackpack.class::isInstance)
								.map(MountedSophisticatedBackpack.class::cast).findFirst()
								.orElseThrow(() -> new IllegalStateException("Mounted sophisticated backpack is missing"));
						Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						storage.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(item, arguments.count()));
					});
				}
				return await(future);
			}
		};
	}
}
