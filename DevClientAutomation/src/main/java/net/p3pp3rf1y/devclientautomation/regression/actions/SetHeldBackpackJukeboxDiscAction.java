package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetHeldBackpackJukeboxDiscAction extends TypedRegressionAction<SetHeldBackpackJukeboxDiscAction.Arguments> {
	public record Arguments(String item, int slot, String repeat, boolean configureShuffle, boolean shuffle) {
	}

	public SetHeldBackpackJukeboxDiscAction() {
		super("fixture.backpack.setHeldJukeboxDisc", "2", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item", "slot", "repeat", "shuffle"), description);
		String repeat = optionalString(args, "repeat", "", description);
		if (!repeat.isEmpty() && !Set.of("all", "one", "no").contains(repeat)) {
			throw new IllegalArgumentException("Argument repeat on " + description + " must be all, one, or no");
		}
		return new Arguments(requireString(args, "item", description), optionalNonNegativeInt(args, "slot", 0, description), repeat, args.has("shuffle"),
				optionalBoolean(args, "shuffle", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown disc " + arguments.item()));
						var jukebox = BackpackWrapper.fromStack(player.getMainHandItem()).getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream()
								.findFirst().orElseThrow(() -> new IllegalStateException("Held backpack has no jukebox upgrade"));
						if (arguments.slot() >= jukebox.getDiscInventory().size()) {
							throw new IllegalArgumentException("Jukebox upgrade has no disc slot " + arguments.slot());
						}
						try (Transaction transaction = Transaction.openRoot()) {
							if (jukebox.getDiscInventory().insert(arguments.slot(), ItemResource.of(new ItemStack(item)), 1, transaction) != 1) {
								throw new IllegalStateException("Jukebox upgrade rejected fixture disc " + arguments.item());
							}
							transaction.commit();
						}
						if (!arguments.repeat().isEmpty()) {
							jukebox.setRepeatMode(RepeatMode.fromName(arguments.repeat()));
						}
						if (arguments.configureShuffle()) {
							jukebox.setShuffleEnabled(arguments.shuffle());
						}
						player.inventoryMenu.broadcastFullState();
					});
				}
				return await(future);
			}
		};
	}
}
