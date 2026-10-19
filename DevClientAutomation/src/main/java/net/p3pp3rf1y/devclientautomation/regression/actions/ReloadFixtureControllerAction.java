package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ReloadFixtureControllerAction extends TypedRegressionAction<ReloadFixtureControllerAction.Arguments> {
	public record Arguments(int offsetX) {
	}
	public ReloadFixtureControllerAction() {
		super("fixture.controller.reload", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offsetX"), description);
		return new Arguments(optionalNonNegativeInt(args, "offsetX", 1, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.serverLevel()
								.getBlockEntity(context.target().east(arguments.offsetX())) instanceof ControllerBlockEntityBase controller)) {
							throw new IllegalStateException("Fixture controller is missing");
						}
						CompoundTag saved = controller.getUpdateTag();
						controller.onChunkUnloaded();
						controller.load(saved);
						controller.onLoad();
					});
				}
				return await(future);
			}
		};
	}
}
