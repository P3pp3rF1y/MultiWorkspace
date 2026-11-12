package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class UseTankSegmentAction extends TypedRegressionAction<UseTankSegmentAction.Arguments> {
	private static final String OFFSET_ARGUMENT = "offset";
	public record Arguments(int x, int y, int z, String face) {
	}

	public UseTankSegmentAction() {
		super("player.useTankSegment", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(OFFSET_ARGUMENT, "face"), description);
		JsonObject position = new JsonObject();
		if (args.has(OFFSET_ARGUMENT))
			position.add(OFFSET_ARGUMENT, args.get(OFFSET_ARGUMENT));
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
			private ItemStack expectedHeld = ItemStack.EMPTY;
			private int deadline = -1;
			private int usedAt = -1;

			@Override
			public RegressionActionProgress tick() {
				BlockPos position = context.target().offset(args.x(), args.y(), args.z());
				if (future == null) {
					deadline = context.minecraft().player.tickCount + 100;
					future = context.onServer(player -> {
						expectedHeld = player.getMainHandItem().copy();
						context.journal().recordBlock(player.level(), position);
						if (player.level().getBlockEntity(position) instanceof TankBlockEntity tank) {
							TankBlockEntity primary = tank.getPrimary().orElseThrow();
							for (int index = 0; index < primary.getHeight(); index++)
								context.journal().recordBlock(player.level(), primary.getBlockPos().above(index));
						}
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED)
					return progress;
				if (usedAt < 0) {
					ItemStack held = context.minecraft().player.getMainHandItem();
					observe(Map.of("heldItem", expectedHeld.toString()), Map.of("heldItem", held.toString()));
					if (!ItemStack.matches(expectedHeld, held)) {
						return context.minecraft().player.tickCount >= deadline
								? fail("Tank interaction main-hand item did not synchronize")
								: RegressionActionProgress.WAITING;
					}
					InteractionResult result = context.minecraft().gameMode.useItemOn(context.minecraft().player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(position), Direction.valueOf(args.face().toUpperCase(Locale.ROOT)), position, false));
					observe(Map.of("heldItem", expectedHeld.toString()), Map.of("heldItem", held.toString(), "interactionResult", result.toString()));
					usedAt = context.minecraft().player.tickCount;
				}
				if (context.minecraft().player.tickCount - usedAt < 4)
					return RegressionActionProgress.WAITING;
				if (afterUse == null)
					afterUse = context.onServer(player -> {
						if (player.level().getBlockEntity(position) instanceof TankBlockEntity tank) {
							context.journal().recordCreatedTankGroup(player.level(), tank.getPrimary().orElseThrow());
						}
					});
				return await(afterUse);
			}
		};
	}
}
