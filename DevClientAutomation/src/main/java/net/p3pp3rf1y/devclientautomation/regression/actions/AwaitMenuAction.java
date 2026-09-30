package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AwaitMenuAction extends TypedRegressionAction<AwaitMenuAction.Arguments> {
	public record Arguments(String kind, int timeoutTicks) {
	}

	public AwaitMenuAction() {
		super("await.menu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "timeoutTicks"), description);
		String kind = optionalString(args, "kind", "backpackBlock", description);
		if (!Set.of("backpackBlock", "backpackItem", "storageBlock").contains(kind)) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be backpackBlock, backpackItem, or storageBlock");
		}
		return new Arguments(kind, optionalPositiveInt(args, "timeoutTicks", 100, description));
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
			if (context.isExpectedMenu(arguments.kind())) {
				context.putDiagnostic("clientMenu", minecraft.player.containerMenu.getClass().getName());
				if (serverMenu == null) {
					serverMenu = context.onServer(player -> {
						context.putDiagnostic("serverMenu", player.containerMenu.getClass().getName());
						if (!matchesServerMenu(player, arguments.kind(), context)) {
							throw new IllegalStateException("Server menu did not match expected " + arguments.kind() + " identity");
						}
					});
					return RegressionActionProgress.WAITING;
				}
				return await(serverMenu);
			}
			if (minecraft.player.tickCount >= deadline) {
				context.putDiagnostic("clientScreen", minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getName());
				context.putDiagnostic("clientMenu", minecraft.player.containerMenu.getClass().getName());
				return fail(minecraft.gui.screen() == null ? "Menu never opened" : "Menu opened but did not synchronize to expected " + arguments.kind());
			}
			return RegressionActionProgress.WAITING;
		}

		private static boolean matchesServerMenu(net.minecraft.server.level.ServerPlayer player, String kind, RegressionExecutionContext context) {
			return switch (kind) {
				case "backpackBlock" ->
					player.containerMenu instanceof BackpackContainer menu && menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
							&& menu.getBlockPosition().filter(context.target()::equals).isPresent();
				case "backpackItem" -> player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK && menu.getBlockPosition().isEmpty();
				case "storageBlock" ->
					player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(context.target()::equals).isPresent();
				default -> false;
			};
		}
	}
}
