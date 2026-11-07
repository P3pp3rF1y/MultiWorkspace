package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;

import java.util.Set;

/** Installs the run-scoped listener before the real GUI playback control is used. */
public final class ObserveHeldBackpackJukeboxSoundAction extends TypedRegressionAction<String> {
	public ObserveHeldBackpackJukeboxSoundAction() {
		super("observe.heldBackpackJukeboxSound", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("item"), description);
		return requireString(args, "item", description);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String itemId) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemId))
						.orElseThrow(() -> new IllegalArgumentException("Unknown disc " + itemId));
				if (!(item instanceof RecordItem record)) {
					return fail("Item " + itemId + " is not a jukebox disc");
				}
				context.observeHeldBackpackJukeboxSound(record.getSound().getLocation());
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
