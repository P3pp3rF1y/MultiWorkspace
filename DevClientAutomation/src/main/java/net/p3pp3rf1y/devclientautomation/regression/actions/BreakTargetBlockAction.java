package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BreakTargetBlockAction extends TypedRegressionAction<BreakTargetBlockAction.Arguments> {
	public record Arguments(int x, int y, int z, boolean broken) {
	}

	public BreakTargetBlockAction() {
		super("player.breakTargetBlock", "2", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "broken"), description);
		JsonObject positionArgs = new JsonObject();
		if (args.has("offset")) {
			positionArgs.add("offset", args.get("offset"));
		}
		var offset = new PlaceHeldBlockAction().parseArguments(positionArgs, description);
		return new Arguments(offset.x(), offset.y(), offset.z(), optionalBoolean(args, "broken", true, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private final BlockPos target = context.target().offset(arguments.x(), arguments.y(), arguments.z());
			private final AABB bounds = new AABB(target).inflate(3);
			@Nullable
			private CompletableFuture<Void> future;
			private Set<UUID> previousDrops = Set.of();
			private boolean prepared;
			private boolean started;
			private boolean observedMining;
			private boolean miningFinished;
			private boolean serverBroken;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = Minecraft.getInstance();
				if (deadline < 0) {
					deadline = minecraft.player.tickCount + 200;
				}
				if (!prepared) {
					if (future == null) {
						future = context.onServer(player -> {
							if (player.serverLevel().getBlockState(target).isAir() || player.isShiftKeyDown()) {
								throw RegressionActionFailure.infrastructure("Break requires a non-air target and a non-sneaking player");
							}
							context.journal().recordBlock(player.serverLevel(), target);
							for (Direction direction : Direction.Plane.HORIZONTAL) {
								context.journal().recordBlock(player.serverLevel(), target.relative(direction));
							}
							previousDrops = player.serverLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream().map(Entity::getUUID)
									.collect(Collectors.toSet());
						});
					}
					RegressionActionProgress result = await(future);
					if (result != RegressionActionProgress.PASSED) {
						return result;
					}
					prepared = true;
					future = null;
				}
				if (!started) {
					minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target));
					if (!(minecraft.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(target)) {
						return minecraft.player.tickCount >= deadline
								? failAssertion("Player did not aim at break target " + target)
								: RegressionActionProgress.WAITING;
					}
					VirtualInput.pressMouseButton(0);
					started = true;
				}
				boolean clientBroken = minecraft.level != null && minecraft.level.getBlockState(target).isAir();
				boolean mining = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
				observedMining |= mining;
				if (!miningFinished && (clientBroken || observedMining && !mining)) {
					VirtualInput.releaseMouseButton();
					miningFinished = true;
				}
				if (future == null) {
					future = context.onServer(player -> {
						context.journal().recordNewItemDrops(player.serverLevel(), bounds, previousDrops);
						serverBroken = player.serverLevel().getBlockState(target).isAir();
					});
				}
				RegressionActionProgress result = await(future);
				if (result != RegressionActionProgress.PASSED) {
					return result;
				}
				future = null;
				observe(Map.of("broken", arguments.broken()),
						Map.of("serverBroken", serverBroken, "clientBroken", clientBroken, "miningFinished", miningFinished, "target", target.toString()));
				if (miningFinished && serverBroken == arguments.broken() && clientBroken == arguments.broken()) {
					return RegressionActionProgress.PASSED;
				}
				return minecraft.player.tickCount >= deadline
						? failAssertion("Block break did not converge: " + expectedActual())
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
