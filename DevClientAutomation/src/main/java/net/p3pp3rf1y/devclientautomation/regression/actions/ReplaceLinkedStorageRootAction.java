package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReplaceLinkedStorageRootAction extends TypedRegressionAction<ReplaceLinkedStorageRootAction.Arguments> {
	public record Arguments() {
	}

	public ReplaceLinkedStorageRootAction() {
		super("fixture.linked.replaceRoot", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return new Arguments();
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						UUID groupId = UUID.fromString((String) context.getDiagnostic("linkedGroup"));
						ILinkedStorageContents contents = LinkedStorageGroupsSavedData.get(player.level()).manager().resolveContents(groupId).orElseThrow();
						contents.setContents(groupId, contents.contents().copy());
					});
				}
				return await(future);
			}
		};
	}
}
