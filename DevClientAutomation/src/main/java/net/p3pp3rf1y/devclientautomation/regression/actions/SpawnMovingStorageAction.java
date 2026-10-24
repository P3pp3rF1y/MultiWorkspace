package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SpawnMovingStorageAction extends TypedRegressionAction<SpawnMovingStorageAction.Arguments> {
	public record Arguments(String kind, int offsetX, String storageItem, String saveAs) {
	}
	public SpawnMovingStorageAction() {
		super("fixture.moving.spawn", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "offsetX", "storageItem", "saveAs"), description);
		String kind = requireString(args, "kind", description);
		if (!Set.of("boat", "minecart", "llama").contains(kind)) {
			throw new IllegalArgumentException(description + ": kind must be boat, minecart or llama");
		}
		return new Arguments(kind, optionalNonNegativeInt(args, "offsetX", 2, description),
				optionalString(args, "storageItem", "sophisticatedstorage:barrel", description),
				RegressionValueStore.requireName(requireString(args, "saveAs", description)));
	}
	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.MOVING_STORAGE);
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						var pos = context.target().east(arguments.offsetX() - 2);
						if (arguments.kind().equals("minecart")) {
							context.journal().recordBlock(player.serverLevel(), pos.below());
							player.serverLevel().setBlock(pos.below(), Blocks.RAIL.defaultBlockState(), 3);
						} else if (arguments.kind().equals("boat") && player.serverLevel().getBlockState(pos.below()).isAir()) {
							context.journal().recordBlock(player.serverLevel(), pos.below());
							player.serverLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
						}
						IMovingStorageEntity moving;
						if (arguments.kind().equals("llama")) {
							Llama llama = EntityType.LLAMA.create(player.serverLevel());
							if (llama == null) {
								throw new IllegalStateException("Failed to create llama");
							}
							llama.setTamed(true);
							llama.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
							moving = (IMovingStorageEntity) llama;
						} else {
							moving = arguments.kind().equals("boat")
									? new StorageBoat(player.serverLevel(), pos.getX() + .5, pos.getY(), pos.getZ() + .5)
									: new StorageMinecart(player.serverLevel(), pos.getX() + .5, pos.getY(), pos.getZ() + .5);
							var item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.storageItem()))
									.orElseThrow(() -> new IllegalArgumentException("Unknown storage item " + arguments.storageItem()));
							moving.getStorageHolder().setStorageItemFrom(new ItemStack(item), true);
						}
						var entity = (net.minecraft.world.entity.Entity) moving;
						context.journal().recordCreatedEntity(entity);
						if (!player.serverLevel().addFreshEntity(entity)) {
							throw new IllegalStateException("Failed to spawn moving storage " + arguments.kind());
						}
						context.values().put(arguments.saveAs(), MovingStorageEntityHandle.class, MovingStorageEntityHandle.of(entity));
					});
				}
				return await(future);
			}
		};
	}
}
