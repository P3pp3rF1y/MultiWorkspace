package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
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
						Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown disc " + arguments.item()));
						ItemStack backpack = player.getMainHandItem();
						IBackpackWrapper wrapper = BackpackLinkedStorageResolver.resolve(player.serverLevel(), backpack)
								.orElseGet(() -> backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
										.orElseThrow(() -> new IllegalStateException("Held backpack has no wrapper capability")));
						var jukebox = wrapper.getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream().findFirst()
								.orElseThrow(() -> new IllegalStateException("Held backpack has no jukebox upgrade"));
						if (arguments.slot() >= jukebox.getDiscInventory().getSlots()) {
							throw new IllegalArgumentException("Jukebox upgrade has no disc slot " + arguments.slot());
						}
						if (!jukebox.getDiscInventory().insertItem(arguments.slot(), new ItemStack(item), false).isEmpty()) {
							throw new IllegalStateException("Jukebox upgrade rejected fixture disc " + arguments.item());
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
