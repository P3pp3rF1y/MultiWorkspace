package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class UseTankSegmentAction extends TypedRegressionAction<UseTankSegmentAction.Arguments> {
	public record Arguments(int x, int y, int z, String face) {
	}
	public UseTankSegmentAction() {
		super("player.useTankSegment", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(PlaceHeldBlockAction.OFFSET_ARGUMENT, "face"), description);
		JsonObject position = new JsonObject();
		if (args.has(PlaceHeldBlockAction.OFFSET_ARGUMENT))
			position.add(PlaceHeldBlockAction.OFFSET_ARGUMENT, args.get(PlaceHeldBlockAction.OFFSET_ARGUMENT));
		PlaceHeldBlockAction.Arguments offset = new PlaceHeldBlockAction().parseArguments(position, description);
		String face = optionalString(args, "face", "up", description);
		if (!Set.of("up", "down", "north", "south", "east", "west").contains(face))
			throw new IllegalArgumentException("Invalid face on " + description);
		return new Arguments(offset.x(), offset.y(), offset.z(), face);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Nullable
			private CompletableFuture<Void> afterUse;
			private int usedAt = -1;
			@Override
			public RegressionActionProgress tick() {
				BlockPos position = context.target().offset(args.x(), args.y(), args.z());
				if (future == null) {
					future = context.onServer(player -> {
						context.journal().recordBlock(player.serverLevel(), position);
						if (player.serverLevel().getBlockEntity(position) instanceof TankBlockEntity tank) {
							TankBlockEntity primary = tank.getPrimary().orElseThrow();
							for (int index = 0; index < primary.getHeight(); index++)
								context.journal().recordBlock(player.serverLevel(), primary.getBlockPos().above(index));
						}
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED)
					return progress;
				if (usedAt < 0) {
					context.minecraft().gameMode.useItemOn(context.minecraft().player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(position), Direction.valueOf(args.face().toUpperCase(Locale.ROOT)), position, false));
					usedAt = context.minecraft().player.tickCount;
				}
				if (context.minecraft().player.tickCount - usedAt < 4)
					return RegressionActionProgress.WAITING;
				if (afterUse == null)
					afterUse = context.onServer(player -> {
						if (player.serverLevel().getBlockEntity(position) instanceof TankBlockEntity tank) {
							context.journal().recordCreatedTankGroup(player.serverLevel(), tank.getPrimary().orElseThrow());
						}
					});
				return await(afterUse);
			}
		};
	}
}
