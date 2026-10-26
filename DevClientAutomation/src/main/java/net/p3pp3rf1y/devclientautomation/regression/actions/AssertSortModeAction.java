package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertSortModeAction extends TypedRegressionAction<AssertSortModeAction.Arguments> {
	public record Arguments(String mode) {
	}

	public AssertSortModeAction() {
		super("assert.sortMode", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("mode"), description);
		String mode = requireString(args, "mode", description);
		if (!Set.of("NAME", "MOD", "COUNT", "TAGS").contains(mode)) {
			throw new IllegalArgumentException(description + ": mode must be NAME, MOD, COUNT, or TAGS");
		}
		return new Arguments(mode);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> serverMode;
			private SortBy actualServerMode = SortBy.NAME;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (!(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
					return fail("Storage inventory menu is not open on client");
				}
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (serverMode == null) {
					serverMode = context.onServer(player -> {
						if (!(player.containerMenu instanceof StorageContainerMenuBase<?> serverMenu)) {
							throw RegressionActionFailure.infrastructure("Storage inventory menu is not open on server");
						}
						actualServerMode = serverMenu.getSortBy();
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(serverMode);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (actualServerMode.name().equals(arguments.mode()) && menu.getSortBy().name().equals(arguments.mode())) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return failAssertion("Expected sort mode " + arguments.mode() + ", server=" + actualServerMode + ", client=" + menu.getSortBy());
				}
				serverMode = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
