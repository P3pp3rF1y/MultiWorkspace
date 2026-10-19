package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

import javax.annotation.Nullable;

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
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.level().getBlockEntity(context.target().east(arguments.offsetX())) instanceof ControllerBlockEntityBase controller)) {
							throw new IllegalStateException("Fixture controller is missing");
						}
						CompoundTag saved = controller.getUpdateTag(player.registryAccess());
						controller.onChunkUnloaded();
						controller.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved));
						controller.onLoad();
					});
				}
				return await(future);
			}
		};
	}
}
