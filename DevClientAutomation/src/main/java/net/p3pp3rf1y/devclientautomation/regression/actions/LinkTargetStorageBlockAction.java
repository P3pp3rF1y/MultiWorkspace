package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class LinkTargetStorageBlockAction extends TypedRegressionAction<LinkTargetStorageBlockAction.Arguments> {
	public record Arguments(String saveAs) {
	}

	public LinkTargetStorageBlockAction() {
		super("storage.linkBlock", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("saveAs"), description);
		return new Arguments(optionalString(args, "saveAs", "", description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.saveAs().isEmpty()
				? RegressionValueContract.EMPTY
				: RegressionValueContract.producing(arguments.saveAs(), RegressionValueTypes.ITEM_STACK);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Nullable
			private LinkedStorageEndpointData linkedEndpoint;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.level().getBlockEntity(context.target()) instanceof StorageBlockEntity storage)) {
							throw new IllegalStateException("Target is not a storage block");
						}
						ItemStack linker = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("sophisticatedcore:ender_linker")));
						if (LinkedStorageService.linkWithResult(player.serverLevel(), player.getUUID(), linker,
								storage) != LinkedStorageService.LinkResult.SUCCESS) {
							throw new IllegalStateException("Could not link target storage block");
						}
						linkedEndpoint = storage.getLinkedStorageEndpointData();
						if (linkedEndpoint == null) {
							throw new IllegalStateException("Linked target storage block has no endpoint");
						}
						if (!arguments.saveAs().isEmpty()) {
							context.values().put(arguments.saveAs(), ItemStack.class, linker);
						}
						context.putDiagnostic("linkedGroup", linkedEndpoint.groupId().toString());
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (linkedEndpoint != null && context.minecraft().level != null
						&& context.minecraft().level.getBlockEntity(context.target()) instanceof StorageBlockEntity storage
						&& linkedEndpoint.equals(storage.getLinkedStorageEndpointData())) {
					return RegressionActionProgress.PASSED;
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Linked storage block endpoint did not synchronize to client")
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
