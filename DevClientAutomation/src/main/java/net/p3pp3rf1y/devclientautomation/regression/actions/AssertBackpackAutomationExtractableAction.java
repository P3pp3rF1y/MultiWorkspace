package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
						var level = player.level();
						if (!(level.getBlockEntity(context.target()) instanceof BackpackBlockEntity)) {
							throw new IllegalStateException("Expected a placed backpack for sided automation");
						}
						Item expected = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown item " + arguments.item()));
						var handler = level.getCapability(Capabilities.Item.BLOCK, context.target(), Direction.UP);
						if (handler == null) {
							throw new IllegalStateException("Placed backpack has no sided item capability");
						}
						int extracted;
						try (Transaction transaction = Transaction.openRoot()) {
							extracted = handler.extract(0, ItemResource.of(new ItemStack(expected)), 1, transaction);
						}
						if (extracted != 1) {
							throw new IllegalStateException("Sided item handler could not extract the stored " + arguments.item());
						}
					});
				}
				return await(future);
			}
		};
	}
}
