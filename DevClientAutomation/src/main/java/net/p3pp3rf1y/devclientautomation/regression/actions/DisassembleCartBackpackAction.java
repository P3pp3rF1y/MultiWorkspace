package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DisassembleCartBackpackAction extends TypedRegressionAction<DisassembleCartBackpackAction.Arguments> {
	public record Arguments(String ref) {
	}

	public DisassembleCartBackpackAction() {
		super("create.disassembleBackpack", "1", Set.of("minecraft:client", "sophisticatedbackpackscreateintegration"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;
			private int readyAt = -1;

			@Override
			public RegressionActionProgress tick() {
				if (readyAt < 0) {
					readyAt = context.minecraft().player.tickCount + 10;
				}
				if (context.minecraft().player.tickCount < readyAt) {
					return RegressionActionProgress.WAITING;
				}
				if (future == null) {
					future = context.onServer(player -> {
						UUID id = context.values().require(arguments.ref(), UUID.class);
						if (!(player.serverLevel().getEntity(id) instanceof AbstractContraptionEntity entity)) {
							throw new IllegalStateException("Mounted Create backpack contraption is missing");
						}
						if (entity.getContraption().getBlocks().values().stream()
								.noneMatch(info -> info.state().getBlock() instanceof BackpackBlock && info.nbt() == null)) {
							throw new IllegalStateException("Mounted backpack actor did not clear the captured block NBT");
						}
						BlockPos assemblerPos = context.target().below();
						player.serverLevel().setBlock(assemblerPos.west(), Blocks.AIR.defaultBlockState(), 3);
						player.serverLevel().setBlock(assemblerPos,
								player.serverLevel().getBlockState(assemblerPos).setValue(CartAssemblerBlock.POWERED, false), 3);
						entity.disassemble();
					});
				}
				return await(future);
			}
		};
	}
}
