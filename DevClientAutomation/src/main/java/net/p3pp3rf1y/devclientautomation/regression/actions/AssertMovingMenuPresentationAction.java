package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorageinmotion.client.gui.MovingStorageScreen;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMovingMenuPresentationAction extends TypedRegressionAction<AssertMovingMenuPresentationAction.Arguments> {
	public record Arguments(String ref, String role) {
	}

	public AssertMovingMenuPresentationAction() {
		super("assert.movingMenuPresentation", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "role"), description);
		String role = requireString(args, "role", description);
		if (!Set.of("primary", "secondary").contains(role)) {
			throw new IllegalArgumentException("Argument role on " + description + " must be primary or secondary");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), role);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> serverAssertion;
		private String groupTitle;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			if (serverAssertion == null) {
				serverAssertion = context.onServer(player -> {
					var entity = context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player);
					if (!(player.containerMenu instanceof MovingStorageContainerMenu<?> menu) || menu.getStorageEntity().filter(entity::equals).isEmpty()
							|| !(menu.getStorageWrapper() instanceof StorageLinkedStorageHostWrapper host)) {
						throw new IllegalStateException("Expected linked moving menu for exact referenced entity");
					}
					groupTitle = host.getDisplayName().getString();
				});
				deadline = context.minecraft().player.tickCount + 100;
			}
			RegressionActionProgress serverProgress = await(serverAssertion);
			if (serverProgress != RegressionActionProgress.PASSED) {
				return serverProgress;
			}
			if (context.minecraft().gui.screen() instanceof MovingStorageScreen screen && context.isExpectedMenu("moving", arguments.ref())) {
				LinkedStorageEndpointRole expectedRole = arguments.role().equals("primary")
						? LinkedStorageEndpointRole.PRIMARY
						: LinkedStorageEndpointRole.SECONDARY;
				if (!groupTitle.equals(screen.getTitle().getString()) || !screen.getLinkedStorageEndpointRole().filter(expectedRole::equals).isPresent()) {
					return fail("Moving menu title or endpoint icon does not match the canonical group and installed role");
				}
				return RegressionActionProgress.PASSED;
			}
			return context.minecraft().player.tickCount >= deadline ? fail("Moving linked screen did not synchronize") : RegressionActionProgress.WAITING;
		}
	}
}
