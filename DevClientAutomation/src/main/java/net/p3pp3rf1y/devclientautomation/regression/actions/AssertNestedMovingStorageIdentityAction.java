package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertNestedMovingStorageIdentityAction extends TypedRegressionAction<AssertNestedMovingStorageIdentityAction.Arguments> {
	public record Arguments(String ref) {
	}

	public AssertNestedMovingStorageIdentityAction() {
		super("assert.movingNestedLinkedIdentity", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						NestedMovingStorageSnapshot snapshot = context.values().require(arguments.ref(), NestedMovingStorageSnapshot.class);
						ItemStack outer = ItemStack.of(snapshot.outerCopy());
						if (LinkedStorageStackData.getEndpoint(outer) != null || NBTHelper.hasTag(outer, StorageWrapper.UUID_TAG)) {
							throw new IllegalStateException("Nested moving carrier has linked or ordinary storage identity on its outer stack");
						}
						ItemStack inner = MovingStorageItem.getStorageItem(outer);
						var endpoint = LinkedStorageStackData.getEndpoint(inner);
						if (endpoint == null || !snapshot.endpoint().equals(endpoint.toString())) {
							throw new IllegalStateException("Nested moving carrier did not preserve its linked endpoint through copy");
						}
						if (NBTHelper.hasTag(inner, StorageWrapper.UUID_TAG)) {
							throw new IllegalStateException("Nested moving carrier assigned an ordinary storage UUID to its linked inner stack");
						}
					});
				}
				return await(future);
			}
		};
	}
}
