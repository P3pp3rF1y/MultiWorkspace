package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMenuUpgradeLayoutAction extends TypedRegressionAction<AssertMenuUpgradeLayoutAction.Arguments> {
	public record Arguments(int columns, int controls, boolean contents) {
	}

	public AssertMenuUpgradeLayoutAction() {
		super("assert.menuUpgradeLayout", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("columns", "controls", "contents"), description);
		if (args.has("contents") && (!args.get("contents").isJsonPrimitive() || !args.get("contents").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument contents must be boolean on " + description);
		}
		return new Arguments(optionalNonNegativeInt(args, "columns", 0, description), optionalNonNegativeInt(args, "controls", 0, description),
				args.has("contents") && args.get("contents").getAsBoolean());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private boolean serverMatched;
			private String serverState = "not sampled";
			private List<ItemStack> serverItems = List.of();
			private ItemStack serverCarried = ItemStack.EMPTY;
			private int deadline = -1;
			private String mismatch = "";

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						serverMatched = player.containerMenu instanceof StorageContainerMenuBase<?> menu && menu.getColumnsTaken() == arguments.columns()
								&& menu.isUpgradeColumnCountSynced();
						serverState = player.containerMenu instanceof StorageContainerMenuBase<?> menu
								? menu.getClass().getSimpleName() + " columns=" + menu.getColumnsTaken() + " synced=" + menu.isUpgradeColumnCountSynced()
								: player.containerMenu.getClass().getSimpleName();
						if (serverMatched && arguments.contents()) {
							serverItems = player.containerMenu.getItems().stream().map(ItemStack::copy).toList();
							serverCarried = player.containerMenu.getCarried().copy();
						}
					});
				}
				RegressionActionProgress server = await(future);
				if (server != RegressionActionProgress.PASSED) {
					return server;
				}
				if (!serverMatched) {
					future = null;
					return context.minecraft().player.tickCount >= deadline
							? fail("Server upgrade columns did not match " + arguments.columns() + ": " + serverState)
							: RegressionActionProgress.WAITING;
				}
				if (context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen
						&& context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu && menu.getColumnsTaken() == arguments.columns()
						&& menu.isUpgradeColumnCountSynced()) {
					long controls = menu.getUpgradeContainers().entrySet().stream()
							.filter(entry -> UpgradeGuiManager
									.getInventoryControl(entry.getKey(), entry.getValue(), new Position(0, 0), menu.getNumberOfRows() * 18, screen).isPresent())
							.count();
					boolean contentsMatch = !arguments.contents() || contentsMatch(menu);
					mismatch = "screen=" + screen.getClass().getSimpleName() + ", menu=" + menu.getClass().getSimpleName() + ", columns="
							+ menu.getColumnsTaken() + "/" + arguments.columns() + ", synced=" + menu.isUpgradeColumnCountSynced() + ", controls=" + controls
							+ "/" + arguments.controls() + ", contents=" + contentsMatch + ", server=" + serverState
							+ (contentsMatch ? "" : ", contentsMismatch=" + mismatch);
					if (controls == arguments.controls() && contentsMatch) {
						return RegressionActionProgress.PASSED;
					}
					future = null;
				} else {
					mismatch = "screen=" + (context.minecraft().gui.screen() == null ? "null" : context.minecraft().gui.screen().getClass().getSimpleName())
							+ ", menu=" + context.minecraft().player.containerMenu.getClass().getSimpleName() + ", server=" + serverState;
					if (context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) {
						mismatch += ", columns=" + menu.getColumnsTaken() + "/" + arguments.columns() + ", synced=" + menu.isUpgradeColumnCountSynced();
					}
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Client upgrade columns or inventory controls did not synchronize: " + mismatch)
						: RegressionActionProgress.WAITING;
			}

			private boolean contentsMatch(StorageContainerMenuBase<?> menu) {
				List<ItemStack> items = menu.getItems();
				if (items.size() != serverItems.size() || !sameStack(menu.getCarried(), serverCarried)) {
					mismatch = "slots=" + items.size() + "/" + serverItems.size() + ", carried=" + menu.getCarried() + "/" + serverCarried;
					return false;
				}
				for (int slot = 0; slot < items.size(); slot++) {
					if (!sameStack(items.get(slot), serverItems.get(slot))) {
						mismatch = "slot=" + slot + ", client=" + items.get(slot) + " " + items.get(slot).getComponentsPatch() + ", server="
								+ serverItems.get(slot) + " " + serverItems.get(slot).getComponentsPatch();
						return false;
					}
				}
				return true;
			}

			private boolean sameStack(ItemStack first, ItemStack second) {
				return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
			}
		};
	}
}
