package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedBackpackHostAction extends TypedRegressionAction<AssertLinkedBackpackHostAction.Arguments> {
	public record Arguments(int minimumSlots, boolean placed, int offsetZ) {
	}

	public AssertLinkedBackpackHostAction() {
		super("assert.linkedBackpackHost", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("minimumSlots", "placed", "offsetZ"), description);
		if (args.has("placed") && (!args.get("placed").isJsonPrimitive() || !args.get("placed").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument placed on " + description + " must be boolean");
		}
		return new Arguments(optionalPositiveInt(args, "minimumSlots", 1, description), args.has("placed") && args.get("placed").getAsBoolean(),
				optionalNonNegativeInt(args, "offsetZ", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private int slots;
			@Nullable
			private String storageType;
			@Nullable
			private Class<?> settingsType;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack stack = player.getMainHandItem();
						if (arguments.placed()) {
							if (!(player.level().getBlockEntity(context.target().south(arguments.offsetZ())) instanceof BackpackBlockEntity backpack)) {
								throw RegressionActionFailure.infrastructure("Expected a placed Backpack secondary");
							}
							stack = backpack.getBackpackWrapper().getBackpack();
						}
						LinkedStorageEndpointData endpoint = stack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
						if (endpoint != null) {
							context.putDiagnostic("linkedGroup", endpoint.groupId().toString());
						}
						if (endpoint == null || stack.has(ModCoreDataComponents.STORAGE_UUID)) {
							throw RegressionActionFailure.assertion("Backpack secondary has no endpoint or retained an ordinary backend");
						}
						LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(player.level()).manager();
						IStorageWrapper host = (IStorageWrapper) manager.resolveVirtualHost(endpoint, false).orElseThrow();
						IBackpackWrapper facade = BackpackLinkedStorageResolver.resolveCanonicalHost(player.level(), stack).orElseThrow();
						if (manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId()) || facade.getInventoryHandler() != host.getInventoryHandler()
								|| facade.getUpgradeHandler() != host.getUpgradeHandler() || facade.getSettingsHandler() != host.getSettingsHandler()) {
							throw RegressionActionFailure.assertion("Backpack secondary does not delegate to the native canonical host");
						}
						slots = host.getInventoryHandler().size();
						storageType = host.getStorageType();
						settingsType = host.getSettingsHandler().getClass();
						if (slots < arguments.minimumSlots()) {
							throw RegressionActionFailure.assertion("Canonical inventory has only " + slots + " slots");
						}
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (!(context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu)
						|| menu.getStorageWrapper().getInventoryHandler().size() != slots || !menu.getStorageWrapper().getStorageType().equals(storageType)
						|| menu.getStorageWrapper().getSettingsHandler().getClass() != settingsType) {
					if (deadline < 0) {
						deadline = context.minecraft().player.tickCount + 100;
					}
					return context.minecraft().player.tickCount >= deadline
							? failAssertion("Client Backpack menu did not use the native host profile and settings platform")
							: RegressionActionProgress.WAITING;
				}
				if (arguments.placed() && (!(menu instanceof BackpackContainer backpackMenu)
						|| backpackMenu.getBlockPosition().filter(context.target().south(arguments.offsetZ())::equals).isEmpty())) {
					return fail("Client menu does not belong to the placed Backpack secondary");
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
