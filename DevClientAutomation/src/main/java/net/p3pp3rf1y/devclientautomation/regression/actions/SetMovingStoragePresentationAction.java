package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetMovingStoragePresentationAction extends TypedRegressionAction<String> {
	public SetMovingStoragePresentationAction() {
		super("moving.setPresentation", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref"), description);
		return RegressionValueStore.requireName(requireString(args, "ref", description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String ref) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(ref, MovingStorageEntityHandle.class)
								.requireServer(player);
						SettingsHandler settings = moving.getStorageHolder().getStorageWrapper().getSettingsHandler();
						settings.getTypeCategory(MemorySettingsCategory.class).selectSlot(0);
						settings.getTypeCategory(ItemDisplaySettingsCategory.class).selectSlot(0);
					});
				}
				return await(future);
			}
		};
	}
}
