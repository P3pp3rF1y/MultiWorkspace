package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ConfigureStorageContentsFilterAction extends TypedRegressionAction<ConfigureStorageContentsFilterAction.Arguments> {
	public enum MatchMode {
		ITEM, MOD
	}

	public record Arguments(int x, int y, int z, String item, MatchMode match) {
	}

	public ConfigureStorageContentsFilterAction() {
		super("fixture.storage.contentsFilter", "1", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("offset", "item", "match"), description);
		String match = requireString(args, "match", description);
		if (!Set.of("mod", "item").contains(match)) {
			throw new IllegalArgumentException("Contents filter match must be mod or item on " + description);
		}
		JsonObject positionArgs = new JsonObject();
		positionArgs.addProperty("block", "minecraft:air");
		positionArgs.add("offset", args.get("offset"));
		var position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(position.x(), position.y(), position.z(), requireString(args, "item", description),
				match.equals("mod") ? MatchMode.MOD : MatchMode.ITEM);
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
						var pos = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
						if (!(player.level().getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
							throw new IllegalStateException("No storage at " + pos);
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown filter anchor item " + arguments.item()));
						context.journal().recordBlock(player.level(), pos);
						var wrapper = storage.getStorageWrapper();
						wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(item));
						wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.ADVANCED_FILTER_UPGRADE.get()));
						var filters = wrapper.getUpgradeHandler().getWrappersThatImplement(FilterUpgradeWrapper.class);
						if (filters.size() != 1) {
							throw new IllegalStateException("Expected exactly one contents filter");
						}
						var filter = filters.getFirst();
						filter.setDirection(Direction.INPUT);
						filter.getFilterLogic().setDepositFilterType(ContentsFilterType.STORAGE);
						filter.getFilterLogic().setPrimaryMatch(arguments.match() == MatchMode.MOD ? PrimaryMatch.MOD : PrimaryMatch.ITEM);
						wrapper.getUpgradeHandler().saveInventory();
						wrapper.refreshInventoryForInputOutput();
					});
				}
				return await(future);
			}
		};
	}
}
