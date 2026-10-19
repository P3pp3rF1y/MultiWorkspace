package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;

import javax.annotation.Nullable;

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
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.serverLevel().getBlockEntity(context.target()) instanceof BackpackBlockEntity backpack)) {
							throw new IllegalStateException("Expected a placed backpack for sided automation");
						}
						Item expected = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, context.target(), Direction.UP);
						if (handler == null || !handler.extractItem(0, 1, true).is(expected)) {
							throw new IllegalStateException("Sided item handler could not extract the stored " + arguments.item());
						}
					});
				}
				return await(future);
			}
		};
	}
}
