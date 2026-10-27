package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AssertLinkedItemContentsAction extends TypedRegressionAction<AssertLinkedItemContentsAction.Arguments> {
	public record Arguments(int slot, int count, int timeoutTicks) {
		public Arguments(int slot, int count) {
			this(slot, count, 0);
		}
	}
	public AssertLinkedItemContentsAction() {
		super("assert.linkedItemContents", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "count", "timeoutTicks"), description);
		int slot = optionalNonNegativeInt(args, "slot", 0, description);
		if (slot > 35) {
			throw new IllegalArgumentException("Inventory slot must be between 0 and 35 on " + description);
		}
		return new Arguments(slot, optionalNonNegativeInt(args, "count", 0, description), optionalNonNegativeInt(args, "timeoutTicks", 0, description));
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private final Map<String, Object> observations = new ConcurrentHashMap<>();
			@Nullable
			private CompletableFuture<Void> future;
			private int deadline = -1;
			private int observedCount;
			private boolean observed;
			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0 && arguments.timeoutTicks() > 0) {
					deadline = context.minecraft().player.tickCount + arguments.timeoutTicks();
				}
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack stack = player.getInventory().getItem(arguments.slot());
						LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
						if (endpoint == null || stack.hasTag() && stack.getTag().hasUUID("contentsUuid")
								|| LinkedStorageStackData.getPendingCraft(stack) != null) {
							throw RegressionActionFailure.assertion(
									"Expected a finalized linked item without an ordinary backend at " + arguments.slot() + ": " + stack + ", endpoint="
											+ endpoint + ", ordinary=" + stack.getTag() + ", pending=" + LinkedStorageStackData.getPendingCraft(stack));
						}
						IStorageWrapper host = (IStorageWrapper) LinkedStorageGroupsSavedData.get(player.serverLevel()).manager()
								.resolveVirtualHost(endpoint, false).orElseThrow();
						int count = 0;
						for (int slot = 0; slot < host.getInventoryHandler().getSlots(); slot++) {
							count += host.getInventoryHandler().getStackInSlot(slot).getCount();
						}
						observedCount = count;
						observed = true;
						if (count != arguments.count() && arguments.timeoutTicks() == 0) {
							observations.put("atAssertion", snapshot(player, arguments.slot(), count));
							throw RegressionActionFailure.assertion("Expected canonical item count " + arguments.count() + ", got " + count);
						}
					});
				}
				if (arguments.timeoutTicks() == 0) {
					RegressionActionProgress result = await(future);
					if (result == RegressionActionProgress.FAILED && !observations.isEmpty()) {
						observe(arguments.count(), observations);
					}
					return result;
				}
				if (!future.isDone()) {
					return context.minecraft().player.tickCount >= deadline
							? failAssertion("Canonical item count observation timed out after " + arguments.timeoutTicks() + " ticks; expected "
									+ arguments.count() + ", last observed " + (observed ? observedCount : "none"))
							: RegressionActionProgress.WAITING;
				}
				RegressionActionProgress result = await(future);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (observedCount == arguments.count()) {
					return RegressionActionProgress.PASSED;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return failAssertion("Canonical item count did not reach " + arguments.count() + " in " + arguments.timeoutTicks()
							+ " ticks; last observed " + observedCount);
				}
				future = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}

	private static Map<String, Object> snapshot(ServerPlayer player, int slot, int count) {
		StringBuilder inventory = new StringBuilder();
		for (int i = 0; i < 36; i++) {
			ItemStack item = player.getInventory().getItem(i);
			if (!item.isEmpty()) {
				inventory.append(i).append('=').append(item).append(';');
			}
		}
		return Map.of("canonicalCount", count, "linkedSlot", slot, "inventory", inventory.toString(), "menuId", player.containerMenu.containerId, "cursor",
				player.containerMenu.getCarried().toString());
	}
}
