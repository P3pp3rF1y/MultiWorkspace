package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;
import net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class RestoreMovingStorageAction extends TypedRegressionAction<RestoreMovingStorageAction.Arguments> {
	public record Arguments(String ref, String saveAs, int offsetX) {
	}

	public RestoreMovingStorageAction() {
		super("fixture.moving.restoreDrop", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "saveAs", "offsetX"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)), optionalNonNegativeInt(args, "offsetX", 2, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (future == null) {
				future = context.onServer(player -> {
					MovingStorageEntityHandle previous = context.values().require(arguments.ref(), MovingStorageEntityHandle.class);
					if (player.serverLevel().getEntity(previous.id()) != null) {
						throw new IllegalStateException("Original moving storage is still present");
					}
					ItemStack drop = player.getInventory().getItem(1);
					var pos = context.target().east(arguments.offsetX() - 2);
					IMovingStorageEntity moving;
					if (drop.is(ModItems.STORAGE_BOAT.get())) {
						moving = new StorageBoat(player.serverLevel(), pos.getX() + .5, pos.getY(), pos.getZ() + .5);
					} else if (drop.is(ModItems.STORAGE_MINECART.get())) {
						moving = new StorageMinecart(player.serverLevel(), pos.getX() + .5, pos.getY(), pos.getZ() + .5);
					} else {
						throw new IllegalStateException("Expected recovered linked moving carrier in player inventory slot 1");
					}
					moving.getStorageHolder().setStorageItemAndCustomNameFromMovingStorageStack(drop, true);
					moving.getStorageHolder().onPlace();
					net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) moving;
					context.journal().recordCreatedEntity(entity);
					if (!player.serverLevel().addFreshEntity(entity)) {
						throw new IllegalStateException("Failed to restore recovered moving storage");
					}
					MovingStorageEntityHandle restored = MovingStorageEntityHandle.of(entity);
					if (!restored.dimension().equals(previous.dimension()) || !restored.type().equals(previous.type())) {
						throw new IllegalStateException("Recovered moving storage changed dimension or type");
					}
					context.values().put(arguments.saveAs(), MovingStorageEntityHandle.class, restored);
					player.getInventory().setItem(1, ItemStack.EMPTY);
				});
			}
			return await(future);
		}
	}
}
