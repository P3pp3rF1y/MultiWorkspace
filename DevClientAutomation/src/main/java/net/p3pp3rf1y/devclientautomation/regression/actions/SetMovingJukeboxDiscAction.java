package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SetMovingJukeboxDiscAction extends TypedRegressionAction<SetMovingJukeboxDiscAction.Arguments> {
	public record Arguments(String ref, String item) {
	}

	public SetMovingJukeboxDiscAction() {
		super("fixture.moving.setJukeboxDisc", "1", Set.of("minecraft:client", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("ref", "item"), description);
		return new Arguments(RegressionValueStore.requireName(requireString(args, "ref", description)), requireString(args, "item", description));
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
						Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown disc " + arguments.item()));
						IMovingStorageEntity moving = (IMovingStorageEntity) context.values().require(arguments.ref(), MovingStorageEntityHandle.class)
								.requireServer(player);
						ItemStack remainder = moving.getStorageHolder().getStorageWrapper().getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE)
								.stream().findFirst().orElseThrow(() -> new IllegalStateException("Moving storage has no jukebox upgrade")).getDiscInventory()
								.insertItem(0, new ItemStack(item), false);
						if (!remainder.isEmpty()) {
							throw new IllegalStateException("Jukebox upgrade rejected fixture disc " + arguments.item());
						}
					});
				}
				return await(future);
			}
		};
	}
}
