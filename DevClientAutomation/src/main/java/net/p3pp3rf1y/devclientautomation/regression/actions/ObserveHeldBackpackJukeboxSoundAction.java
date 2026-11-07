package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

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
				Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId))
						.orElseThrow(() -> new IllegalArgumentException("Unknown disc " + itemId));
				JukeboxPlayable playable = new ItemStack(item).get(DataComponents.JUKEBOX_PLAYABLE);
				if (playable == null) {
					return fail("Item " + itemId + " is not a jukebox disc");
				}
				JukeboxSong song = playable.song().unwrap(context.minecraft().level.registryAccess())
						.orElseThrow(() -> new IllegalStateException("Jukebox song is unavailable for " + itemId)).value();
				context.installHeldBackpackJukeboxSoundObserver(song.soundEvent().value().getLocation());
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
