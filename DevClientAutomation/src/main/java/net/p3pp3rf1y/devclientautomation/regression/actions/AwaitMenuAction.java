package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorageinmotion.common.gui.MovingStorageContainerMenu;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AwaitMenuAction extends TypedRegressionAction<AwaitMenuAction.Arguments> {
	public record Arguments(MenuKind kind, int timeoutTicks, @Nullable String ref) {
	}

	public AwaitMenuAction() {
		super("await.menu", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("kind", "timeoutTicks", "ref"), description);
		MenuKind kind = MenuKind.parse(optionalString(args, "kind", "backpackBlock", description));
		if (kind == MenuKind.MOUNTED) {
			throw new IllegalArgumentException("Argument kind on " + description + " must be backpackBlock, backpackItem, or storageBlock");
		}
		if (kind == MenuKind.MOVING != args.has("ref")) {
			throw new IllegalArgumentException("Argument ref on " + description + " is required exactly for moving menus");
		}
		return new Arguments(kind, optionalPositiveInt(args, "timeoutTicks", 100, description),
				kind == MenuKind.MOVING ? RegressionValueStore.requireName(requireString(args, "ref", description)) : null);
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.kind() == MenuKind.MOVING
				? RegressionValueContract.consuming(arguments.ref(), RegressionValueTypes.MOVING_STORAGE)
				: RegressionValueContract.EMPTY;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	static boolean matchesServerMenu(ServerPlayer player, MenuKind kind, @Nullable String ref, RegressionExecutionContext context) {
		return Execution.matchesServerMenu(player, kind, ref, context);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private int deadline = -1;
		@Nullable
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
				context.putDiagnostic("clientScreen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
				context.putDiagnostic("clientMenu", minecraft.player.containerMenu.getClass().getName());
				return fail(minecraft.screen == null ? "Menu never opened" : "Menu opened but did not synchronize to expected " + arguments.kind());
			}
			return RegressionActionProgress.WAITING;
		}

		static boolean matchesServerMenu(ServerPlayer player, MenuKind kind, @Nullable String ref, RegressionExecutionContext context) {
			return switch (kind) {
				case BACKPACK_BLOCK ->
					player.containerMenu instanceof BackpackContainer menu && menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
							&& menu.getBlockPosition().filter(context.target()::equals).isPresent();
				case BACKPACK_ITEM -> player.containerMenu instanceof BackpackContainer menu
						&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK && menu.getBlockPosition().isEmpty();
				case STORAGE_BLOCK ->
					player.containerMenu instanceof StorageContainerMenu menu && menu.getBlockPosition().filter(context.target()::equals).isPresent();
				case MOVING -> player.containerMenu instanceof MovingStorageContainerMenu<?> menu && menu.getStorageEntity()
						.filter(entity -> entity == context.values().require(ref, MovingStorageEntityHandle.class).requireServer(player)).isPresent();
				default -> false;
			};
		}
	}
}
