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
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorageinmotion.crafting.MovingStorageTierUpgradeShapedRecipe;
import net.p3pp3rf1y.sophisticatedstorageinmotion.crafting.MovingStorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorageinmotion.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorageinmotion.item.MovingStorageItem;

import java.util.ArrayList;
import java.util.List;
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
		if (!values.contains(value))
			throw new IllegalArgumentException("Argument " + name + " on " + description + " must be one of " + values);
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
				if (LinkedStorageService.linkWithResult(level, playerId, linker, storage) != LinkedStorageService.LinkResult.SUCCESS)
					throw RegressionActionFailure.infrastructure("Could not create linked moving storage recipe fixture");
				primary = storage.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
				if (arguments.linkage().equals("secondary"))
					storage = LinkedStorageService.createSecondaryEndpointCopy(level, storage)
							.orElseThrow(() -> RegressionActionFailure.infrastructure("Could not create secondary moving storage recipe fixture"));
			}
			ItemStack moving = MovingStorageItem.createWithStorage(new ItemStack(carrier(arguments)), storage);
			moving.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("moving recipe fixture"));
			RecipeHolder<?> holder = level.getRecipeManager().byKey(recipeId(arguments))
					.orElseThrow(() -> RegressionActionFailure.infrastructure("Missing registered recipe " + recipeId(arguments)));
			Class<?> expectedClass = arguments.recipe().equals("shaped")
					? MovingStorageTierUpgradeShapedRecipe.class
					: MovingStorageTierUpgradeShapelessRecipe.class;
			if (holder.value().getClass() != expectedClass)
				throw RegressionActionFailure
						.assertion("Registered recipe has class " + holder.value().getClass().getName() + ", expected " + expectedClass.getName());
			CraftingInput input = input(moving, arguments);
			@SuppressWarnings("unchecked")
			Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) holder.value();
			boolean expected = !arguments.linkage().equals("secondary");
			if (recipe.matches(input, level) != expected || recipe.assemble(input, level.registryAccess()).isEmpty() != !expected)
				throw RegressionActionFailure.assertion("Moving storage tier upgrade " + arguments + " eligibility differed from contract");
			if (recipe.matches(CraftingInput.of(1, 1, List.of(moving)), level))
				throw RegressionActionFailure.assertion("Moving storage tier upgrade accepted malformed input");
			if (expected)
				assertOutput(recipe.assemble(input, level.registryAccess()), moving, storage, arguments);
		} finally {
			if (primary != null)
				LinkedStorageGroupsSavedData.get(level).manager().discardUnboundGroup(primary.groupId(), primary.endpointId());
		}
	}

	private static ItemStack storage(Arguments arguments) {
		ItemStack storage = new ItemStack(arguments.recipe().equals("shaped") ? ModBlocks.GOLD_CHEST_ITEM.get() : ModBlocks.DIAMOND_CHEST_ITEM.get());
		storage.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("nested recipe fixture"));
		storage.set(ModCoreDataComponents.MAIN_COLOR, 0x123456);
		storage.set(ModCoreDataComponents.ACCENT_COLOR, 0x654321);
		storage.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		return storage;
	}

	private static MovingStorageItem carrier(Arguments arguments) {
		return arguments.carrier().equals("boat") ? ModItems.STORAGE_BOAT.get() : ModItems.STORAGE_MINECART.get();
	}
	private static ResourceLocation recipeId(Arguments arguments) {
		return ResourceLocation.fromNamespaceAndPath("sophisticatedstorageinmotion", "storage_" + arguments.carrier() + "_with_"
				+ (arguments.recipe().equals("shaped") ? "gold_chest_to_diamond_chest" : "diamond_chest_to_netherite_chest"));
	}

	private static CraftingInput input(ItemStack moving, Arguments arguments) {
		List<ItemStack> stacks = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
		if (arguments.recipe().equals("shaped")) {
			for (int slot = 0; slot < 9; slot++)
				stacks.set(slot, new ItemStack(Items.DIAMOND));
			stacks.set(4, moving);
		} else {
			stacks.set(0, moving);
			stacks.set(1, new ItemStack(Items.NETHERITE_INGOT));
		}
		return CraftingInput.of(3, 3, stacks);
	}

	private static void assertOutput(ItemStack output, ItemStack moving, ItemStack storage, Arguments arguments) {
		ItemStack nested = MovingStorageItem.getStorageItem(output);
		if (!output.is(carrier(arguments)) || !output.get(DataComponents.CUSTOM_NAME).equals(moving.get(DataComponents.CUSTOM_NAME))
				|| !nested.is(arguments.recipe().equals("shaped") ? ModBlocks.DIAMOND_CHEST_ITEM.get() : ModBlocks.NETHERITE_CHEST_ITEM.get())
				|| !nested.get(DataComponents.CUSTOM_NAME).equals(storage.get(DataComponents.CUSTOM_NAME))
				|| !nested.get(ModCoreDataComponents.MAIN_COLOR).equals(storage.get(ModCoreDataComponents.MAIN_COLOR))
				|| !nested.get(ModCoreDataComponents.ACCENT_COLOR).equals(storage.get(ModCoreDataComponents.ACCENT_COLOR))
				|| !nested.get(ModCoreDataComponents.STORAGE_UUID).equals(storage.get(ModCoreDataComponents.STORAGE_UUID))
				|| !java.util.Objects.equals(nested.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT),
						storage.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT))
				|| nested.has(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT) != storage.has(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT))
			throw RegressionActionFailure.assertion("Moving storage tier upgrade did not preserve expected outer and nested storage identity");
	}
}
