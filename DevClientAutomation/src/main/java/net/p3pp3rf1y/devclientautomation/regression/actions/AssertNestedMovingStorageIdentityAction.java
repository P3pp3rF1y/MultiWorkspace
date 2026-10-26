package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;
import org.jspecify.annotations.Nullable;

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
	protected RegressionValueContract values(Arguments arguments) {
		return RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.NESTED_STORAGE);
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
						NestedMovingStorageSnapshot snapshot = context.values().require(arguments.ref(), NestedMovingStorageSnapshot.class);
						ItemStack outer = snapshot.outerCopy();
						if (outer.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) || outer.has(ModCoreDataComponents.STORAGE_UUID)) {
							throw RegressionActionFailure.assertion("Nested moving carrier has linked or ordinary storage identity on its outer stack");
						}
						ItemStack inner = MovingStorageItem.getStorageItem(outer);
						var endpoint = inner.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint == null || !snapshot.endpoint().equals(endpoint.toString())) {
							throw RegressionActionFailure.assertion("Nested moving carrier did not preserve its linked endpoint through copy");
						}
						if (inner.has(ModCoreDataComponents.STORAGE_UUID)) {
							throw RegressionActionFailure.assertion("Nested moving carrier assigned an ordinary storage UUID to its linked inner stack");
						}
					});
				}
				return await(future);
			}
		};
	}
}
