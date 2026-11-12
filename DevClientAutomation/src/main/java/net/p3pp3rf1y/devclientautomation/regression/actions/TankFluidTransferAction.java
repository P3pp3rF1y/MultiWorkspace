package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class TankFluidTransferAction extends TypedRegressionAction<TankFluidTransferAction.Arguments> {
	private static final String SIMULATE_ARGUMENT = "simulate";
	private static final String TRANSFERRED_RESULT = "transferred";

	public record Arguments(int x, int y, int z, String face, String fluid, int amount, int expected, boolean drain, boolean simulate) {
	}
	public TankFluidTransferAction() {
		super("capability.tankFluid", "1", Set.of(CLIENT_CAPABILITY, SophisticatedStorage.MOD_ID));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of(PlaceHeldBlockAction.OFFSET_ARGUMENT, "face", "fluid", "amount", "expected", "drain", SIMULATE_ARGUMENT), description);
		JsonObject position = new JsonObject();
		if (args.has(PlaceHeldBlockAction.OFFSET_ARGUMENT))
			position.add(PlaceHeldBlockAction.OFFSET_ARGUMENT, args.get(PlaceHeldBlockAction.OFFSET_ARGUMENT));
		PlaceHeldBlockAction.Arguments offset = new PlaceHeldBlockAction().parseArguments(position, description);
		String face = optionalString(args, "face", "up", description);
		if (!Set.of("up", "down", "north", "south", "east", "west").contains(face))
			throw new IllegalArgumentException("Invalid face on " + description);
		int amount = requirePositiveInt(args, "amount", description);
		return new Arguments(offset.x(), offset.y(), offset.z(), face, optionalString(args, "fluid", "minecraft:water", description), amount,
				optionalNonNegativeInt(args, "expected", amount, description), optionalBoolean(args, "drain", false, description),
				optionalBoolean(args, SIMULATE_ARGUMENT, false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments args) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			@Override
			public RegressionActionProgress tick() {
				if (future == null)
					future = context.onServer(player -> {
						BlockPos position = context.target().offset(args.x(), args.y(), args.z());
						context.journal().recordBlock(player.serverLevel(), position);
						IFluidHandler handler = player.serverLevel().getCapability(Capabilities.FluidHandler.BLOCK, position,
								Direction.valueOf(args.face().toUpperCase(Locale.ROOT)));
						if (handler == null)
							throw RegressionActionFailure.infrastructure("No native fluid capability at " + position);
						Fluid fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(args.fluid())).orElseThrow();
						var action = args.simulate() ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
						int transferred = args.drain()
								? handler.drain(new FluidStack(fluid, args.amount()), action).getAmount()
								: handler.fill(new FluidStack(fluid, args.amount()), action);
						observe(Map.of(TRANSFERRED_RESULT, args.expected()), Map.of(TRANSFERRED_RESULT, transferred));
						if (transferred != args.expected())
							throw RegressionActionFailure.assertion("Fluid transfer: " + expectedActual());
					});
				return await(future);
			}
		};
	}
}
