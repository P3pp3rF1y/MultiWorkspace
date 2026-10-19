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
			private List<ItemStack> serverItems = List.of();
			private ItemStack serverCarried = ItemStack.EMPTY;
			private int deadline = -1;
			private String mismatch = "";
			private String serverState = "no storage menu";
			private String clientState = "no storage screen/menu";
			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						serverMatched = player.containerMenu instanceof StorageContainerMenuBase<?> menu && menu.getColumnsTaken() == arguments.columns()
								&& menu.isUpgradeColumnCountSynced();
						serverState = player.containerMenu instanceof StorageContainerMenuBase<?> menu ? describe(menu) : "no storage menu";
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
				if (context.minecraft().screen instanceof StorageScreenBase<?> screen
						&& context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) {
					long controls = menu.getUpgradeContainers().entrySet().stream()
							.filter(entry -> UpgradeGuiManager
									.getInventoryControl(entry.getKey(), entry.getValue(), new Position(0, 0), menu.getNumberOfRows() * 18, screen).isPresent())
							.count();
					clientState = describe(menu) + ", controls=" + controls;
					if (menu.getColumnsTaken() == arguments.columns() && menu.isUpgradeColumnCountSynced() && controls == arguments.controls()
							&& (!arguments.contents() || contentsMatch(menu))) {
						return RegressionActionProgress.PASSED;
					}
					future = null;
				} else {
					clientState = "screen=" + context.minecraft().screen + ", menu=" + context.minecraft().player.containerMenu;
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Client upgrade columns or inventory controls did not synchronize: server=" + serverState + ", client=" + clientState
								+ (mismatch.isEmpty() ? "" : ", contents=" + mismatch))
						: RegressionActionProgress.WAITING;
			}
			private String describe(StorageContainerMenuBase<?> menu) {
				return "columns=" + menu.getColumnsTaken() + ", synced=" + menu.isUpgradeColumnCountSynced() + ", upgrades=" + menu.getUpgradeContainers()
						.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().getUpgradeWrapper().getUpgradeStack().getItem()).toList();
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
