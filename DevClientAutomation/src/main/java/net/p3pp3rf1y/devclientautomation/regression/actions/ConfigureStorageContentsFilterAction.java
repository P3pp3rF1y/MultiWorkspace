package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModItems.ADVANCED_FILTER_UPGRADE;

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
		if (!Set.of("item", "mod").contains(match)) {
			throw new IllegalArgumentException("Contents filter match must be item or mod on " + description);
		}
		JsonObject positionArgs = new JsonObject();
		positionArgs.addProperty("block", "minecraft:air");
		positionArgs.add("offset", args.get("offset"));
		SetFixtureBlockAction.Arguments position = new SetFixtureBlockAction().parseArguments(positionArgs, description);
		return new Arguments(position.x(), position.y(), position.z(), requireString(args, "item", description),
				match.equals("mod") ? MatchMode.MOD : MatchMode.ITEM);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> {
						var pos = context.standingPosition().south(2).offset(arguments.x(), arguments.y(), arguments.z());
						ServerLevel level = (ServerLevel) player.level();
						if (!(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
							throw new IllegalStateException("No storage at " + pos);
						}
						var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(arguments.item()))
								.orElseThrow(() -> new IllegalArgumentException("Unknown filter anchor item " + arguments.item()));
						context.journal().recordBlock(level, pos);
						var wrapper = storage.getStorageWrapper();
						wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(item));
						wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ADVANCED_FILTER_UPGRADE.get()));
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
