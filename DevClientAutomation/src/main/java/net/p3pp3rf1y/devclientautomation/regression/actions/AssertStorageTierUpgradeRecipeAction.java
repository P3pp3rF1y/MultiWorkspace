package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertStorageTierUpgradeRecipeAction extends TypedRegressionAction<AssertStorageTierUpgradeRecipeAction.Arguments> {
	public record Arguments(String form, String recipe, String linkage) {
	}

	public AssertStorageTierUpgradeRecipeAction() {
		super("assert.storageTierUpgradeRecipe", "2", Set.of("minecraft:client", "sophisticatedstorage"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("form", "recipe", "linkage"), description);
		return new Arguments(choice(args, "form", Set.of("normal", "double"), description), choice(args, "recipe", Set.of("shaped", "shapeless"), description),
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
					throw RegressionActionFailure.infrastructure("Could not create linked storage recipe fixture");
				}
				primary = LinkedStorageStackData.getEndpoint(storage);
				if (arguments.linkage().equals("secondary")) {
					storage = LinkedStorageService.createSecondaryEndpointCopy(level, storage)
							.orElseThrow(() -> RegressionActionFailure.infrastructure("Could not create secondary storage recipe fixture"));
				}
			}
			ResourceLocation id = recipeId(arguments);
			Recipe<?> recipe = level.getRecipeManager().byKey(id).orElseThrow(() -> RegressionActionFailure.infrastructure("Missing registered recipe " + id));
			assertRecipeClass(recipe, arguments);
			@SuppressWarnings("unchecked")
			Recipe<CraftingContainer> craftingRecipe = (Recipe<CraftingContainer>) recipe;
			CraftingContainer input = input(storage, arguments);
			boolean expected = !arguments.linkage().equals("secondary");
			boolean matches = craftingRecipe.matches(input, level);
			ItemStack result = craftingRecipe.assemble(input, level.registryAccess());
			if (matches != expected || result.isEmpty() != !expected) {
				throw RegressionActionFailure.assertion("Storage tier upgrade " + arguments + " eligibility differed from contract");
			}
			if (craftingRecipe.matches(new TransientCraftingContainer(null, 1, 1, NonNullList.of(ItemStack.EMPTY, storage)), level)) {
				throw RegressionActionFailure.assertion("Storage tier upgrade accepted malformed input");
			}
			if (expected) {
				assertOutput(result, storage, arguments);
			}
		} finally {
			if (primary != null) {
				LinkedStorageGroupsSavedData.get(level).manager().discardUnboundGroup(primary.groupId(), primary.endpointId());
			}
		}
	}

	private static ItemStack storage(Arguments arguments) {
		ItemStack storage = new ItemStack(arguments.recipe().equals("shaped") ? ModBlocks.GOLD_CHEST_ITEM.get() : ModBlocks.DIAMOND_CHEST_ITEM.get());
		if (arguments.form().equals("double")) {
			ChestBlockItem.setDoubleChest(storage, true);
		}
		storage.setHoverName(net.minecraft.network.chat.Component.literal("recipe fixture"));
		storage.getOrCreateTag().putInt(StorageBlockItem.MAIN_COLOR_TAG, 0x123456);
		storage.getOrCreateTag().putInt(StorageBlockItem.ACCENT_COLOR_TAG, 0x654321);
		storage.getOrCreateTag().putUUID("uuid", UUID.randomUUID());
		return storage;
	}

	static ResourceLocation recipeId(Arguments arguments) {
		return new ResourceLocation("sophisticatedstorage", recipePath(arguments));
	}

	static String recipePath(Arguments arguments) {
		return StorageTierUpgradeRecipeContract.recipePath(arguments.form(), arguments.recipe());
	}

	private static void assertRecipeClass(Recipe<?> recipe, Arguments arguments) {
		Class<?> expected = arguments.recipe().equals("shaped")
				? (arguments.form().equals("double") ? DoubleChestTierUpgradeRecipe.class : StorageTierUpgradeRecipe.class)
				: (arguments.form().equals("double") ? DoubleChestTierUpgradeShapelessRecipe.class : StorageTierUpgradeShapelessRecipe.class);
		if (recipe.getClass() != expected) {
			throw RegressionActionFailure.assertion("Registered recipe has class " + recipe.getClass().getName() + ", expected " + expected.getName());
		}
	}

	private static CraftingContainer input(ItemStack storage, Arguments arguments) {
		NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
		if (arguments.recipe().equals("shaped")) {
			for (int slot = 0; slot < 9; slot++) {
				stacks.set(slot, new ItemStack(Items.DIAMOND));
			}
			stacks.set(4, storage);
			if (arguments.form().equals("double")) {
				stacks.set(7, new ItemStack(Blocks.DIAMOND_BLOCK));
			}
		} else {
			stacks.set(0, storage);
			for (int slot = 1; slot <= requiredNetheriteIngots(arguments); slot++) {
				stacks.set(slot, new ItemStack(Items.NETHERITE_INGOT));
			}
		}
		return new TransientCraftingContainer(null, 3, 3, stacks);
	}

	static int requiredNetheriteIngots(Arguments arguments) {
		return StorageTierUpgradeRecipeContract.requiredNetheriteIngots(arguments.form(), arguments.recipe());
	}

	private static void assertOutput(ItemStack output, ItemStack input, Arguments arguments) {
		if (!output.is(arguments.recipe().equals("shaped") ? ModBlocks.DIAMOND_CHEST_ITEM.get() : ModBlocks.NETHERITE_CHEST_ITEM.get())
				|| ChestBlockItem.isDoubleChest(output) != arguments.form().equals("double") || !Objects.equals(output.getHoverName(), input.getHoverName())
				|| !StorageBlockItem.getMainColorFromStack(output).equals(StorageBlockItem.getMainColorFromStack(input))
				|| !StorageBlockItem.getAccentColorFromStack(output).equals(StorageBlockItem.getAccentColorFromStack(input))
				|| !StorageBlockItem.getContentsUuid(output).equals(StorageBlockItem.getContentsUuid(input))
				|| !Objects.equals(LinkedStorageStackData.getEndpoint(output), LinkedStorageStackData.getEndpoint(input))
				|| LinkedStorageStackData.isPrimaryEndpoint(output) != LinkedStorageStackData.isPrimaryEndpoint(input)) {
			throw RegressionActionFailure.assertion("Storage tier upgrade did not preserve the expected storage identity");
		}
	}
}
