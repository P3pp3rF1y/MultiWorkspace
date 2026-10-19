package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AwaitMenuAction extends TypedRegressionAction<AwaitMenuAction.Arguments> {
	public record Arguments(String kind, int timeoutTicks, String ref) {
	}

	public AwaitMenuAction() {
		super("await.menu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "timeoutTicks", "ref"), description);
		String kind = optionalString(args, "kind", "backpackBlock", description);
		RegressionExecutionContext.requireMenuKind(kind, description);
		if (kind.equals("moving") != args.has("ref")) {
			throw new IllegalArgumentException("Argument ref on " + description + " is required exactly for moving menus");
		}
		return new Arguments(kind, optionalPositiveInt(args, "timeoutTicks", 100, description),
				kind.equals("moving") ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private int deadline = -1;
		private CompletableFuture<Void> serverMenu;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + arguments.timeoutTicks();
			}
			if (context.isExpectedMenu(arguments.kind(), arguments.ref())) {
				context.putClientMenuDiagnostic();
				if (serverMenu == null) {
					serverMenu = context.onServer(player -> {
						context.putServerMenuDiagnostic(player);
						if (!isExpectedServerMenu(player, context, arguments)) {
							throw new IllegalStateException("Server menu did not match expected " + arguments.kind() + " identity");
						}
					});
					return RegressionActionProgress.WAITING;
				}
				return await(serverMenu);
			}
			if (minecraft.player.tickCount >= deadline) {
				context.putClientMenuDiagnostic();
				context.putDiagnostic("clientScreen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
				return fail(minecraft.screen == null ? "Menu never opened" : "Menu opened but did not synchronize to expected " + arguments.kind());
			}
			return RegressionActionProgress.WAITING;
		}

		private static boolean isExpectedServerMenu(net.minecraft.server.level.ServerPlayer player, RegressionExecutionContext context, Arguments arguments) {
			if (arguments.kind().equals("moving")) {
				return player.containerMenu instanceof net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu<?> menu
						&& menu.getStorageEntity()
								.filter(entity -> entity == context.values().require(arguments.ref(), MovingStorageEntityHandle.class).requireServer(player))
								.isPresent();
			}
			return context.isExpectedServerMenu(player, arguments.kind());
		}
	}
}
