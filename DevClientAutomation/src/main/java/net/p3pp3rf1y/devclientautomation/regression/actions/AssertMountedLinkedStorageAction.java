package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.client.MountedStorageScreen;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.common.MountedStorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstoragecreateintegration.storage.MountedSophisticatedStorage;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertMountedLinkedStorageAction extends TypedRegressionAction<AssertMountedLinkedStorageAction.Arguments> {
	public record Arguments(String ref, int count, boolean checkPresentation) {
	}

	public AssertMountedLinkedStorageAction() {
		super("assert.createMountedLinkedStorage", "1", Set.of("minecraft:client", "sophisticatedstoragecreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "count", "checkPresentation"), description);
		if (args.has("checkPresentation")
				&& (!args.get("checkPresentation").isJsonPrimitive() || !args.get("checkPresentation").getAsJsonPrimitive().isBoolean())) {
			throw new IllegalArgumentException("Argument checkPresentation on " + description + " must be boolean");
		}
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), optionalNonNegativeInt(args, "count", 0, description),
				!args.has("checkPresentation") || args.get("checkPresentation").getAsBoolean());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private CompletableFuture<Void> future;
		private String groupTitle;
		private int deadline;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			MountedLinkedStorageHandle handle = context.values().require(arguments.ref(), MountedLinkedStorageHandle.class);
			if (future == null) {
				future = context.onServer(player -> {
					var entity = handle.requireServer(player);
					if (!(ContraptionHelper.getMountedStorage(entity, handle.localPos()) instanceof MountedSophisticatedStorage storage)
							|| !Objects.equals(handle.endpoint(), storage.getStorageStack().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
							|| storage.getStorageStack().has(ModCoreDataComponents.STORAGE_UUID) || !LinkedStorageGroupsSavedData.get(player.level()).manager()
									.isEndpointMember(handle.endpoint().groupId(), handle.endpoint().endpointId())) {
						throw new IllegalStateException("Create mount lost its registered linked block endpoint or allocated an ordinary sidecar");
					}
					var stack = storage.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
					if (!stack.is(Items.DIAMOND) || stack.getCount() != arguments.count()) {
						throw new IllegalStateException("Mounted storage does not resolve canonical group inventory");
					}
					groupTitle = storage.getStorageWrapper().getDisplayName().getString();
					if (!entity.handlePlayerInteraction(player, handle.localPos(), Direction.UP, InteractionHand.MAIN_HAND)
							|| !(player.containerMenu instanceof MountedStorageContainerMenu menu)
							|| menu.getContraptionEntity().filter(entity::equals).isEmpty() || !menu.getLocalPos().equals(handle.localPos())) {
						throw new IllegalStateException("Create mounted storage interaction did not open its own menu");
					}
				});
				deadline = context.minecraft().player.tickCount + 100;
			}
			RegressionActionProgress serverProgress = await(future);
			if (serverProgress != RegressionActionProgress.PASSED) {
				return serverProgress;
			}
			if (context.minecraft().player.containerMenu instanceof MountedStorageContainerMenu menu
					&& menu.getContraptionEntity().filter(entity -> entity.getUUID().equals(handle.entityId())).isPresent()
					&& menu.getLocalPos().equals(handle.localPos())) {
				var stack = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
				if (!stack.is(Items.DIAMOND) || stack.getCount() != arguments.count()) {
					return fail("Create mounted client menu did not receive canonical group snapshot");
				}
				if (context.minecraft().gui.screen() instanceof MountedStorageScreen screen) {
					return (!arguments.checkPresentation() || groupTitle.equals(screen.getTitle().getString())
							&& screen.getLinkedStorageEndpointRole().filter(LinkedStorageEndpointRole.PRIMARY::equals).isPresent())
									? RegressionActionProgress.PASSED
									: fail("Create mounted screen title or linked-primary icon does not match the group");
				}
			}
			return context.minecraft().player.tickCount >= deadline ? fail("Create mounted client menu did not synchronize") : RegressionActionProgress.WAITING;
		}
	}
}
