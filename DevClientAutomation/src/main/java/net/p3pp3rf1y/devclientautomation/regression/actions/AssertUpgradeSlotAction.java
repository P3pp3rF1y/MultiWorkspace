package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertUpgradeSlotAction extends TypedRegressionAction<AssertUpgradeSlotAction.Arguments> {
	public record Arguments(int upgrade, int slot, String item, int count, int timeoutTicks) {
	}

	public AssertUpgradeSlotAction() {
		super("assert.upgradeSlot", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("upgrade", "slot", "item", "count", "timeoutTicks"), description);
		int timeout = optionalNonNegativeInt(args, "timeoutTicks", 100, description);
		if (timeout == 0) {
			throw new IllegalArgumentException(description + " timeoutTicks must be positive");
		}
		return new Arguments(optionalNonNegativeInt(args, "upgrade", 0, description), optionalNonNegativeInt(args, "slot", 0, description),
				requireString(args, "item", description), optionalNonNegativeInt(args, "count", 1, description), timeout);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> serverStack;
			private ItemStack observedServerStack = ItemStack.EMPTY;
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				ticks++;
				if (serverStack == null) {
					serverStack = context.onServer(player -> observedServerStack = stack(player.containerMenu).copy());
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(serverStack);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				ItemStack server = observedServerStack;
				ItemStack client = stack(context.minecraft().player.containerMenu);
				observe(Map.of("upgrade", arguments.upgrade(), "slot", arguments.slot(), "item", arguments.item(), "count", arguments.count()),
						Map.of("server", server.toString(), "client", client.toString(), "elapsedTicks", ticks));
				if (matches(server) && matches(client)) {
					return RegressionActionProgress.PASSED;
				}
				serverStack = null;
				return ticks >= arguments.timeoutTicks()
						? failAssertion("Upgrade slot did not synchronize expected " + arguments.count() + " " + arguments.item() + "; server=" + server
								+ ", client=" + client)
						: RegressionActionProgress.WAITING;
			}

			private ItemStack stack(AbstractContainerMenu container) {
				if (!(container instanceof StorageContainerMenuBase<?> menu)) {
					throw RegressionActionFailure.infrastructure("Storage menu is not active for upgrade slot assertion");
				}
				UpgradeContainerBase<?, ?> upgrade = menu.getUpgradeContainers().get(arguments.upgrade());
				if (upgrade == null || arguments.slot() >= upgrade.getSlots().size()) {
					throw RegressionActionFailure.infrastructure("Menu has no upgrade " + arguments.upgrade() + " slot " + arguments.slot());
				}
				return upgrade.getSlots().get(arguments.slot()).getItem();
			}

			private boolean matches(ItemStack stack) {
				return arguments.count() == 0
						? stack.isEmpty()
						: BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(arguments.item()) && stack.getCount() == arguments.count();
			}
		};
	}
}
