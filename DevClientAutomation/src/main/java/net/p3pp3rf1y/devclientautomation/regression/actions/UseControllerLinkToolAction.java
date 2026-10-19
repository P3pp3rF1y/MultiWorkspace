package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UseControllerLinkToolAction extends TypedRegressionAction<UseControllerLinkToolAction.Arguments> {
	public record Arguments(String target) {
	}

	public UseControllerLinkToolAction() {
		super("player.useControllerLinkTool", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("target"), description);
		String target = requireString(args, "target", description);
		if (!Set.of("controller", "link", "groupController", "firstGroupLink", "secondGroupLink").contains(target)) {
			throw new IllegalArgumentException(
					"Argument target on " + description + " must be controller, link, groupController, firstGroupLink or secondGroupLink");
		}
		return new Arguments(target);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new Execution(context, arguments);
	}

	private static final class Execution extends TickRegressionActionExecution {
		private final Arguments arguments;
		private final AtomicBoolean serverAccepted = new AtomicBoolean();
		private CompletableFuture<Void> check;
		private CompletableFuture<Void> snapshot;
		private boolean used;
		private int deadline = -1;

		private Execution(RegressionExecutionContext context, Arguments arguments) {
			super(context);
			this.arguments = arguments;
		}

		@Override
		public RegressionActionProgress tick() {
			Minecraft minecraft = context.minecraft();
			BlockPos center = context.standingPosition().south(2);
			boolean groupTarget = arguments.target().contains("Group") || arguments.target().equals("groupController");
			BlockPos controllerPos = center.west(groupTarget ? 3 : 2);
			BlockPos linkPos = switch (arguments.target()) {
				case "firstGroupLink" -> center.west().north();
				case "secondGroupLink" -> center.east(3).north();
				default -> center.east();
			};
			boolean controllerTarget = arguments.target().equals("controller") || arguments.target().equals("groupController");
			BlockPos pos = controllerTarget ? controllerPos : linkPos;
			if (deadline < 0) {
				deadline = minecraft.player.tickCount + 100;
			}
			if (!used && minecraft.gameMode != null && minecraft.player.getMainHandItem().getItem() instanceof StorageToolItem
					&& minecraft.level.getBlockEntity(pos) != null) {
				if (snapshot == null) {
					snapshot = context.onServer(player -> {
						context.journal().recordBlock(player.level(), controllerPos);
						context.journal().recordBlock(player.level(), linkPos);
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress recorded = await(snapshot);
				if (recorded != RegressionActionProgress.PASSED) {
					return recorded;
				}
				context.putDiagnostic("toolClick." + arguments.target(), minecraft.gameMode
						.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).toString());
				used = true;
			}
			if (used) {
				if (check == null) {
					check = context.onServer(player -> {
						if (controllerTarget) {
							serverAccepted.set(StorageToolItem.getControllerLink(player.getMainHandItem()).filter(controllerPos::equals).isPresent());
						} else {
							serverAccepted.set(player.level().getBlockEntity(linkPos) instanceof StorageLinkBlockEntity link
									&& link.getControllerPos().filter(controllerPos::equals).isPresent());
							if (player.level().getBlockEntity(controllerPos) instanceof ControllerBlockEntity controller) {
								context.putDiagnostic("toolLink.serverLinkedBlocks." + arguments.target(), controller.getLinkedBlocks().toString());
							}
						}
					});
				}
				RegressionActionProgress result = await(check);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				if (serverAccepted.get() && StorageToolItem.getControllerLink(minecraft.player.getMainHandItem()).filter(controllerPos::equals).isPresent()) {
					return RegressionActionProgress.PASSED;
				}
				check = null;
			}
			return minecraft.player.tickCount >= deadline
					? fail("Storage tool did not " + (used ? "link " : "reach ") + arguments.target())
					: RegressionActionProgress.WAITING;
		}
	}
}
