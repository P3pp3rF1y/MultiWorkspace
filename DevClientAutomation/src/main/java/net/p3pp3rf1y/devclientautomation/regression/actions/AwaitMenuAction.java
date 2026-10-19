package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;

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
		if (!Set.of("backpackBlock", "backpackItem", "storageBlock", "moving").contains(kind)) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be backpackBlock, backpackItem, or storageBlock");
		}
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

	static boolean matchesServerMenu(net.minecraft.server.level.ServerPlayer player, String kind, String ref, RegressionExecutionContext context) {
		return Execution.matchesServerMenu(player, kind, ref, context);
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
				context.putDiagnostic("clientMenu", minecraft.player.containerMenu.getClass().getName());
				if (serverMenu == null) {
					serverMenu = context.onServer(player -> {
						context.putDiagnostic("serverMenu", player.containerMenu.getClass().getName());
						if (!matchesServerMenu(player, arguments.kind(), arguments.ref(), context)) {
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

		static boolean matchesServerMenu(net.minecraft.server.level.ServerPlayer player, String kind, String ref, RegressionExecutionContext context) {
			return switch (kind) {
				case "backpackBlock" ->
					player.containerMenu instanceof BackpackContainer menu && menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
							&& menu.getBlockPosition().filter(context.target()::equals).isPresent();
				case "backpackItem" -> player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK && menu.getBlockPosition().isEmpty();
				case "storageBlock" ->
					player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(context.target()::equals).isPresent();
				case "moving" -> player.containerMenu instanceof MovingStorageContainerMenu<?> menu && menu.getStorageEntity()
						.filter(entity -> entity == context.values().require(ref, MovingStorageEntityHandle.class).requireServer(player)).isPresent();
				default -> false;
			};
		}
	}
}
