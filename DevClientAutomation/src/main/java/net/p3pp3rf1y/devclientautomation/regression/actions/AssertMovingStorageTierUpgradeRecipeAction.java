package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.crafting.MovingStorageTierUpgradeShapedRecipe;
import net.p3pp3rf1y.sophisticatedstorageinmotion.crafting.MovingStorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertMovingStorageTierUpgradeRecipeAction extends TypedRegressionAction<AssertMovingStorageTierUpgradeRecipeAction.Arguments> {
	public record Arguments(String carrier, String recipe, String linkage) {
	}

	public AssertMovingStorageTierUpgradeRecipeAction() {
		super("assert.movingStorageTierUpgradeRecipe", "2", Set.of("minecraft:client", "sophisticatedstorage", "sophisticatedstorageinmotion"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("carrier", "recipe", "linkage"), description);
		return new Arguments(choice(args, "carrier", Set.of("boat", "minecart"), description),
				choice(args, "recipe", Set.of("shaped", "shapeless"), description),
				choice(args, "linkage", Set.of("unlinked", "primary", "secondary"), description));
	}

	private static String choice(JsonObject args, String name, Set<String> values, String description) {
		String value = requireString(args, name, description);
		if (!values.contains(value)) {
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be one of " + values);
		}
		return value;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (future == null) {
					future = context.onServer(player -> assertRecipe(player.serverLevel(), player.getUUID(), arguments));
					return RegressionActionProgress.WAITING;
				}
				return future.isDone() ? await(future) : RegressionActionProgress.WAITING;
			}
		};
	}

	private static void assertRecipe(ServerLevel level, UUID playerId, Arguments arguments) {
		ItemStack storage = storage(arguments);
		LinkedStorageEndpointData primary = null;
		try {
			if (!arguments.linkage().equals("unlinked")) {
				ItemStack linker = new ItemStack(
						net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation("sophisticatedcore:ender_linker")));
				if (LinkedStorageService.linkWithResult(level, playerId, linker, storage) != LinkedStorageService.LinkResult.SUCCESS) {
					throw new IllegalStateException("Could not create linked moving storage recipe fixture");
				}
				primary = LinkedStorageStackData.getEndpoint(storage);
				if (arguments.linkage().equals("secondary")) {
					storage = LinkedStorageService.createSecondaryEndpointCopy(level, storage)
							.orElseThrow(() -> new IllegalStateException("Could not create secondary moving storage recipe fixture"));
				}
			}
			ItemStack moving = MovingStorageItem.createWithStorage(new ItemStack(carrier(arguments)), storage);
			moving.setHoverName(net.minecraft.network.chat.Component.literal("moving recipe fixture"));
			ResourceLocation id = recipeId(arguments);
			Recipe<?> recipe = level.getRecipeManager().byKey(id).orElseThrow(() -> new IllegalStateException("Missing registered recipe " + id));
			Class<?> expectedClass = arguments.recipe().equals("shaped")
					? MovingStorageTierUpgradeShapedRecipe.class
					: MovingStorageTierUpgradeShapelessRecipe.class;
			if (recipe.getClass() != expectedClass) {
				throw new IllegalStateException("Registered recipe has class " + recipe.getClass().getName() + ", expected " + expectedClass.getName());
			}
			@SuppressWarnings("unchecked")
			Recipe<CraftingContainer> craftingRecipe = (Recipe<CraftingContainer>) recipe;
			CraftingContainer input = input(moving, arguments);
			boolean expected = !arguments.linkage().equals("secondary");
			ItemStack result = craftingRecipe.assemble(input, level.registryAccess());
			if (craftingRecipe.matches(input, level) != expected || result.isEmpty() != !expected) {
				throw new IllegalStateException("Moving storage tier upgrade " + arguments + " eligibility differed from contract");
			}
			if (craftingRecipe.matches(new TransientCraftingContainer(null, 1, 1, NonNullList.of(ItemStack.EMPTY, moving)), level)) {
				throw new IllegalStateException("Moving storage tier upgrade accepted malformed input");
			}
			if (expected) {
				assertOutput(result, moving, storage, arguments);
			}
		} finally {
			if (primary != null) {
				LinkedStorageGroupsSavedData.get(level).manager().discardUnboundGroup(primary.groupId(), primary.endpointId());
			}
		}
	}

	private static ItemStack storage(Arguments arguments) {
		ItemStack storage = new ItemStack(arguments.recipe().equals("shaped") ? ModBlocks.GOLD_CHEST_ITEM.get() : ModBlocks.DIAMOND_CHEST_ITEM.get());
		storage.setHoverName(net.minecraft.network.chat.Component.literal("nested recipe fixture"));
		storage.getOrCreateTag().putInt(StorageBlockItem.MAIN_COLOR_TAG, 0x123456);
		storage.getOrCreateTag().putInt(StorageBlockItem.ACCENT_COLOR_TAG, 0x654321);
		storage.getOrCreateTag().putUUID("uuid", UUID.randomUUID());
		return storage;
	}

	private static Item carrier(Arguments arguments) {
		return arguments.carrier().equals("boat") ? ModItems.STORAGE_BOAT.get() : ModItems.STORAGE_MINECART.get();
	}

	private static ResourceLocation recipeId(Arguments arguments) {
		return new ResourceLocation("sophisticatedstorageinmotion", "storage_" + arguments.carrier() + "_with_"
				+ (arguments.recipe().equals("shaped") ? "gold_chest_to_diamond_chest" : "diamond_chest_to_netherite_chest"));
	}

	private static CraftingContainer input(ItemStack moving, Arguments arguments) {
		NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
		if (arguments.recipe().equals("shaped")) {
			for (int slot = 0; slot < 9; slot++) {
				stacks.set(slot, new ItemStack(Items.DIAMOND));
			}
			stacks.set(4, moving);
		} else {
			stacks.set(0, moving);
			stacks.set(1, new ItemStack(Items.NETHERITE_INGOT));
		}
		return new TransientCraftingContainer(null, 3, 3, stacks);
	}

	private static void assertOutput(ItemStack output, ItemStack moving, ItemStack storage, Arguments arguments) {
		ItemStack nested = MovingStorageItem.getStorageItem(output);
		if (!output.is(carrier(arguments)) || !Objects.equals(output.getHoverName(), moving.getHoverName())
				|| !nested.is(arguments.recipe().equals("shaped") ? ModBlocks.DIAMOND_CHEST_ITEM.get() : ModBlocks.NETHERITE_CHEST_ITEM.get())
				|| !Objects.equals(nested.getHoverName(), storage.getHoverName())
				|| !StorageBlockItem.getMainColorFromStack(nested).equals(StorageBlockItem.getMainColorFromStack(storage))
				|| !StorageBlockItem.getAccentColorFromStack(nested).equals(StorageBlockItem.getAccentColorFromStack(storage))
				|| !StorageBlockItem.getContentsUuid(nested).equals(StorageBlockItem.getContentsUuid(storage))
				|| !Objects.equals(LinkedStorageStackData.getEndpoint(nested), LinkedStorageStackData.getEndpoint(storage))
				|| LinkedStorageStackData.isPrimaryEndpoint(nested) != LinkedStorageStackData.isPrimaryEndpoint(storage)) {
			throw new IllegalStateException("Moving storage tier upgrade did not preserve expected outer and nested storage identity");
		}
	}
}
