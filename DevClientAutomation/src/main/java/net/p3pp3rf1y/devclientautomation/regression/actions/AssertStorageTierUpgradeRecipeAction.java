package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;

import java.util.ArrayList;
import java.util.List;
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
						net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("sophisticatedcore:ender_linker")));
				if (LinkedStorageService.linkWithResult(level, playerId, linker, storage) != LinkedStorageService.LinkResult.SUCCESS) {
					throw new IllegalStateException("Could not create linked storage recipe fixture");
				}
				primary = storage.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
				if (arguments.linkage().equals("secondary")) {
					storage = LinkedStorageService.createSecondaryEndpointCopy(level, storage)
							.orElseThrow(() -> new IllegalStateException("Could not create secondary storage recipe fixture"));
				}
			}

			RecipeHolder<?> holder = level.getRecipeManager().byKey(recipeId(arguments))
					.orElseThrow(() -> new IllegalStateException("Missing registered recipe " + recipeId(arguments)));
			assertRecipeClass(holder.value(), arguments);
			CraftingInput input = input(storage, arguments);
			@SuppressWarnings("unchecked")
			Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) holder.value();
			boolean expected = !arguments.linkage().equals("secondary");
			if (recipe.matches(input, level) != expected || recipe.assemble(input, level.registryAccess()).isEmpty() != !expected) {
				throw new IllegalStateException("Storage tier upgrade " + arguments + " eligibility differed from contract");
			}
			if (recipe.matches(CraftingInput.of(1, 1, List.of(storage)), level)) {
				throw new IllegalStateException("Storage tier upgrade accepted malformed input");
			}
			if (expected) {
				assertOutput(recipe.assemble(input, level.registryAccess()), storage, arguments);
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
		storage.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("recipe fixture"));
		storage.set(ModCoreDataComponents.MAIN_COLOR, 0x123456);
		storage.set(ModCoreDataComponents.ACCENT_COLOR, 0x654321);
		storage.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		return storage;
	}

	static ResourceLocation recipeId(Arguments arguments) {
		return ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", recipePath(arguments));
	}

	static String recipePath(Arguments arguments) {
		return StorageTierUpgradeRecipeContract.recipePath(arguments.form(), arguments.recipe());
	}

	private static void assertRecipeClass(Recipe<?> recipe, Arguments arguments) {
		Class<?> expected = arguments.recipe().equals("shaped")
				? (arguments.form().equals("double") ? DoubleChestTierUpgradeRecipe.class : StorageTierUpgradeRecipe.class)
				: (arguments.form().equals("double") ? DoubleChestTierUpgradeShapelessRecipe.class : StorageTierUpgradeShapelessRecipe.class);
		if (recipe.getClass() != expected) {
			throw new IllegalStateException("Registered recipe has class " + recipe.getClass().getName() + ", expected " + expected.getName());
		}
	}

	private static CraftingInput input(ItemStack storage, Arguments arguments) {
		List<ItemStack> stacks = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
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
		return CraftingInput.of(3, 3, stacks);
	}

	static int requiredNetheriteIngots(Arguments arguments) {
		return StorageTierUpgradeRecipeContract.requiredNetheriteIngots(arguments.form(), arguments.recipe());
	}

	private static void assertOutput(ItemStack output, ItemStack input, Arguments arguments) {
		if (!output.is(arguments.recipe().equals("shaped") ? ModBlocks.DIAMOND_CHEST_ITEM.get() : ModBlocks.NETHERITE_CHEST_ITEM.get())
				|| ChestBlockItem.isDoubleChest(output) != arguments.form().equals("double")
				|| !output.get(DataComponents.CUSTOM_NAME).equals(input.get(DataComponents.CUSTOM_NAME))
				|| !output.get(ModCoreDataComponents.MAIN_COLOR).equals(input.get(ModCoreDataComponents.MAIN_COLOR))
				|| !output.get(ModCoreDataComponents.ACCENT_COLOR).equals(input.get(ModCoreDataComponents.ACCENT_COLOR))
				|| !output.get(ModCoreDataComponents.STORAGE_UUID).equals(input.get(ModCoreDataComponents.STORAGE_UUID))
				|| !java.util.Objects.equals(output.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT),
						input.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				|| output.has(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT) != input.has(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT)) {
			throw new IllegalStateException("Storage tier upgrade did not preserve the expected storage identity");
		}
	}
}
