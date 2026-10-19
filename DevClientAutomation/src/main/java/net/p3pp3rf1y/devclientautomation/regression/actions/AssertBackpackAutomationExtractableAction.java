package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertBackpackAutomationExtractableAction extends TypedRegressionAction<AssertBackpackAutomationExtractableAction.Arguments> {
	public record Arguments(String item) {
	}

	public AssertBackpackAutomationExtractableAction() {
		super("assert.backpackAutomationExtractable", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item"), description);
		return new Arguments(requireString(args, "item", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.serverLevel().getBlockEntity(context.target()) instanceof BackpackBlockEntity backpack)) {
							throw new IllegalStateException("Expected a placed backpack for sided automation");
						}
						Item expected = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						ItemStack extractable = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
								.map(handler -> handler.extractItem(0, 1, true)).orElse(ItemStack.EMPTY);
						if (!extractable.is(expected) || extractable.getCount() != 1) {
							throw new IllegalStateException("Sided item handler could not extract the stored " + arguments.item());
						}
					});
				}
				return await(future);
			}
		};
	}
}
